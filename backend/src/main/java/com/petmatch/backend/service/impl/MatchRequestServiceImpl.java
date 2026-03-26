package com.petmatch.backend.service.impl;

import com.petmatch.backend.dto.response.MatchRequestResponse;
import com.petmatch.backend.entity.MatchRequest;
import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.MatchStatus;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.BlockRepository;
import com.petmatch.backend.repository.MatchRequestRepository;
import com.petmatch.backend.repository.PetProfileRepository;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.MatchRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MatchRequestServiceImpl implements MatchRequestService {

    private final MatchRequestRepository matchRepo;
    private final PetProfileRepository petProfileRepo;
    private final BlockRepository blockRepo;
    private final UserRepository userRepo;

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", NOT_FOUND));
    }

    private PetProfile myPet() {
        return petProfileRepo.findByOwnerId(currentUser().getId())
                .orElseThrow(() -> new AppException("Bạn chưa có hồ sơ thú cưng", NOT_FOUND));
    }

    @Override
    public MatchRequestResponse sendRequest(UUID receiverPetId) {
        PetProfile sender   = myPet();
        PetProfile receiver = petProfileRepo.findById(receiverPetId)
                .orElseThrow(() -> new AppException("Không tìm thấy hồ sơ", NOT_FOUND));

        // Không tự gửi cho chính mình
        if (sender.getId().equals(receiverPetId))
            throw new AppException("Không thể gửi cho chính mình", BAD_REQUEST);

        // Kiểm tra block
        if (blockRepo.existsByBlockerIdAndBlockedId(
                currentUser().getId(), receiver.getOwner().getId()))
            throw new AppException("Bạn đã chặn người này", BAD_REQUEST);

        // Kiểm tra đã gửi chưa
        if (matchRepo.existsBySenderPetIdAndReceiverPetId(sender.getId(), receiverPetId))
            throw new AppException("Đã gửi yêu cầu trước đó", CONFLICT);

        MatchRequest req = MatchRequest.builder()
                .senderPet(sender)
                .receiverPet(receiver)
                .status(MatchStatus.PENDING)
                .build();

        return toResponse(matchRepo.save(req));
    }

    @Override
    public MatchRequestResponse respond(UUID matchId, MatchStatus newStatus) {
        MatchRequest match = matchRepo.findById(matchId)
                .orElseThrow(() -> new AppException("Không tìm thấy yêu cầu", NOT_FOUND));

        // Chỉ receiver mới được phản hồi
        if (!match.getReceiverPet().getOwner().getId().equals(currentUser().getId()))
            throw new AppException("Không có quyền phản hồi", FORBIDDEN);

        if (match.getStatus() != MatchStatus.PENDING)
            throw new AppException("Yêu cầu đã được xử lý", BAD_REQUEST);

        match.setStatus(newStatus);
        return toResponse(matchRepo.save(match));
    }

    @Override
    public List<MatchRequestResponse> getMySentRequests() {
        return matchRepo.findBySenderPetIdOrderByCreatedAtDesc(myPet().getId())
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<MatchRequestResponse> getMyPendingReceived() {
        return matchRepo.findByReceiverPetIdAndStatusOrderByCreatedAtDesc(
                        myPet().getId(), MatchStatus.PENDING)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<MatchRequestResponse> getMyMatches() {
        return matchRepo.findAcceptedByPetId(myPet().getId())
                .stream().map(this::toResponse).toList();
    }

    // canOpenConversation = cả 2 phía đều accepted (mutual match)
    private boolean isMutual(MatchRequest m) {
        return matchRepo.isMatched(m.getSenderPet().getId(), m.getReceiverPet().getId())
                && matchRepo.isMatched(m.getReceiverPet().getId(), m.getSenderPet().getId());
    }

    private MatchRequestResponse toResponse(MatchRequest m) {
        return MatchRequestResponse.builder()
                .id(m.getId())
                .senderPetId(m.getSenderPet().getId())
                .senderPetName(m.getSenderPet().getName())
                .receiverPetId(m.getReceiverPet().getId())
                .receiverPetName(m.getReceiverPet().getName())
                .status(m.getStatus().name())
                .createdAt(m.getCreatedAt())
                .canOpenConversation(m.getStatus() == MatchStatus.ACCEPTED && isMutual(m))
                .build();
    }
}
