package com.petmatch.backend.controller;

import com.petmatch.backend.dto.AppointmentRequest;
import com.petmatch.backend.entity.Appointment;
import com.petmatch.backend.entity.AppointmentStatus;
import com.petmatch.backend.exception.AppException;
import com.petmatch.backend.repository.UserRepository;
import com.petmatch.backend.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;

    /**
     * Fix #4: requesterId lấy từ JWT (Authentication), không phải @RequestParam.
     * Fix #14: AppointmentService sẽ gửi notification real-time qua WebSocket.
     */
    @PostMapping
    public ResponseEntity<Appointment> createAppointment(
            Authentication auth,
            @Valid @RequestBody AppointmentRequest request) {

        Long requesterId = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND))
                .getId();

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(appointmentService.createAppointment(requesterId, request));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Appointment>> getUserAppointments(@PathVariable Long userId) {
        return ResponseEntity.ok(appointmentService.getAppointmentsForUser(userId));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Appointment> updateStatus(
            Authentication auth,
            @PathVariable Long id,
            @RequestParam AppointmentStatus status) {
        Long currentUserId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, status, currentUserId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Appointment> updateDetails(
            Authentication auth,
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRequest request) {
        Long currentUserId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND))
                .getId();
        return ResponseEntity.ok(appointmentService.updateAppointmentDetails(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(
            Authentication auth,
            @PathVariable Long id) {
        Long currentUserId = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
        appointmentService.cancelAppointment(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}

