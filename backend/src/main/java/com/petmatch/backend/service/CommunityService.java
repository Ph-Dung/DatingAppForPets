package com.petmatch.backend.service;

import java.util.List;
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
import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.CommentRepository;
import com.petmatch.backend.repository.LikeRepository;
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
    private final UserRepository userRepository;
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
        User currentUser = currentUser();
        return postRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(post -> mapToPostResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts() {
        User currentUser = currentUser();
        return postRepository.findAllByUserOrderByCreatedAtDesc(currentUser).stream()
                .map(post -> mapToPostResponse(post, currentUser))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PostResponse getPostDetail(Long postId) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));
        return mapToPostResponse(post, currentUser);
    }

    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(Long userId) {
        User currentUser = currentUser();
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(post -> mapToPostResponse(post, currentUser))
                .collect(Collectors.toList());
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
        String uploadedImageUrl = imageFile == null ? null :
                cloudinaryService.uploadImage(imageFile, "petmatch/community");

        return createPost(content, uploadedImageUrl, location);
    }

    @Transactional
    public PostResponse updatePost(Long postId, String content, String imageUrl, String location) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

        if (!post.getUser().getId().equals(currentUser.getId()) && !canModerate(currentUser)) {
            throw new AppException("Bạn không có quyền chỉnh sửa bài viết này", HttpStatus.FORBIDDEN);
        }

        validateContentForModeration(content);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setLocation(location);

        return mapToPostResponse(postRepository.save(post), currentUser);
    }

    @Transactional
    public void deletePost(Long postId) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

        if (!post.getUser().getId().equals(currentUser.getId()) && !canModerate(currentUser)) {
            throw new AppException("Bạn không có quyền xóa bài viết này", HttpStatus.FORBIDDEN);
        }

        postRepository.delete(post);
    }

    @Transactional
    public boolean toggleLike(Long postId) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

        return likeRepository.findByUserAndPost(currentUser, post)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    return false;
                })
                .orElseGet(() -> {
                    likeRepository.save(Like.builder().user(currentUser).post(post).build());
                    return true;
                });
    }

    @Transactional
    public void unlikePost(Long postId) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

        likeRepository.findByUserAndPost(currentUser, post)
                .ifPresent(likeRepository::delete);
    }

    @Transactional
    public CommentResponse addComment(Long postId, String content) {
        User currentUser = currentUser();
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

        validateContentForModeration(content);
        Comment comment = Comment.builder()
                .content(content)
                .user(currentUser)
                .post(post)
                .build();

        return mapToCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public CommentResponse replyComment(Long commentId, String content) {
        User currentUser = currentUser();
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment không tồn tại", HttpStatus.NOT_FOUND));

        validateContentForModeration(content);
        Comment reply = Comment.builder()
                .content(content)
                .user(currentUser)
                .post(parentComment.getPost())
                .parentComment(parentComment)
                .build();

        return mapToCommentResponse(commentRepository.save(reply));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, String content) {
        User currentUser = currentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment không tồn tại", HttpStatus.NOT_FOUND));

        if (!comment.getUser().getId().equals(currentUser.getId()) && !canModerate(currentUser)) {
            throw new AppException("Bạn không có quyền chỉnh sửa bình luận này", HttpStatus.FORBIDDEN);
        }

        validateContentForModeration(content);
        comment.setContent(content);
        return mapToCommentResponse(commentRepository.save(comment));
    }

    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = currentUser();
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException("Comment không tồn tại", HttpStatus.NOT_FOUND));

        if (!comment.getUser().getId().equals(currentUser.getId()) && !canModerate(currentUser)) {
            throw new AppException("Bạn không có quyền xóa bình luận này", HttpStatus.FORBIDDEN);
        }

        commentRepository.delete(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));

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

        return reportRepository.save(report);
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
        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .location(post.getLocation())
                .ownerId(post.getUser().getId())
                .ownerName(post.getUser().getFullName())
                .ownerAvatar(post.getUser().getAvatarUrl())
                .createdAt(post.getCreatedAt())
                .likesCount(likeRepository.countByPost(post))
                .commentsCount(post.getComments().size())
                .isLiked(currentUser != null && likeRepository.existsByUserAndPost(currentUser, post))
                .build();
    }
}
