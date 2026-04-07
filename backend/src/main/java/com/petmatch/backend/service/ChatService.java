package com.petmatch.backend.service;

import com.petmatch.backend.dto.ConversationSummaryDto;
import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.entity.*;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;
    private final MatchRepository matchRepository;
    private final CloudinaryService cloudinaryService;

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User không tồn tại", HttpStatus.UNAUTHORIZED));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found: " + id, HttpStatus.NOT_FOUND));
    }

    // ── Conversation List (Match-gated) ───────────────────────────────────────

    /**
     * Trả danh sách cuộc trò chuyện của currentUser.
     * Chỉ hiển thị những người đã MATCH và conversation chưa bị xóa.
     */
    @Transactional(readOnly = true)
    public List<ConversationSummaryDto> getConversations() {
        User me = currentUser();
        List<Match> matches = matchRepository.findAllByUser(me);

        return matches.stream().map(match -> {
            User other = match.getUser1().getId().equals(me.getId())
                    ? match.getUser2() : match.getUser1();

            Optional<Message> lastMsgOpt = messageRepository.findLastMessage(me, other);
            String lastMsg = lastMsgOpt.map(m -> {
                if (m.getType() == MessageType.IMAGE) return "📷 Hình ảnh";
                if (m.getType() == MessageType.VOICE) return "🎙️ Tin nhắn thoại";
                return m.getContent();
            }).orElse(null);

            // Nếu conversation bị xóa (deletedBySenderAt <= lastMsg.sentAt) và không có tin nhắn mới → ẩn
            boolean deletedConv = lastMsgOpt.map(m -> {
                // Nếu me là sender
                if (m.getSender().getId().equals(me.getId())) {
                    return m.getDeletedBySenderAt() != null && !m.getSentAt().isAfter(m.getDeletedBySenderAt());
                } else {
                    return m.getDeletedByReceiverAt() != null && !m.getSentAt().isAfter(m.getDeletedByReceiverAt());
                }
            }).orElse(false);

            if (deletedConv && lastMsg == null) return null;

            String lastTime = lastMsgOpt.map(m -> m.getSentAt().toString()).orElse(match.getMatchedAt().toString());
            long unread = lastMsgOpt.isPresent() ? messageRepository.countUnread(other, me) : 0;

            // Avatar: lấy từ User.avatarUrl
            return ConversationSummaryDto.builder()
                    .matchedUserId(other.getId())
                    .userName(other.getFullName())
                    .avatarUrl(other.getAvatarUrl())
                    .lastMessage(lastMsg)
                    .lastMessageTime(lastTime)
                    .unreadCount(unread)
                    .isOnline(false)
                    .isMuted(false)
                    .build();
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    // ── Send Message ──────────────────────────────────────────────────────────

    @Transactional
    public MessageDto saveMessage(MessageDto messageDto) {
        User sender = currentUser();
        User receiver = getUser(messageDto.getReceiverId());

        // Match check: chỉ người đã match mới nhắn tin được
        if (matchRepository.findMatchByUsers(sender, receiver).isEmpty()) {
            throw new AppException("Chỉ có thể nhắn tin với người đã match", HttpStatus.FORBIDDEN);
        }

        // Block check: cấp MESSAGE hoặc ALL
        List<BlockLevel> msgLevels = List.of(BlockLevel.MESSAGE, BlockLevel.ALL);
        if (blockRepository.existsByBlockerAndBlockedAndLevelIn(receiver, sender, msgLevels)) {
            throw new AppException("Bạn đã bị người nhận chặn, không thể gửi tin nhắn", HttpStatus.FORBIDDEN);
        }
        if (blockRepository.existsByBlockerAndBlockedAndLevelIn(sender, receiver, msgLevels)) {
            throw new AppException("Bạn đã chặn người này, hãy mở chặn để nhắn tin", HttpStatus.FORBIDDEN);
        }

        // Nội dung
        if ((messageDto.getContent() == null || messageDto.getContent().isBlank())
                && messageDto.getMediaUrl() == null) {
            throw new AppException("Tin nhắn không được trống", HttpStatus.BAD_REQUEST);
        }

        MessageType type = messageDto.getType() != null ? messageDto.getType() : MessageType.TEXT;

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .type(type)
                .content(messageDto.getContent())
                .mediaUrl(messageDto.getMediaUrl())
                .isRead(false)
                .build();

        return toDto(messageRepository.save(message));
    }

    // ── Chat History ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MessageDto> getChatHistory(Long userId1, Long userId2, int page, int size) {
        User user1 = getUser(userId1);
        User user2 = getUser(userId2);

        // Match check để lấy lịch sử
        if (matchRepository.findMatchByUsers(user1, user2).isEmpty()) {
            throw new AppException("Không tìm thấy match giữa 2 người dùng", HttpStatus.FORBIDDEN);
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findChatHistory(user1, user2, pageable);
        return messages.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ── Delete Conversation ───────────────────────────────────────────────────

    /**
     * Xóa conversation (soft-delete) từ phía người dùng hiện tại.
     * Người kia vẫn thấy bình thường.
     * Nếu nhắn tin lại, conversation mới sẽ xuất hiện nhưng không thấy tin cũ.
     */
    @Transactional
    public void deleteConversation(Long otherId) {
        User me = currentUser();
        User other = getUser(otherId);
        LocalDateTime now = LocalDateTime.now();
        messageRepository.softDeleteBySender(me, other, now);
        messageRepository.softDeleteByReceiver(me, other, now);
    }

    // ── Mark as Read ──────────────────────────────────────────────────────────

    @Transactional
    public void markAsRead(Long senderId, Long receiverId) {
        User sender = getUser(senderId);
        User receiver = getUser(receiverId);
        messageRepository.markAllAsRead(sender, receiver);
    }

    // ── Unread Count ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public long countUnread(Long senderId, Long receiverId) {
        return messageRepository.countUnread(getUser(senderId), getUser(receiverId));
    }

    // ── Upload Media ──────────────────────────────────────────────────────────

    /**
     * Upload file ảnh hoặc voice lên Cloudinary và trả về URL + type.
     */
    @Transactional
    public Map<String, String> uploadMedia(MultipartFile file, String typeStr) {
        MessageType type;
        try {
            type = MessageType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("type phải là IMAGE hoặc VOICE", HttpStatus.BAD_REQUEST);
        }
        if (type == MessageType.TEXT) {
            throw new AppException("Không dùng upload cho type TEXT", HttpStatus.BAD_REQUEST);
        }

        String folder = type == MessageType.IMAGE ? "chat/images" : "chat/voices";
        String url = cloudinaryService.uploadFile(file, folder);
        return Map.of("mediaUrl", url, "type", type.name());
    }

    // ── Check Block Status (cho client biết có bị block không) ────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getBlockStatus(Long otherUserId) {
        User me = currentUser();
        User other = getUser(otherUserId);

        boolean iBlockedThem = blockRepository.existsByBlockerAndBlocked(me, other);
        boolean theyBlockedMe = blockRepository.existsByBlockerAndBlocked(other, me);

        BlockLevel myLevel = blockRepository.findByBlockerAndBlocked(me, other)
                .map(Block::getLevel).orElse(null);

        return Map.of(
                "iBlockedThem", iBlockedThem,
                "theyBlockedMe", theyBlockedMe,
                "myBlockLevel", myLevel != null ? myLevel.name() : ""
        );
    }

    // ── DTO Mapper ────────────────────────────────────────────────────────────

    private MessageDto toDto(Message msg) {
        return MessageDto.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .receiverId(msg.getReceiver().getId())
                .type(msg.getType())
                .content(msg.getContent())
                .mediaUrl(msg.getMediaUrl())
                .sentAt(msg.getSentAt())
                .isRead(msg.getIsRead())
                .build();
    }
}
