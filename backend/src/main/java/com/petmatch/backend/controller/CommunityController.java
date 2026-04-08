package com.petmatch.backend.controller;

import com.petmatch.backend.dto.PostResponse;
import com.petmatch.backend.dto.request.CommunityReportRequest;
import com.petmatch.backend.dto.request.CreateCommentRequest;
import com.petmatch.backend.dto.request.CreatePostRequest;
import com.petmatch.backend.dto.request.UpdateCommentRequest;
import com.petmatch.backend.dto.request.UpdatePostRequest;
import com.petmatch.backend.dto.response.CommentResponse;
import com.petmatch.backend.entity.Report;
import com.petmatch.backend.service.CommunityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {
    private final CommunityService communityService;

    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed() {
        return ResponseEntity.ok(communityService.getFeed());
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostResponse>> getMyPosts() {
        return ResponseEntity.ok(communityService.getMyPosts());
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostResponse> getPostDetail(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getPostDetail(id));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<PostResponse>> getPostsByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(communityService.getPostsByUserId(userId));
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody CreatePostRequest request) {
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
        communityService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/like")
    public ResponseEntity<Map<String, Object>> toggleLike(@PathVariable Long id) {
        boolean liked = communityService.toggleLike(id);
        return ResponseEntity.ok(Map.of("liked", liked));
    }

    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<Void> unlikePost(@PathVariable Long id) {
        communityService.unlikePost(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.addComment(id, request.getContent()));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.replyComment(commentId, request.getContent()));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        return ResponseEntity.ok(communityService.updateComment(commentId, request.getContent()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        communityService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<CommentResponse>> getPostComments(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.getCommentsByPost(id));
    }

    @PostMapping("/reports")
    public ResponseEntity<Report> submitReport(@Valid @RequestBody CommunityReportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(communityService.submitReport(request));
    }

    @GetMapping("/reports/me")
    public ResponseEntity<List<Report>> getMyReports() {
        return ResponseEntity.ok(communityService.getMyReports());
    }
}
