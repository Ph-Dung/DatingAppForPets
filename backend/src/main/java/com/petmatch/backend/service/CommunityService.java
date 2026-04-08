package com.petmatch.backend.service;

import java.util.List;
import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.petmatch.backend.dto.PostResponse;
import com.petmatch.backend.dto.request.CommunityReportRequest;
import com.petmatch.backend.dto.response.CommentResponse;
import com.petmatch.backend.entity.Comment;
import com.petmatch.backend.entity.HiddenPost;
import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.CommentRepository;
import com.petmatch.backend.repository.HiddenPostRepository;
import com.petmatch.backend.repository.LikeRepository;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.PostRepository;
import com.petmatch.backend.repository.ReportRepository;
import com.petmatch.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommunityService {
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;
    private final HiddenPostRepository hiddenPostRepository;
    private final UserRepository userRepository;
    private final PetProfileRepository petProfileRepository;
    private final PetPhotoRepository petPhotoRepository;
    private final CloudinaryService cloudinaryService;

    @Value("${community.moderation.enabled:false}")
    private boolean moderationEnabled;

    @Value("${community.moderation.blocked-keywords:}")
    private List<String> blockedKeywords;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", HttpStatus.NOT_FOUND));
    }

    private Post requirePost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));
    }

    private Comment requireComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment không tồn tại", HttpStatus.NOT_FOUND));
    }

    private void assertCanManagePost(User actor, Post post, String action) {
        if (!post.getUser().getId().equals(actor.getId()) && !canModerate(actor)) {
            throw new AppException("Bạn không có quyền " + action + " bài viết này", HttpStatus.FORBIDDEN);
        }
    }

    private void assertCanManageComment(User actor, Comment comment, String action) {
        if (!comment.getUser().getId().equals(actor.getId()) && !canModerate(actor)) {
            throw new AppException("Bạn không có quyền " + action + " bình luận này", HttpStatus.FORBIDDEN);
        }
    }

    private List<PostResponse> toPostResponses(List<Post> posts, User actor) {
        return posts.stream()
                .map(post -> mapToPostResponse(post, actor))
                .collect(Collectors.toList());
    }

    private boolean canModerate(User user) {
        return user.getRole() == Role.ADMIN;
    }

    private void validateContentForModeration(String content) {
        if (!moderationEnabled || content == null || content.isBlank()) {
            return;
        }

        String lowered = content.toLowerCase();
        for (String keyword : blockedKeywords) {
            if (keyword != null && !keyword.isBlank() && lowered.contains(keyword.toLowerCase().trim())) {
                throw new AppException("Nội dung vi phạm tiêu chuẩn cộng đồng", HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getFeed() {
        User actor = currentUser();
        List<Long> hiddenPostIds = hiddenPostRepository.findPostIdsByUserId(actor.getId());
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        if (hiddenPostIds != null && !hiddenPostIds.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> !hiddenPostIds.contains(post.getId()))
                    .collect(Collectors.toList());
        }
        return toPostResponses(posts, actor);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts() {
        User actor = currentUser();
        return toPostResponses(postRepository.findAllByUserOrderByCreatedAtDesc(actor), actor);
    }

    @Transactional(readOnly = true)
    public PostResponse getPostDetail(Long postId) {
        User actor = currentUser();
        return mapToPostResponse(requirePost(postId), actor);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(Long userId) {
        User actor = currentUser();
        return toPostResponses(postRepository.findAllByUserIdOrderByCreatedAtDesc(userId), actor);
    }

    @Transactional
    public PostResponse createPost(String content, String imageUrl, String location) {
        User currentUser = currentUser();
        validateContentForModeration(content);

        Post post = Post.builder()
                .content(content)
                .imageUrl(imageUrl)
                .location(location)
                .user(currentUser)
                .build();
        return mapToPostResponse(postRepository.save(post), currentUser);
    }

    @Transactional
    public PostResponse createPostWithImageUpload(String content, String location, MultipartFile imageFile) {
        List<MultipartFile> files = imageFile == null ? List.of() : List.of(imageFile);
        return createPostWithImageUploads(content, location, files);
    }

    @Transactional
    public PostResponse createPostWithImageUploads(String content, String location, List<MultipartFile> imageFiles) {
        String uploadedImageUrls = null;
        if (imageFiles != null && !imageFiles.isEmpty()) {
            uploadedImageUrls = imageFiles.stream()
                    .filter(Objects::nonNull)
                    .filter(file -> !file.isEmpty())
                    .map(file -> cloudinaryService.uploadImage(file, "petmatch/community"))
                    .collect(Collectors.joining(","));
            if (uploadedImageUrls.isBlank()) {
                uploadedImageUrls = null;
            }
        }

        return createPost(content, uploadedImageUrls, location);
    }

    @Transactional
    public PostResponse updatePost(Long postId, String content, String imageUrl, String location) {
        User actor = currentUser();
        Post post = requirePost(postId);
        assertCanManagePost(actor, post, "chỉnh sửa");

        validateContentForModeration(content);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setLocation(location);

        return mapToPostResponse(postRepository.save(post), actor);
    }

    @Transactional
    public PostResponse updatePostWithImageUploads(
            Long postId,
            String content,
            String location,
            String existingImageUrls,
            List<MultipartFile> imageFiles
    ) {
        User actor = currentUser();
        Post post = requirePost(postId);
        assertCanManagePost(actor, post, "chỉnh sửa");

        validateContentForModeration(content);

        List<String> finalUrls = new ArrayList<>();
        if (existingImageUrls != null && !existingImageUrls.isBlank()) {
            String[] parts = existingImageUrls.split("[,;]");
            for (String part : parts) {
                String url = part == null ? "" : part.trim();
                if (!url.isBlank()) {
                    finalUrls.add(url);
                }
            }
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            imageFiles.stream()
                    .filter(Objects::nonNull)
                    .filter(file -> !file.isEmpty())
                    .map(file -> cloudinaryService.uploadImage(file, "petmatch/community"))
                    .forEach(finalUrls::add);
        }

        post.setContent(content);
        post.setLocation(location);
        post.setImageUrl(finalUrls.isEmpty() ? null : String.join(",", finalUrls));

        return mapToPostResponse(postRepository.save(post), actor);
    }

    @Transactional
    public void deletePost(Long postId) {
        User actor = currentUser();
        Post post = requirePost(postId);
        assertCanManagePost(actor, post, "xóa");

        postRepository.delete(post);
    }

    @Transactional
    public boolean toggleLike(Long postId) {
        User actor = currentUser();
        Post post = requirePost(postId);

        return likeRepository.findByUserAndPost(actor, post)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    return false;
                })
                .orElseGet(() -> {
                    likeRepository.save(Like.builder().user(actor).post(post).build());
                    return true;
                });
    }

    @Transactional
    public void unlikePost(Long postId) {
        User actor = currentUser();
        Post post = requirePost(postId);

        likeRepository.findByUserAndPost(actor, post)
                .ifPresent(likeRepository::delete);
    }

    @Transactional
    public CommentResponse addComment(Long postId, String content) {
        User actor = currentUser();
        Post post = requirePost(postId);

        validateContentForModeration(content);
        Comment comment = Comment.builder()
                .content(content)
                .user(actor)
                .post(post)
                .build();

        return mapToCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse replyComment(Long commentId, String content) {
        User actor = currentUser();
        Comment parentComment = requireComment(commentId);

        validateContentForModeration(content);
        Comment reply = Comment.builder()
                .content(content)
                .user(actor)
                .post(parentComment.getPost())
                .parentComment(parentComment)
                .build();

        return mapToCommentResponse(commentRepository.save(reply));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, String content) {
        User actor = currentUser();
        Comment comment = requireComment(commentId);
        assertCanManageComment(actor, comment, "chỉnh sửa");

        validateContentForModeration(content);
        comment.setContent(content);
        return mapToCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User actor = currentUser();
        Comment comment = requireComment(commentId);
        assertCanManageComment(actor, comment, "xóa");

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        Post post = requirePost(postId);

        return commentRepository.findAllByPostAndParentCommentIsNullOrderByCreatedAtAsc(post)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public Report submitReport(CommunityReportRequest request) {
        User currentUser = currentUser();
        validateReportTarget(request.getTargetType(), request.getTargetId());

        Report report = Report.builder()
                .reporter(currentUser)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .reason(request.getReason())
                .status(ReportStatus.PENDING)
                .build();

        Report saved = reportRepository.save(report);

        if (Boolean.TRUE.equals(request.getHidePost()) && request.getTargetType() == ReportTargetType.POST) {
            Post post = requirePost(request.getTargetId());
            hiddenPostRepository.findByUserAndPost(currentUser, post)
                .orElseGet(() -> hiddenPostRepository.save(HiddenPost.builder()
                    .user(currentUser)
                    .post(post)
                    .build()));
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Report> getMyReports() {
        User currentUser = currentUser();
        return reportRepository.findByReporterIdOrderByCreatedAtDesc(currentUser.getId());
    }

    private void validateReportTarget(ReportTargetType targetType, Long targetId) {
        boolean exists = switch (targetType) {
            case POST -> postRepository.existsById(targetId);
            case COMMENT -> commentRepository.existsById(targetId);
            default -> throw new AppException("Community chỉ hỗ trợ report POST hoặc COMMENT", HttpStatus.BAD_REQUEST);
        };

        if (!exists) {
            throw new AppException("Đối tượng bị báo cáo không tồn tại", HttpStatus.NOT_FOUND);
        }
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        List<CommentResponse> replyResponses = commentRepository
                .findAllByParentCommentOrderByCreatedAtAsc(comment)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());

        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userId(comment.getUser().getId())
                .userName(comment.getUser().getFullName())
                .userAvatar(comment.getUser().getAvatarUrl())
                .createdAt(comment.getCreatedAt())
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() == null ? null : comment.getParentComment().getId())
                .replies(replyResponses)
                .build();
    }

    private PostResponse mapToPostResponse(Post post, User currentUser) {
        String ownerAvatar = post.getUser().getAvatarUrl();
        if (ownerAvatar == null || ownerAvatar.isBlank()) {
            PetProfile ownerPet = petProfileRepository.findByOwnerId(post.getUser().getId()).orElse(null);
            if (ownerPet != null) {
                ownerAvatar = petPhotoRepository.findByPetIdAndIsAvatarTrue(ownerPet.getId())
                    .map(PetPhoto::getPhotoUrl)
                    .orElseGet(() -> petPhotoRepository.findByPetId(ownerPet.getId()).stream()
                        .findFirst()
                        .map(PetPhoto::getPhotoUrl)
                        .orElse(null));
            }
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .location(post.getLocation())
                .ownerId(post.getUser().getId())
                .ownerName(post.getUser().getFullName())
                .ownerAvatar(ownerAvatar)
                .createdAt(post.getCreatedAt())
                .likesCount(likeRepository.countByPost(post))
                .commentsCount(post.getComments().size())
                .isLiked(currentUser != null && likeRepository.existsByUserAndPost(currentUser, post))
                .build();
    }
}
