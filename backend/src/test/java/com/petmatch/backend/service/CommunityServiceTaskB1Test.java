package com.petmatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

import com.petmatch.backend.dto.PostResponse;
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
class CommunityServiceTaskB1Test {

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

    private User owner;

    @BeforeEach
    void setUp() {
        owner = User.builder()
                .id(1L)
                .email("owner@petmatch.dev")
                .fullName("Owner")
                .passwordHash("hash")
                .role(Role.USER)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createPost_shouldSaveAndReturnPostResponse() {
        authenticateAs(owner.getEmail());
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        when(likeRepository.countByPost(any(Post.class))).thenReturn(0L);
        when(likeRepository.existsByUserAndPost(any(User.class), any(Post.class))).thenReturn(false);

        PostResponse response = communityService.createPost("Xin chao cong dong", "https://img", "HCM");

        assertEquals(100L, response.getId());
        assertEquals("Xin chao cong dong", response.getContent());
        assertEquals(owner.getId(), response.getOwnerId());

        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        assertEquals("Xin chao cong dong", postCaptor.getValue().getContent());
        assertEquals("https://img", postCaptor.getValue().getImageUrl());
        assertEquals("HCM", postCaptor.getValue().getLocation());
    }

    @Test
    void updatePost_shouldUpdateWhenActorIsOwner() {
        authenticateAs(owner.getEmail());
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        Post existing = Post.builder()
                .id(10L)
                .content("old")
                .imageUrl("oldImg")
                .location("oldLoc")
                .user(owner)
                .build();

        when(postRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(postRepository.save(existing)).thenReturn(existing);
        when(likeRepository.countByPost(existing)).thenReturn(0L);
        when(likeRepository.existsByUserAndPost(owner, existing)).thenReturn(false);

        PostResponse response = communityService.updatePost(10L, "new", "newImg", "newLoc");

        assertEquals("new", response.getContent());
        assertEquals("newImg", response.getImageUrl());
        assertEquals("newLoc", response.getLocation());
        verify(postRepository).save(existing);
    }

    @Test
    void updatePost_shouldThrowForbiddenWhenActorIsNotOwner() {
        User actor = User.builder()
                .id(2L)
                .email("other@petmatch.dev")
                .fullName("Other")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        authenticateAs(actor.getEmail());
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));

        Post existing = Post.builder()
                .id(11L)
                .content("old")
                .user(owner)
                .build();
        when(postRepository.findById(11L)).thenReturn(Optional.of(existing));

        AppException ex = assertThrows(AppException.class,
                () -> communityService.updatePost(11L, "new", null, null));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    void deletePost_shouldDeleteWhenActorIsOwner() {
        authenticateAs(owner.getEmail());
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));

        Post existing = Post.builder()
                .id(12L)
                .content("x")
                .user(owner)
                .build();
        when(postRepository.findById(12L)).thenReturn(Optional.of(existing));

        communityService.deletePost(12L);

        verify(postRepository).delete(existing);
    }

    @Test
    void deletePost_shouldThrowNotFoundWhenPostMissing() {
        authenticateAs(owner.getEmail());
        when(userRepository.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
        when(postRepository.findById(999L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> communityService.deletePost(999L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(postRepository, never()).delete(any(Post.class));
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }
}
