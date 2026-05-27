package com.petmatch.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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
import com.petmatch.backend.dto.response.CommunityNotificationResponse;
import com.petmatch.backend.entity.Comment;
import com.petmatch.backend.entity.CommunityNotification;
import com.petmatch.backend.entity.HiddenPost;
import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.CommunityNotificationType;
import com.petmatch.backend.enums.ReportStatus;
import com.petmatch.backend.enums.ReportTargetType;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.CommentRepository;
import com.petmatch.backend.repository.CommunityNotificationRepository;
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
    private static final double DISTANCE_WEIGHT = 35.0;
    private static final double SPECIES_WEIGHT = 20.0;
    private static final double RECENCY_WEIGHT = 25.0;
    private static final double ENGAGEMENT_WEIGHT = 20.0;
    private static final int RECENCY_FULL_SCORE_HOURS = 72;
    private static final int RANDOM_NEW_POST_HOURS = 24;
    private static final double RANDOM_NEW_POST_RATIO = 0.20;

    /**
     * Service xử lý nghiệp vụ cho module Community (mạng xã hội).
     * - Chịu trách nhiệm tạo / sửa / xóa bài viết
     * - Xử lý like / unlike
     * - Thêm / trả lời / sửa / xoá bình luận
     * - Gửi / lưu report cho post/comment
     * - (Có hỗ trợ) quản lý notification liên quan đến community
     *
     * Lưu ý: các phương thức public tuân theo ràng buộc transaction ở annotation.
     */
    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final CommunityNotificationRepository communityNotificationRepository;
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
        // Lấy user hiện tại từ SecurityContext (authentication đã được thiết lập)
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new AppException("User không tồn tại", HttpStatus.NOT_FOUND));
    }

    private Post requirePost(Long postId) {
        // Nạp Post hoặc ném 404 nếu không tồn tại
        return postRepository.findById(postId)
            .orElseThrow(() -> new AppException("Post không tồn tại", HttpStatus.NOT_FOUND));
    }

    private Comment requireComment(Long commentId) {
        // Nạp Comment hoặc ném 404 nếu không tồn tại
        return commentRepository.findById(commentId)
            .orElseThrow(() -> new AppException("Comment không tồn tại", HttpStatus.NOT_FOUND));
    }

    private void assertCanManagePost(User actor, Post post, String action) {
        if (!post.getUser().getId().equals(actor.getId()) && !canModerate(actor)) {
            throw new AppException("Bạn không có quyền " + action + " bài viết này", HttpStatus.FORBIDDEN);
        }
    }

    // Kiểm tra quyền thao tác lên bình luận: chủ comment hoặc admin mới được phép
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

    // Helper: chuyển danh sách `Post` sang `PostResponse` dùng cho API trả về feed hoặc list
    
    private boolean canModerate(User user) {
        return user.getRole() == Role.ADMIN;
    }

    // Helper: kiểm tra role ADMIN (dùng để cho phép moderator override quyền của owner)

    private void validateContentForModeration(String content) {
        // Kiểm tra nội dung theo chính sách moderation (nếu bật)
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

    // Transaction: readOnly = true — chỉ đọc dữ liệu, không thay đổi DB
    @Transactional(readOnly = true)
    public List<PostResponse> getFeed() {
        // Nó áp dụng 3 lớp xử lý:
        // 1) Loại bài đã ẩn của user
        // 2) Chèn một phần bài mới gần đây theo tỷ lệ ngẫu nhiên để feed bớt cứng
        // 3) Xếp phần còn lại theo recommendationScore (điểm phù hợp)
        User actor = currentUser();
        List<Long> hiddenPostIds = hiddenPostRepository.findPostIdsByUserId(actor.getId());
        List<Post> posts = postRepository.findAllByOrderByCreatedAtDesc();
        if (hiddenPostIds != null && !hiddenPostIds.isEmpty()) {
            Set<Long> hiddenSet = new HashSet<>(hiddenPostIds);
            posts = posts.stream()
                .filter(post -> !hiddenSet.contains(post.getId()))
                    .collect(Collectors.toList());
        }
        if (posts.size() <= 1) {
            return toPostResponses(posts, actor);
        }

        String actorSpecies = petProfileRepository.findByOwnerId(actor.getId())
            .map(PetProfile::getSpecies)
            .orElse(null);

        List<Post> freshPosts = posts.stream()
            .filter(post -> post.getCreatedAt() != null)
            .filter(post -> post.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusHours(RANDOM_NEW_POST_HOURS)))
            .collect(Collectors.toList());

        int randomFreshCount = Math.min(
            freshPosts.size(),
            Math.max(0, (int) Math.round(posts.size() * RANDOM_NEW_POST_RATIO))
        );

        List<Post> randomFreshPosts = new ArrayList<>();
        if (randomFreshCount > 0) {
            List<Post> shuffled = new ArrayList<>(freshPosts);
            java.util.Collections.shuffle(shuffled, ThreadLocalRandom.current());
            randomFreshPosts = shuffled.subList(0, randomFreshCount);
        }

        Set<Long> randomFreshIds = randomFreshPosts.stream()
            .map(Post::getId)
            .collect(Collectors.toSet());

        List<Post> rankedPosts = posts.stream()
            .filter(post -> !randomFreshIds.contains(post.getId()))
            .sorted(Comparator.comparingDouble((Post post) -> recommendationScore(post, actor, actorSpecies)).reversed())
            .collect(Collectors.toList());

        List<Post> finalOrder = new ArrayList<>(posts.size());
        finalOrder.addAll(randomFreshPosts);
        finalOrder.addAll(rankedPosts);

        return toPostResponses(finalOrder, actor);
    }

    // Transaction: readOnly = true — lấy danh sách bài của user, không ghi DB
    @Transactional(readOnly = true)
    public List<PostResponse> getMyPosts() {
        User actor = currentUser();
        return toPostResponses(postRepository.findAllByUserOrderByCreatedAtDesc(actor), actor);
    }

    // Transaction: readOnly = true — đọc chi tiết 1 post
    @Transactional(readOnly = true)
    public PostResponse getPostDetail(Long postId) {
        User actor = currentUser();
        return mapToPostResponse(requirePost(postId), actor);
    }

    // Transaction: readOnly = true — lấy bài của 1 user theo id
    @Transactional(readOnly = true)
    public List<PostResponse> getPostsByUserId(Long userId) {
        User actor = currentUser();
        return toPostResponses(postRepository.findAllByUserIdOrderByCreatedAtDesc(userId), actor);
    }

    // Transaction: readOnly = false — tạo post mới, ghi vào DB
    @Transactional
    public PostResponse createPost(String content, String imageUrl, String location) {
        // Tạo bài mới, kiểm tra moderation, lưu và trả về DTO
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

    // Transaction: readOnly = false — upload 1 ảnh và tạo post
    @Transactional
    public PostResponse createPostWithImageUpload(String content, String location, MultipartFile imageFile) {
        List<MultipartFile> files = imageFile == null ? List.of() : List.of(imageFile);
        return createPostWithImageUploads(content, location, files);
    }

    // Transaction: readOnly = false — upload nhiều ảnh và tạo post
    @Transactional
    public PostResponse createPostWithImageUploads(String content, String location, List<MultipartFile> imageFiles) {
        // Upload nhiều file lên Cloudinary (nếu có) và ghép thành chuỗi URL, sau đó gọi createPost
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

    // Transaction: readOnly = false — cập nhật post, kiểm tra quyền và ghi DB
    @Transactional
    public PostResponse updatePost(Long postId, String content, String imageUrl, String location) {
        // Cập nhật bài: kiểm tra quyền (owner hoặc admin), validate nội dung, save
        User actor = currentUser();
        Post post = requirePost(postId);
        assertCanManagePost(actor, post, "chỉnh sửa");

        validateContentForModeration(content);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setLocation(location);

        return mapToPostResponse(postRepository.save(post), actor);
    }

    // Transaction: readOnly = false — cập nhật post kèm upload ảnh
    @Transactional
    public PostResponse updatePostWithImageUploads(
            Long postId,
            String content,
            String location,
            String existingImageUrls,
            List<MultipartFile> imageFiles
    ) {
        // Cập nhật post với upload ảnh: giữ lại existingImageUrls nếu có, upload file mới và ghép URL
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

    // Transaction: readOnly = false — xóa post nếu có quyền
    @Transactional
    public void deletePost(Long postId) {
        // Xoá bài, kiểm tra quyền
        User actor = currentUser();
        Post post = requirePost(postId);
        assertCanManagePost(actor, post, "xóa");

        postRepository.delete(post);
    }

    // Transaction: readOnly = false — tạo/xóa like, có thể sinh notification
    @Transactional
    public boolean toggleLike(Long postId) {
        // Thực hiện toggle like: nếu tồn tại like thì xóa, ngược lại tạo mới
        User actor = currentUser();
        Post post = requirePost(postId);

        return likeRepository.findByUserAndPost(actor, post)
                .map(existingLike -> {
                    likeRepository.delete(existingLike);
                    return false;
                })
                .orElseGet(() -> {
                    likeRepository.save(Like.builder().user(actor).post(post).build());
                    createPostLikeNotification(actor, post);
                    return true;
                });
    }

    // Transaction: readOnly = false — xóa like nếu tồn tại
    @Transactional
    public void unlikePost(Long postId) {
        User actor = currentUser();
        Post post = requirePost(postId);

        likeRepository.findByUserAndPost(actor, post)
                .ifPresent(likeRepository::delete);
    }

    // Transaction: readOnly = false — thêm bình luận, ghi DB và tạo notification
    @Transactional
    public CommentResponse addComment(Long postId, String content) {
        // Thêm bình luận vào bài: validate nội dung, lưu comment, tạo notification nếu cần
        User actor = currentUser();
        Post post = requirePost(postId);

        validateContentForModeration(content);
        Comment comment = Comment.builder()
            .content(content)
            .user(actor)
            .post(post)
            .build();

        Comment saved = commentRepository.save(comment);
        createPostCommentNotification(actor, post, saved);
        return mapToCommentResponse(saved);
    }

    // Transaction: readOnly = false — trả lời bình luận (lưu parent relation)
    @Transactional
    public CommentResponse replyComment(Long commentId, String content) {
        // Trả lời 1 bình luận: lưu parent relation, tạo notification cho chủ comment
        User actor = currentUser();
        Comment parentComment = requireComment(commentId);

        validateContentForModeration(content);
        Comment reply = Comment.builder()
            .content(content)
            .user(actor)
            .post(parentComment.getPost())
            .parentComment(parentComment)
            .build();

        Comment saved = commentRepository.save(reply);
        createCommentReplyNotification(actor, parentComment, saved);
        return mapToCommentResponse(saved);
    }

    // Transaction: readOnly = true — đọc notification; nếu markAsRead true thì vẫn gọi update riêng
    @Transactional(readOnly = true)
    public List<CommunityNotificationResponse> getMyNotifications(boolean markAsRead) {
        // Lấy danh sách notification cho user; nếu markAsRead=true thì đánh dấu đã đọc
        User currentUser = currentUser();
        List<CommunityNotification> notifications = communityNotificationRepository
            .findAllByRecipientOrderByCreatedAtDesc(currentUser);

        if (markAsRead) {
            communityNotificationRepository.markAllAsReadByRecipient(currentUser);
        }

        return notifications.stream()
            .map(this::mapToCommunityNotificationResponse)
            .collect(Collectors.toList());
    }

    // Transaction: readOnly = true — trả về số lượng notification chưa đọc
    @Transactional(readOnly = true)
    public long getUnreadNotificationCount() {
        User currentUser = currentUser();
        return communityNotificationRepository.countByRecipientAndIsReadFalse(currentUser);
    }

    // Transaction: readOnly = false — đánh dấu tất cả notification của user là đã đọc
    @Transactional
    public void markAllNotificationsAsRead() {
        User currentUser = currentUser();
        communityNotificationRepository.markAllAsReadByRecipient(currentUser);
    }

    // Transaction: readOnly = false — cập nhật nội dung bình luận
    @Transactional
    public CommentResponse updateComment(Long commentId, String content) {
        User actor = currentUser();
        Comment comment = requireComment(commentId);
        assertCanManageComment(actor, comment, "chỉnh sửa");

        validateContentForModeration(content);
        comment.setContent(content);
        return mapToCommentResponse(commentRepository.save(comment));
    }

    // Transaction: readOnly = false — xóa bình luận nếu có quyền
    @Transactional
    public void deleteComment(Long commentId) {
        User actor = currentUser();
        Comment comment = requireComment(commentId);
        assertCanManageComment(actor, comment, "xóa");

        commentRepository.delete(comment);
    }

    // Transaction: readOnly = true — lấy danh sách bình luận top-level cho 1 post
    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsByPost(Long postId) {
        Post post = requirePost(postId);

        return commentRepository.findAllByPostAndParentCommentIsNullOrderByCreatedAtAsc(post)
                .stream()
                .map(this::mapToCommentResponse)
                .collect(Collectors.toList());
    }

    // Transaction: readOnly = false — tạo report, có thể tạo bản ghi HiddenPost
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

    // Transaction: readOnly = true — lấy danh sách báo cáo user đã gửi
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
        // Đệ quy xây dựng danh sách reply cho comment
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
        // Lấy avatar chủ bài: ưu tiên avatar user, fallback lấy avatar con vật nếu có
        String ownerAvatar = post.getUser().getAvatarUrl();
        if ((ownerAvatar == null || ownerAvatar.isBlank()) && petProfileRepository != null && petPhotoRepository != null) {
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

    private double recommendationScore(Post post, User actor, String actorSpecies) {
        // Điểm tổng hợp = khoảng cách + cùng loài + độ mới + tương tác
        double distanceScore = distanceScore(actor, post.getUser());
        double speciesScore = speciesScore(actorSpecies, post.getUser().getId());
        double recencyScore = recencyScore(post);
        double engagementScore = engagementScore(post);
        return distanceScore + speciesScore + recencyScore + engagementScore;
    }

    private double distanceScore(User actor, User owner) {
        // Nếu thiếu tọa độ thì cho điểm trung tính thay vì loại bỏ hoàn toàn
        if (actor.getLatitude() == null || actor.getLongitude() == null
                || owner.getLatitude() == null || owner.getLongitude() == null) {
            return DISTANCE_WEIGHT * 0.30;
        }

        // Tính khoảng cách Haversine và đổi sang điểm theo khoảng cách thực tế
        double km = haversineKm(actor.getLatitude(), actor.getLongitude(), owner.getLatitude(), owner.getLongitude());
        if (km <= 1) return DISTANCE_WEIGHT;
        if (km <= 5) return DISTANCE_WEIGHT * 0.85;
        if (km <= 15) return DISTANCE_WEIGHT * 0.60;
        if (km <= 30) return DISTANCE_WEIGHT * 0.35;
        return DISTANCE_WEIGHT * 0.10;
    }

    private double speciesScore(String actorSpecies, Long ownerId) {
        // Ưu tiên bài của user cùng loài với pet của người đang xem feed
        if (actorSpecies == null || actorSpecies.isBlank()) {
            return SPECIES_WEIGHT * 0.40;
        }

        String ownerSpecies = petProfileRepository.findByOwnerId(ownerId)
                .map(PetProfile::getSpecies)
                .orElse(null);

        if (ownerSpecies == null || ownerSpecies.isBlank()) {
            return SPECIES_WEIGHT * 0.20;
        }

        return actorSpecies.trim().equalsIgnoreCase(ownerSpecies.trim()) ? SPECIES_WEIGHT : 0.0;
    }

    private double recencyScore(Post post) {
        // Bài càng mới càng có điểm cao; trong 72h đầu sẽ giảm dần theo thời gian
        if (post.getCreatedAt() == null) {
            return 0.0;
        }

        long hours = java.time.temporal.ChronoUnit.HOURS.between(post.getCreatedAt(), java.time.LocalDateTime.now());
        if (hours <= 0) {
            return RECENCY_WEIGHT;
        }
        if (hours >= RECENCY_FULL_SCORE_HOURS) {
            return 0.0;
        }
        double ratio = 1.0 - ((double) hours / RECENCY_FULL_SCORE_HOURS);
        return RECENCY_WEIGHT * ratio;
    }

    private double engagementScore(Post post) {
        // Engagement = like + comment (comment tính trọng số cao hơn like)
        long likes = likeRepository.countByPost(post);
        long comments = post.getComments() == null ? 0 : post.getComments().size();
        double raw = likes + comments * 2.0;
        double normalized = Math.min(1.0, raw / 20.0);
        return ENGAGEMENT_WEIGHT * normalized;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        // Công thức Haversine để tính khoảng cách địa lý giữa 2 tọa độ
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 6371.0 * c;
    }

    private void createPostLikeNotification(User actor, Post post) {
        // NOTE: Notification subsystem chưa phát triển hoàn chỉnh.
        // Các hàm liên quan đến notification hiện chỉ lưu record vào DB như placeholder.
        // Phần gửi push/websocket/chi tiết UI chưa được triển khai — xem TODO để mở rộng sau.
        User recipient = post.getUser();
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        boolean exists = communityNotificationRepository.existsByRecipientAndActorAndPostAndType(
                recipient,
                actor,
                post,
                CommunityNotificationType.POST_LIKE
        );
        if (exists) {
            return;
        }

        communityNotificationRepository.save(CommunityNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .post(post)
                .type(CommunityNotificationType.POST_LIKE)
                .isRead(false)
                .build());
    }

    private void createPostCommentNotification(User actor, Post post, Comment comment) {
        // NOTE: Notification lưu vào DB như placeholder. Chức năng gửi/hiển thị notification chưa hoàn thiện.
        User recipient = post.getUser();
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        communityNotificationRepository.save(CommunityNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .post(post)
                .comment(comment)
                .type(CommunityNotificationType.POST_COMMENT)
                .isRead(false)
                .build());
    }

    private void createCommentReplyNotification(User actor, Comment parentComment, Comment reply) {
        // NOTE: Placeholder lưu notification khi reply. Chức năng push/hiển thị cần triển khai riêng.
        User recipient = parentComment.getUser();
        if (recipient.getId().equals(actor.getId())) {
            return;
        }

        communityNotificationRepository.save(CommunityNotification.builder()
                .recipient(recipient)
                .actor(actor)
                .post(parentComment.getPost())
                .comment(reply)
                .type(CommunityNotificationType.COMMENT_REPLY)
                .isRead(false)
                .build());
    }

    private CommunityNotificationResponse mapToCommunityNotificationResponse(CommunityNotification notification) {
        // Chú ý: chuyển đổi notification sang DTO cho API. Nội dung message hiện hard-coded,
        // nếu muốn support nhiều loại/đa ngôn ngữ hoặc template thì cần refactor.
        User actor = notification.getActor();
        String actorName = actor != null ? actor.getFullName() : "Ai đó";
        String message = switch (notification.getType()) {
            case POST_LIKE -> actorName + " đã thả tim bài viết của bạn";
            case POST_COMMENT -> actorName + " đã bình luận bài viết của bạn";
            case COMMENT_REPLY -> actorName + " đã trả lời bình luận của bạn";
        };

        return CommunityNotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .message(message)
                .postId(notification.getPost() != null ? notification.getPost().getId() : null)
                .commentId(notification.getComment() != null ? notification.getComment().getId() : null)
                .actorId(actor != null ? actor.getId() : null)
                .actorName(actorName)
                .actorAvatar(actor != null ? actor.getAvatarUrl() : null)
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
