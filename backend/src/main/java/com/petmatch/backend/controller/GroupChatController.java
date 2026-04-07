package com.petmatch.backend.controller;

import com.petmatch.backend.dto.request.GroupChatCreateRequest;
import com.petmatch.backend.dto.request.GroupMessageRequest;
import com.petmatch.backend.dto.response.GroupChatResponse;
import com.petmatch.backend.dto.response.GroupMessageResponse;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.GroupChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupChatController {

    private final GroupChatService groupChatService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<GroupChatResponse> createGroup(
            Authentication auth,
            @RequestBody GroupChatCreateRequest request) {
        Long creatorId = getUserId(auth);
        return ResponseEntity.ok(groupChatService.createGroup(creatorId, request));
    }

    @GetMapping
    public ResponseEntity<List<GroupChatResponse>> getUserGroups(Authentication auth) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(groupChatService.getUserGroups(userId));
    }

    @PostMapping("/{groupId}/messages")
    public ResponseEntity<GroupMessageResponse> sendMessage(
            Authentication auth,
            @PathVariable Long groupId,
            @RequestBody GroupMessageRequest request) {
        Long userId = getUserId(auth);
        return ResponseEntity.ok(groupChatService.sendMessage(userId, groupId, request));
    }

    @GetMapping("/{groupId}/history")
    public ResponseEntity<List<GroupMessageResponse>> getGroupHistory(
            Authentication auth,
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        Long userId = getUserId(auth);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(groupChatService.getGroupHistory(groupId, userId, pageable));
    }

    @PostMapping("/{groupId}/members/{newMemberId}")
    public ResponseEntity<Void> addMember(
            Authentication auth,
            @PathVariable Long groupId,
            @PathVariable Long newMemberId) {
        Long adminId = getUserId(auth);
        groupChatService.addMember(adminId, groupId, newMemberId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
