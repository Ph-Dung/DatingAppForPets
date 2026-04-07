package com.petmatch.backend.service;

import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.entity.Message;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.BlockRepository;
import com.petmatch.backend.repository.MessageRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final BlockRepository blockRepository;

    @Transactional
    public MessageDto saveMessage(MessageDto messageDto) {
        User sender = userRepository.findById(messageDto.getSenderId())
                .orElseThrow(() -> new AppException("Sender not found", HttpStatus.NOT_FOUND));
        User receiver = userRepository.findById(messageDto.getReceiverId())
                .orElseThrow(() -> new AppException("Receiver not found", HttpStatus.NOT_FOUND));

        // Fix #9: Kiểm tra block — không cho phép gửi tin nếu bị chặn
        if (blockRepository.existsByBlockerAndBlocked(receiver, sender)) {
            throw new AppException("Bạn đã bị người nhận chặn, không thể gửi tin nhắn", HttpStatus.FORBIDDEN);
        }
        if (blockRepository.existsByBlockerAndBlocked(sender, receiver)) {
            throw new AppException("Bạn đã chặn người này, hãy bỏ chặn trước khi nhắn tin", HttpStatus.FORBIDDEN);
        }

        if (messageDto.getContent() == null || messageDto.getContent().isBlank()) {
            throw new AppException("Nội dung tin nhắn không được trống", HttpStatus.BAD_REQUEST);
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(messageDto.getContent())
                .isRead(false)
                .build();

        Message savedMessage = messageRepository.save(message);
        return toDto(savedMessage);
    }

    /** Fix #12: phân trang — page bắt đầu từ 0, size mặc định 30 */
    @Transactional(readOnly = true)
    public List<MessageDto> getChatHistory(Long userId1, Long userId2, int page, int size) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new AppException("User 1 not found", HttpStatus.NOT_FOUND));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new AppException("User 2 not found", HttpStatus.NOT_FOUND));

        Pageable pageable = PageRequest.of(page, size);
        Page<Message> messages = messageRepository.findChatHistory(user1, user2, pageable);

        return messages.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /** Đánh dấu đã đọc toàn bộ tin nhắn từ senderId gửi cho receiverId */
    @Transactional
    public void markAsRead(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException("Sender not found", HttpStatus.NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AppException("Receiver not found", HttpStatus.NOT_FOUND));
        messageRepository.markAllAsRead(sender, receiver);
    }

    /** Đếm số tin chưa đọc từ senderId gửi cho receiverId */
    @Transactional(readOnly = true)
    public long countUnread(Long senderId, Long receiverId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new AppException("Sender not found", HttpStatus.NOT_FOUND));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new AppException("Receiver not found", HttpStatus.NOT_FOUND));
        return messageRepository.countUnread(sender, receiver);
    }

    private MessageDto toDto(Message msg) {
        return MessageDto.builder()
                .id(msg.getId())
                .senderId(msg.getSender().getId())
                .receiverId(msg.getReceiver().getId())
                .content(msg.getContent())
                .sentAt(msg.getSentAt())
                .isRead(msg.getIsRead())
                .build();
    }
}

