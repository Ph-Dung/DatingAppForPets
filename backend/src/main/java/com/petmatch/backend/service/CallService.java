package com.petmatch.backend.service;

import com.petmatch.backend.dto.CallRequest;
import com.petmatch.backend.entity.*;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallHistoryRepository callHistoryRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final BlockRepository blockRepository;

    @Transactional
    public CallHistory initiateCall(Long callerId, CallRequest request) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new RuntimeException("Caller not found"));
        User callee = userRepository.findById(request.getCalleeId())
                .orElseThrow(() -> new RuntimeException("Callee not found"));

        // Match check
        if (matchRepository.findMatchByUsers(caller, callee).isEmpty()) {
            throw new AppException("Chỉ có thể gọi cho người đã match", HttpStatus.FORBIDDEN);
        }

        // Block check: cấp CALL hoặc ALL
        List<BlockLevel> callLevels = List.of(BlockLevel.CALL, BlockLevel.ALL);
        if (blockRepository.existsByBlockerAndBlockedAndLevelIn(callee, caller, callLevels)) {
            throw new AppException("Bạn đã bị người nhận chặn cuộc gọi", HttpStatus.FORBIDDEN);
        }
        if (blockRepository.existsByBlockerAndBlockedAndLevelIn(caller, callee, callLevels)) {
            throw new AppException("Bạn đã chặn cuộc gọi từ người này", HttpStatus.FORBIDDEN);
        }

        CallHistory callHistory = CallHistory.builder()
                .caller(caller)
                .callee(callee)
                .type(request.getType())
                .status(CallStatus.ONGOING)
                .build();

        return callHistoryRepository.save(callHistory);
    }

    @Transactional
    public CallHistory endCall(Long callId, Long userId, CallStatus finalStatus, Integer durationSeconds) {
        CallHistory call = callHistoryRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));
                
        // Validation: Must be participant
        if (!call.getCaller().getId().equals(userId) && !call.getCallee().getId().equals(userId)) {
            throw new AppException("Không có quyền thao tác", HttpStatus.FORBIDDEN);
        }
        
        call.setStatus(finalStatus);
        call.setEndedAt(LocalDateTime.now());
        if (durationSeconds != null && durationSeconds > 0) {
            call.setDurationSeconds(durationSeconds);
        }
        return callHistoryRepository.save(call);
    }

    @Transactional(readOnly = true)
    public List<CallHistory> getUserCallHistory(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return callHistoryRepository.findByCallerOrCallee(user);
    }
}
