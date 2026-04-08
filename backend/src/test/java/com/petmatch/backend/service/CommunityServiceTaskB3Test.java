package com.petmatch.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.petmatch.backend.dto.request.CommunityReportRequest;
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

@ExtendWith(MockitoExtension.class)
class CommunityServiceTaskB3Test {

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

    @BeforeEach
    void setUp() {
        actor = User.builder()
                .id(1L)
                .email("actor@petmatch.dev")
                .fullName("Actor")
                .passwordHash("hash")
                .role(Role.USER)
                .build();

        authenticateAs(actor.getEmail());
        when(userRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void submitReport_shouldSavePendingReportForPostTarget() {
        CommunityReportRequest request = new CommunityReportRequest();
        request.setTargetType(ReportTargetType.POST);
        request.setTargetId(20L);
        request.setReason("Spam content");

        when(postRepository.existsById(20L)).thenReturn(true);
        when(reportRepository.save(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            report.setId(101L);
            return report;
        });

        Report saved = communityService.submitReport(request);

        assertEquals(101L, saved.getId());
        assertEquals(ReportStatus.PENDING, saved.getStatus());
        assertEquals(ReportTargetType.POST, saved.getTargetType());
        assertEquals(20L, saved.getTargetId());
        assertEquals(actor.getId(), saved.getReporter().getId());

        ArgumentCaptor<Report> reportCaptor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(reportCaptor.capture());
        assertEquals("Spam content", reportCaptor.getValue().getReason());
    }

    @Test
    void submitReport_shouldThrowNotFoundWhenPostTargetMissing() {
        CommunityReportRequest request = new CommunityReportRequest();
        request.setTargetType(ReportTargetType.POST);
        request.setTargetId(999L);
        request.setReason("Not found target");

        when(postRepository.existsById(999L)).thenReturn(false);

        AppException ex = assertThrows(AppException.class, () -> communityService.submitReport(request));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void submitReport_shouldThrowBadRequestForUnsupportedTargetType() {
        CommunityReportRequest request = new CommunityReportRequest();
        request.setTargetType(ReportTargetType.USER);
        request.setTargetId(2L);
        request.setReason("Unsupported target");

        AppException ex = assertThrows(AppException.class, () -> communityService.submitReport(request));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verifyNoInteractions(reportRepository);
    }

    @Test
    void createPost_shouldRejectBlockedContentWhenModerationEnabled() {
        ReflectionTestUtils.setField(communityService, "moderationEnabled", true);
        ReflectionTestUtils.setField(communityService, "blockedKeywords", List.of("scam", "fake"));

        AppException ex = assertThrows(AppException.class,
                () -> communityService.createPost("This is a scam offer", null, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createPost_shouldAllowBlockedKeywordWhenModerationDisabled() {
        ReflectionTestUtils.setField(communityService, "moderationEnabled", false);
        ReflectionTestUtils.setField(communityService, "blockedKeywords", new ArrayList<>(List.of("scam")));

        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> {
            Post post = invocation.getArgument(0);
            post.setId(77L);
            return post;
        });
        when(likeRepository.countByPost(any(Post.class))).thenReturn(0L);
        when(likeRepository.existsByUserAndPost(any(User.class), any(Post.class))).thenReturn(false);

        var response = communityService.createPost("This is a scam offer", null, "HN");

        assertNotNull(response);
        assertEquals(77L, response.getId());
        assertEquals("This is a scam offer", response.getContent());
    }

    private void authenticateAs(String email) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(email, null));
    }
}
