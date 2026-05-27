package com.petmatch.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.petmatch.backend.dto.PostResponse;
import com.petmatch.backend.dto.request.CommunityReportRequest;
import com.petmatch.backend.dto.request.CreateCommentRequest;
import com.petmatch.backend.dto.request.CreatePostRequest;
import com.petmatch.backend.dto.request.UpdateCommentRequest;
import com.petmatch.backend.dto.request.UpdatePostRequest;
import com.petmatch.backend.dto.response.CommentResponse;
import com.petmatch.backend.dto.response.CommunityNotificationResponse;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.service.CommunityService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;

    /**
     * Controller cho module cộng đồng (Community).
     * Bao gồm các endpoint để: lấy feed, quản lý bài đăng (CRUD),
     * like/unlike, bình luận, trả lời bình luận, report và thông báo.
     *
     * Ghi chú: tất cả endpoint trả về DTO/Response đã chuẩn hoá ở Service.
     */

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed() {
        // Lấy feed cho người đang đăng nhập, bao gồm các bài đã lọc/ẩn
        return ResponseEntity.ok(communityService.getFeed());
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponse>> getMyPosts() {
        // Lấy danh sách bài đăng của user hiện tại
        return ResponseEntity.ok(communityService.getMyPosts());
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getPostDetail(@PathVariable Long id) {
        // Chi tiết 1 bài đăng
        return ResponseEntity.ok(communityService.getPostDetail(id));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<PostResponse>> getPostsByUser(@PathVariable Long userId) {
        // Lấy danh sách bài của 1 user theo id
        return ResponseEntity.ok(communityService.getPostsByUserId(userId));
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
        // Tạo bài đăng đơn giản (không upload file), trả về PostResponse
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createPost(
                        request.getContent(),
                        request.getImageUrl(),
                        request.getLocation()
                ));
    }

    @PostMapping(value = "/posts/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPostWithUpload(
            @RequestPart("content") String content,
            @RequestPart(value = "location", required = false) String location,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        // Chuẩn hoá danh sách ảnh: ưu tiên `images` nếu có, tiếp theo `image` đơn
        List<MultipartFile> uploadImages;
        if (images != null && !images.isEmpty()) {
            uploadImages = images;
        } else if (image != null) {
            uploadImages = List.of(image);
        } else {
            uploadImages = List.of();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.createPostWithImageUploads(content, location, uploadImages));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<PostResponse> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        // Cập nhật nội dung/ảnh/vị trí bài viết
        return ResponseEntity.ok(communityService.updatePost(
                id,
                request.getContent(),
                request.getImageUrl(),
                request.getLocation()
        ));
    }

    @PutMapping(value = "/posts/{id}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> updatePostWithUpload(
            @PathVariable Long id,
            @RequestPart("content") String content,
            @RequestPart(value = "location", required = false) String location,
            @RequestPart(value = "existingImageUrls", required = false) String existingImageUrls,
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        // Chuẩn hoá file upload tương tự create
        List<MultipartFile> uploadImages;
        if (images != null && !images.isEmpty()) {
            uploadImages = images;
        } else if (image != null) {
            uploadImages = List.of(image);
        } else {
            uploadImages = List.of();
        }

        return ResponseEntity.ok(
                communityService.updatePostWithImageUploads(id, content, location, existingImageUrls, uploadImages)
        );
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        // Xoá bài viết (kiểm tra quyền bên service)
        communityService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        // Toggle like: nếu đã like thì un-like, ngược lại tạo like
        boolean liked = communityService.toggleLike(id);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable Long id) {
        // Xoá like nếu tồn tại
        communityService.unlikePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        // Thêm bình luận mới cho bài viết (không support attach ảnh hiện tại)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(communityService.addComment(id, request.getContent()));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {
        // Trả lời 1 bình luận (parent comment)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(communityService.replyComment(commentId, request.getContent()));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        // Cập nhật nội dung bình luận (chỉ author hoặc admin có quyền)
        return ResponseEntity.ok(communityService.updateComment(commentId, request.getContent()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        // Xoá bình luận (kiểm tra quyền bên service)
        communityService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getPostComments(@PathVariable Long id) {
        // Lấy danh sách bình luận top-level cho 1 bài
        return ResponseEntity.ok(communityService.getCommentsByPost(id));
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> submitReport(@Valid @RequestBody CommunityReportRequest request) {
        // Gửi báo cáo (report) cho post/comment; có thể ẩn bài cho reporter nếu chọn
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(communityService.submitReport(request));
    }

    @GetMapping("/reports/me")
    public ResponseEntity<List<Report>> getMyReports() {
        // Lấy các báo cáo user đã gửi
        return ResponseEntity.ok(communityService.getMyReports());
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<CommunityNotificationResponse>> getMyNotifications(
            @RequestParam(name = "markRead", defaultValue = "true") boolean markRead
    ) {
        // Lấy thông báo cho user (markRead true để đánh dấu đã đọc)
        return ResponseEntity.ok(communityService.getMyNotifications(markRead));
    }

    @GetMapping("/notifications/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount() {
        // Đếm thông báo chưa đọc
        return ResponseEntity.ok(Map.of("count", communityService.getUnreadNotificationCount()));
    }

    @PostMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead() {
        // Đánh dấu tất cả thông báo đã đọc
        communityService.markAllNotificationsAsRead();
        return ResponseEntity.noContent().build();
    }
}
