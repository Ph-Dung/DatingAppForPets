package com.petmatch.backend.service;

import com.petmatch.backend.dto.request.GroupChatCreateRequest;
import com.petmatch.backend.dto.request.GroupMessageRequest;
import com.petmatch.backend.dto.response.GroupChatResponse;
import com.petmatch.backend.dto.response.GroupMessageResponse;
import com.petmatch.backend.entity.*;
import com.petmatch.backend.enums.GroupMemberRole;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupChatService {

    private final ChatGroupRepository chatGroupRepository;
    private final ChatGroupMemberRepository chatGroupMemberRepository;
    private final GroupMessageRepository groupMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public GroupChatResponse createGroup(Long creatorId, GroupChatCreateRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new AppException("Creator not found", HttpStatus.NOT_FOUND));

        ChatGroup group = ChatGroup.builder()
                .name(request.getName())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(creator)
                .build();

        ChatGroup savedGroup = chatGroupRepository.save(group);

        // Add creator as ADMIN
        ChatGroupMember creatorMember = ChatGroupMember.builder()
                .group(savedGroup)
                .user(creator)
                .role(GroupMemberRole.ADMIN)
                .build();
        chatGroupMemberRepository.save(creatorMember);

        // Add other members as MEMBER
        for (Long memberId : request.getMemberIds()) {
            if (memberId.equals(creatorId)) continue;
            User user = userRepository.findById(memberId)
                    .orElseThrow(() -> new AppException("Member not found: " + memberId, HttpStatus.NOT_FOUND));

            ChatGroupMember member = ChatGroupMember.builder()
                    .group(savedGroup)
                    .user(user)
                    .role(GroupMemberRole.MEMBER)
                    .build();
            chatGroupMemberRepository.save(member);
        }

        // Notify all members via WebSocket (topic /topic/group.{groupId})
        // Note: Client should subscribe to /topic/group.{groupId} for group events
        return toGroupResponse(savedGroup);
    }

    @Transactional
    public GroupMessageResponse sendMessage(Long senderId, Long groupId, GroupMessageRequest request) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException("Group not found", HttpStatus.NOT_FOUND));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException("Sender not found", HttpStatus.NOT_FOUND));

        // Check if sender is a member
        if (!chatGroupMemberRepository.existsByGroupAndUser(group, sender)) {
            throw new AppException("You are not a member of this group", HttpStatus.FORBIDDEN);
        }

        GroupMessage message = GroupMessage.builder()
                .group(group)
                .sender(sender)
                .content(request.getContent())
                .build();

        GroupMessage savedMessage = groupMessageRepository.save(message);
        GroupMessageResponse response = toMessageResponse(savedMessage);

        // Broadcast to all group subscribers
        messagingTemplate.convertAndSend("/topic/group/" + groupId, response);

        return response;
    }

    @Transactional(readOnly = true)
    public List<GroupChatResponse> getUserGroups(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return chatGroupRepository.findGroupsByUser(user).stream()
                .map(this::toGroupResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupMessageResponse> getGroupHistory(Long groupId, Long userId, Pageable pageable) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException("Group not found", HttpStatus.NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (!chatGroupMemberRepository.existsByGroupAndUser(group, user)) {
            throw new AppException("Access denied", HttpStatus.FORBIDDEN);
        }

        Page<GroupMessage> messages = groupMessageRepository.findByGroupOrderBySentAtDesc(group, pageable);
        return messages.stream()
                .map(this::toMessageResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addMember(Long adminId, Long groupId, Long newMemberId) {
        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new AppException("Group not found", HttpStatus.NOT_FOUND));

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AppException("Admin not found", HttpStatus.NOT_FOUND));

        ChatGroupMember adminMember = chatGroupMemberRepository.findByGroupAndUser(group, admin)
                .orElseThrow(() -> new AppException("Not a member", HttpStatus.FORBIDDEN));

        if (adminMember.getRole() != GroupMemberRole.ADMIN) {
            throw new AppException("Only admins can add members", HttpStatus.FORBIDDEN);
        }

        User newMemberUser = userRepository.findById(newMemberId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (chatGroupMemberRepository.existsByGroupAndUser(group, newMemberUser)) {
            throw new AppException("User already in group", HttpStatus.CONFLICT);
        }

        ChatGroupMember newMember = ChatGroupMember.builder()
                .group(group)
                .user(newMemberUser)
                .role(GroupMemberRole.MEMBER)
                .build();
        chatGroupMemberRepository.save(newMember);
    }

    private GroupChatResponse toGroupResponse(ChatGroup group) {
        List<ChatGroupMember> members = chatGroupMemberRepository.findByGroup(group);
        String lastMessage = groupMessageRepository.findTopByGroupOrderBySentAtDesc(group)
                .map(GroupMessage::getContent)
                .orElse(null);

        return GroupChatResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .avatarUrl(group.getAvatarUrl())
                .createdById(group.getCreatedBy().getId())
                .createdAt(group.getCreatedAt())
                .members(members.stream().map(m -> GroupChatResponse.GroupMemberResponse.builder()
                        .userId(m.getUser().getId())
                        .fullName(m.getUser().getFullName())
                        .avatarUrl(m.getUser().getAvatarUrl())
                        .role(m.getRole().name())
                        .joinedAt(m.getJoinedAt())
                        .build()).collect(Collectors.toList()))
                .lastMessage(lastMessage)
                .build();
    }

    private GroupMessageResponse toMessageResponse(GroupMessage msg) {
        return GroupMessageResponse.builder()
                .id(msg.getId())
                .groupId(msg.getGroup().getId())
                .senderId(msg.getSender().getId())
                .senderName(msg.getSender().getFullName())
                .senderAvatarUrl(msg.getSender().getAvatarUrl())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .build();
    }
}
