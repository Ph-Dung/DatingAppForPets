package com.petmatch.backend.service;

import com.petmatch.backend.config.ResourceNotFoundException;
import com.petmatch.backend.dto.MessageDto;
import com.petmatch.backend.entity.Message;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.repository.MessageRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional
    public MessageDto saveMessage(MessageDto messageDto) {
        User sender = userRepository.findById(messageDto.getSenderId())
                .orElseThrow(() -> new ResourceNotFoundException("User", messageDto.getSenderId()));
        User receiver = userRepository.findById(messageDto.getReceiverId())
                .orElseThrow(() -> new ResourceNotFoundException("User", messageDto.getReceiverId()));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(messageDto.getContent())
                .build();

        Message savedMessage = messageRepository.save(message);
        
        return MessageDto.builder()
                .id(savedMessage.getId())
                .senderId(savedMessage.getSender().getId())
                .receiverId(savedMessage.getReceiver().getId())
                .content(savedMessage.getContent())
                .sentAt(savedMessage.getSentAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getChatHistory(Long userId1, Long userId2) {
        User user1 = userRepository.findById(userId1)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId1));
        User user2 = userRepository.findById(userId2)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId2));

        return messageRepository.findChatHistory(user1, user2).stream()
                .map(msg -> MessageDto.builder()
                        .id(msg.getId())
                        .senderId(msg.getSender().getId())
                        .receiverId(msg.getReceiver().getId())
                        .content(msg.getContent())
                        .sentAt(msg.getSentAt())
                        .build())
                .collect(Collectors.toList());
    }
}
