package com.petmatch.backend.service;

import com.petmatch.backend.dto.CallRequest;
import com.petmatch.backend.entity.CallHistory;
import com.petmatch.backend.entity.CallStatus;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.repository.CallHistoryRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CallService {

    private final CallHistoryRepository callHistoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public CallHistory initiateCall(Long callerId, CallRequest request) {
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new RuntimeException("Caller not found"));
        User callee = userRepository.findById(request.getCalleeId())
                .orElseThrow(() -> new RuntimeException("Callee not found"));

        CallHistory callHistory = CallHistory.builder()
                .caller(caller)
                .callee(callee)
                .type(request.getType())
                .status(CallStatus.ONGOING)
                .build();

        return callHistoryRepository.save(callHistory);
    }

    @Transactional
    public CallHistory endCall(Long callId, CallStatus finalStatus) {
        CallHistory call = callHistoryRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Call not found"));
        
        call.setStatus(finalStatus);
        if (finalStatus == CallStatus.COMPLETED) {
            call.setEndedAt(LocalDateTime.now());
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
