package com.petmatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.petmatch.backend.dto.response.CommentResponse;
import com.petmatch.backend.entity.Comment;
import com.petmatch.backend.entity.Like;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.Role;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.CommentRepository;
import com.petmatch.backend.repository.LikeRepository;
import com.petmatch.backend.repository.PostRepository;
import com.petmatch.backend.repository.ReportRepository;
import com.petmatch.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTaskB2Test {

    @Mock
    private PostRepository postRepository;
    @Mock
    private LikeRepository likeRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ReportRepository reportRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CloudinaryService cloudinaryService;

    @InjectMocks
    private CommunityService communityService;

    private User actor;
    private Post post;

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .id(1L)
                .email("actor@petmatch.dev")
                .fullName("Actor")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        post = Post.builder()
                .id(10L)
                .content("post")
                .user(actor)
                .build();

        authenticateAs(actor.getEmail());
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void toggleLike_shouldCreateLikeWhenNotExists() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUserAndPost(actor, post)).thenReturn(Optional.empty());

        boolean liked = communityService.toggleLike(10L);

        assertTrue(liked);
        ArgumentCaptor<Like> captor = ArgumentCaptor.forClass(Like.class);
        verify(likeRepository).save(captor.capture());
        assertEquals(actor.getId(), captor.getValue().getUser().getId());
        assertEquals(post.getId(), captor.getValue().getPost().getId());
    }

    @Test
    void toggleLike_shouldRemoveLikeWhenExists() {
        Like existing = Like.builder().id(5L).user(actor).post(post).build();
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(likeRepository.findByUserAndPost(actor, post)).thenReturn(Optional.of(existing));

        boolean liked = communityService.toggleLike(10L);

        assertFalse(liked);
        verify(likeRepository).delete(existing);
    }

    @Test
    void addComment_shouldSaveAndReturnCommentResponse() {
        when(postRepository.findById(10L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment c = invocation.getArgument(0);
            c.setId(77L);
            return c;
        });
        when(commentRepository.findAllByParentCommentOrderByCreatedAtAsc(any(Comment.class)))
                .thenReturn(Collections.emptyList());

        CommentResponse response = communityService.addComment(10L, "hello comment");

        assertEquals(77L, response.getId());
        assertEquals(10L, response.getPostId());
        assertEquals("hello comment", response.getContent());
        assertEquals(actor.getId(), response.getUserId());
        assertEquals(null, response.getParentCommentId());
    }

    @Test
    void addComment_shouldThrowNotFoundWhenPostMissing() {
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> communityService.addComment(999L, "x"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void replyComment_shouldThrowNotFoundWhenParentMissing() {
        when(commentRepository.findById(333L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> communityService.replyComment(333L, "reply"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }
}
