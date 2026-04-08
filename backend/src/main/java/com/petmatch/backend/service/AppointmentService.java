package com.petmatch.backend.service;

import com.petmatch.backend.dto.AppointmentRequest;
import com.petmatch.backend.entity.Appointment;
import com.petmatch.backend.entity.AppointmentStatus;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.AppointmentRepository;
import com.petmatch.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.petmatch.backend.repository.MatchRepository matchRepository;
    private final com.petmatch.backend.repository.MessageRepository messageRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @Transactional
    public Appointment createAppointment(Long requesterId, AppointmentRequest request) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new AppException("Requester not found", HttpStatus.NOT_FOUND));
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new AppException("Recipient not found", HttpStatus.NOT_FOUND));

        // Match check: chỉ người đã match mới tạo lịch hẹn được
        // Ghi chú: Tạm thời vô hiệu hoá check này để cho phép test / trò chuyện tự do và đặt lịch
        // if (matchRepository.findMatchByUserIds(requesterId, request.getRecipientId()).isEmpty()) {
        //     throw new AppException("Chỉ có thể đặt lịch hẹn với người đã match", HttpStatus.FORBIDDEN);
        // }

        Appointment appointment = Appointment.builder()
                .requester(requester)
                .recipient(recipient)
                .meetingTime(request.getMeetingTime())
                .location(request.getLocation())
                .notes(request.getNotes())
                .status(AppointmentStatus.PENDING)
                .build();

        Appointment saved = appointmentRepository.save(appointment);

        // Gửi thông báo noti lịch hẹn
        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getRecipientId()),
                "/queue/appointments",
                java.util.Map.of(
                        "event", "NEW_APPOINTMENT",
                        "appointmentId", saved.getId(),
                        "from", requester.getFullName()
                )
        );

        // Sinh thẻ tin nhắn trong Chat
        String contentJson = String.format("{\"id\":%d,\"location\":\"%s\",\"time\":\"%s\",\"status\":\"PENDING\"}",
                saved.getId(),
                request.getLocation().replace("\"", "\\\""),
                request.getMeetingTime()
        );
        com.petmatch.backend.entity.Message msg = com.petmatch.backend.entity.Message.builder()
                .sender(requester)
                .receiver(recipient)
                .type(com.petmatch.backend.entity.MessageType.APPOINTMENT)
                .content(contentJson)
                .build();
        com.petmatch.backend.entity.Message savedMsg = messageRepository.save(msg);

        com.petmatch.backend.dto.MessageDto dto = com.petmatch.backend.dto.MessageDto.builder()
                .id(savedMsg.getId())
                .senderId(requester.getId())
                .receiverId(recipient.getId())
                .type(com.petmatch.backend.entity.MessageType.APPOINTMENT)
                .content(contentJson)
                .sentAt(savedMsg.getSentAt())
                .isRead(false)
                .build();
        messagingTemplate.convertAndSendToUser(
                String.valueOf(recipient.getId()),
                "/queue/messages",
                dto
        );

        return saved;
    }

    /**
     * Fix #5: Kiểm tra status trước khi cập nhật.
     * Fix #6: Chỉ chủ appointment (requester/recipient) mới được cập nhật status.
     */
    @Transactional
    public Appointment updateAppointmentStatus(Long appointmentId, AppointmentStatus newStatus, Long currentUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Appointment not found", HttpStatus.NOT_FOUND));

        // Fix #6: Kiểm tra quyền
        boolean isParticipant = appointment.getRequester().getId().equals(currentUserId)
                || appointment.getRecipient().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new AppException("Bạn không có quyền cập nhật lịch hẹn này", HttpStatus.FORBIDDEN);
        }

        // Fix #5: Không cho cập nhật status nếu đã CANCELLED hoặc COMPLETED
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppException("Không thể thay đổi lịch hẹn đã bị hủy", HttpStatus.BAD_REQUEST);
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppException("Không thể thay đổi lịch hẹn đã hoàn thành", HttpStatus.BAD_REQUEST);
        }

        appointment.setStatus(newStatus);
        Appointment saved = appointmentRepository.save(appointment);

        // Fix #14: Thông báo cập nhật status cho cả 2 bên (noti)
        Long notifyUserId = appointment.getRequester().getId().equals(currentUserId)
                ? appointment.getRecipient().getId()
                : appointment.getRequester().getId();
        messagingTemplate.convertAndSendToUser(
                String.valueOf(notifyUserId),
                "/queue/appointments",
                java.util.Map.of(
                        "event", "APPOINTMENT_STATUS_CHANGED",
                        "appointmentId", saved.getId(),
                        "newStatus", newStatus.name()
                )
        );

        // Xóa tính năng tự động phát tin nhắn TEXT thông báo.
        // Thay vào đó cập nhật content của thẻ Message APPOINTMENT gốc
        try {
            String q = "UPDATE messages SET content = REPLACE(content, '\"status\":\"PENDING\"', '\"status\":\"" + newStatus.name() + "\"') " +
                       "WHERE type = 'APPOINTMENT' AND content LIKE '%\"id\":" + appointmentId + ",%'";
            entityManager.createNativeQuery(q).executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return saved;
    }

    /**
     * Fix #5: Chỉ được sửa chi tiết khi status là PENDING.
     * Fix #6: Chỉ requester mới được sửa chi tiết.
     */
    @Transactional
    public Appointment updateAppointmentDetails(Long appointmentId, AppointmentRequest request, Long currentUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Appointment not found", HttpStatus.NOT_FOUND));

        // Fix #6: Chỉ người đặt lịch mới được sửa nội dung
        if (!appointment.getRequester().getId().equals(currentUserId)) {
            throw new AppException("Chỉ người đặt lịch mới có thể chỉnh sửa nội dung hẹn", HttpStatus.FORBIDDEN);
        }

        // Fix #5: Chỉ được sửa khi đang ở PENDING
        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new AppException("Chỉ có thể sửa lịch hẹn khi đang ở trạng thái PENDING", HttpStatus.BAD_REQUEST);
        }

        appointment.setMeetingTime(request.getMeetingTime());
        appointment.setLocation(request.getLocation());
        appointment.setNotes(request.getNotes());
        return appointmentRepository.save(appointment);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getAppointmentsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));
        return appointmentRepository.findByRequesterOrRecipientOrderByMeetingTimeDesc(user, user);
    }

    /**
     * Fix #6: Kiểm tra quyền sở hữu trước khi cancel.
     */
    @Transactional
    public void cancelAppointment(Long appointmentId, Long currentUserId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException("Appointment not found", HttpStatus.NOT_FOUND));

        boolean isParticipant = appointment.getRequester().getId().equals(currentUserId)
                || appointment.getRecipient().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new AppException("Bạn không có quyền hủy lịch hẹn này", HttpStatus.FORBIDDEN);
        }
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new AppException("Lịch hẹn đã bị hủy rồi", HttpStatus.BAD_REQUEST);
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }
}

