package com.petmatch.backend.service;

import com.petmatch.backend.dto.response.MatchRequestResponse;
import com.petmatch.backend.dto.response.SuperLikeStatusResponse;
import com.petmatch.backend.entity.Match;
import com.petmatch.backend.entity.MatchRequest;
import com.petmatch.backend.entity.PetPhoto;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.MatchStatus;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.BlockRepository;
import com.petmatch.backend.repository.MatchRepository;
import com.petmatch.backend.repository.MatchRequestRepository;
import com.petmatch.backend.repository.PetPhotoRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchRequestService {

    private final MatchRequestRepository matchRepo;
    private final MatchRepository matchRepository;
    private final PetProfileRepository petProfileRepo;
    private final PetPhotoRepository petPhotoRepo;
    private final BlockRepository blockRepo;
    private final UserRepository userRepo;
    private final AiMatchingService aiMatchingService;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", NOT_FOUND));
    }

    private PetProfile myPet() {
        return petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Bạn chưa có hồ sơ thú cưng", NOT_FOUND));
    }

    private String avatarOf(PetProfile pet) {
        return petPhotoRepo.findByPetIdAndIsAvatarTrue(pet.getId())
                .map(PetPhoto::getPhotoUrl).orElse(null);
    }

    // ── Super Like Status ─────────────────────────────────
    @Transactional(readOnly = true)
    public SuperLikeStatusResponse getSuperLikeStatus() {
        PetProfile pet = myPet();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        boolean used = matchRepo.existsBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(
                pet.getId(), startOfDay);
        LocalDateTime nextReset = LocalDate.now().plusDays(1).atStartOfDay();
        return SuperLikeStatusResponse.builder()
                .canSuperLike(!used)
                .nextResetAt(nextReset)
                .usedToday(used ? 1 : 0)
                .build();
    }

    // ── Send Match Request (Like / Super Like / Discard) ──
    public MatchRequestResponse sendRequest(Long receiverPetId, boolean isSuperLike) {
        PetProfile sender   = myPet();
        PetProfile receiver = petProfileRepo.findById(receiverPetId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ", NOT_FOUND));

        if (sender.getId().equals(receiverPetId))
            throw new AppException("Không thể gửi cho chính mình", BAD_REQUEST);

        if (blockRepo.existsByBlockerIdAndBlockedId(
                currentUser().getId(), receiver.getOwner().getId()))
            throw new AppException("Bạn đã chặn người này", BAD_REQUEST);

        if (matchRepo.existsBySenderPetIdAndReceiverPetId(sender.getId(), receiverPetId))
            throw new AppException("Đã gửi yêu cầu trước đó", CONFLICT);

        // Validate super like quota (1 lần/ngày)
        if (isSuperLike) {
            LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
            if (matchRepo.existsBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(
                    sender.getId(), startOfDay))
                throw new AppException("Bạn đã dùng Super Like hôm nay rồi. Hãy quay lại vào ngày mai!", BAD_REQUEST);
        }

        MatchRequest req = MatchRequest.builder()
                .senderPet(sender)
                .receiverPet(receiver)
                .status(MatchStatus.PENDING)
                .isSuperLike(isSuperLike)
                .build();

        MatchRequest saved = matchRepo.save(req);

        // ── Tinder-style auto-match ────────────────────────
        // Nếu bên kia đã like mình trước → auto ACCEPTED cả 2
        Optional<MatchRequest> reverse = matchRepo.findBySenderPetIdAndReceiverPetId(
                receiverPetId, sender.getId());
        if (reverse.isPresent() && reverse.get().getStatus() == MatchStatus.PENDING) {
            saved.setStatus(MatchStatus.ACCEPTED);
            reverse.get().setStatus(MatchStatus.ACCEPTED);
            matchRepo.save(reverse.get());
            matchRepo.save(saved);
            
            // ── Create User-level Match for chat ───────────────────────
            createOrUpdateUserMatch(sender.getOwner(), receiver.getOwner());
        }

        // ── Cập nhật preference cho AI scoring ───────────────
        try { aiMatchingService.updatePreferences(sender.getId()); }
        catch (Exception ignored) { /* không block luồng chính */ }

        return toResponse(saved);
    }

    /**
     * Tạo Match record giữa 2 user nếu chưa tồn tại,
     * để cho chat list có thể hiển thị cuộc trò chuyện.
     */
    private void createOrUpdateUserMatch(User user1, User user2) {
        // Kiểm tra Match đã tồn tại chưa (2 chiều)
        boolean matchExists = matchRepository.findMatchByUserIds(user1.getId(), user2.getId()).isPresent();
        
        if (!matchExists) {
            Match match = Match.builder()
                    .user1(user1)
                    .user2(user2)
                    .build();
            matchRepository.save(match);
        }
    }

    // ── Respond (vẫn giữ cho trường hợp manual nếu cần) ───
    public MatchRequestResponse respond(Long matchId, MatchStatus newStatus) {
        MatchRequest match = matchRepo.findById(matchId)
                .orElseThrow(() -> new AppException("Không tìm thấy yêu cầu", NOT_FOUND));

        if (!match.getReceiverPet().getOwner().getId().equals(currentUser().getId()))
            throw new AppException("Không có quyền phản hồi", FORBIDDEN);

        if (match.getStatus() != MatchStatus.PENDING)
            throw new AppException("Yêu cầu đã được xử lý", BAD_REQUEST);

        match.setStatus(newStatus);
        MatchRequest saved = matchRepo.save(match);
        
        // ── Nếu ACCEPTED, tạo user-level Match cho chat ─────────────────
        if (newStatus == MatchStatus.ACCEPTED) {
            createOrUpdateUserMatch(match.getSenderPet().getOwner(), match.getReceiverPet().getOwner());
        }
        
        return toResponse(saved);
    }

    // ── Lists ─────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<MatchRequestResponse> getMySentRequests() {
        return matchRepo.findBySenderPetIdOrderByCreatedAtDesc(myPet().getId())
                .stream().map(this::toResponse).toList();
    }

    /** Ai đã like/super-like mình – super like xếp trước */
    @Transactional(readOnly = true)
    public List<MatchRequestResponse> getWhoLikedMe() {
        return matchRepo.findByReceiverPetIdOrderByIsSuperLikeDescCreatedAtDesc(myPet().getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MatchRequestResponse> getMyMatches() {
        return matchRepo.findAcceptedByPetId(myPet().getId())
                .stream().map(this::toResponse).toList();
    }

    // ── Helper ────────────────────────────────────────────
    private boolean isMutual(MatchRequest m) {
        return matchRepo.isMatched(m.getSenderPet().getId(), m.getReceiverPet().getId())
                && matchRepo.isMatched(m.getReceiverPet().getId(), m.getSenderPet().getId());
    }

    private MatchRequestResponse toResponse(MatchRequest m) {
        return MatchRequestResponse.builder()
                .id(m.getId())
                .senderPetId(m.getSenderPet().getId())
                .senderPetName(m.getSenderPet().getName())
                .senderPetAvatarUrl(avatarOf(m.getSenderPet()))
                .receiverPetId(m.getReceiverPet().getId())
                .receiverPetName(m.getReceiverPet().getName())
                .receiverPetAvatarUrl(avatarOf(m.getReceiverPet()))
                .status(m.getStatus().name())
                .isSuperLike(m.getIsSuperLike())
                .createdAt(m.getCreatedAt())
                .canOpenConversation(m.getStatus() == MatchStatus.ACCEPTED && isMutual(m))
                .build();
    }

    // ── Data Migration for existing Matches ─────────────────
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void syncOldMatches() {
        System.out.println("Running data migration: Syncing old MatchRequests to User Matches...");
        List<MatchRequest> acceptedRequests = matchRepo.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.ACCEPTED)
                .toList();

        for (MatchRequest req : acceptedRequests) {
            try {
                createOrUpdateUserMatch(req.getSenderPet().getOwner(), req.getReceiverPet().getOwner());
            } catch (Exception e) {
                System.err.println("Failed to sync match for req " + req.getId() + ": " + e.getMessage());
            }
        }
        System.out.println("Data migration completed!");
    }
}
