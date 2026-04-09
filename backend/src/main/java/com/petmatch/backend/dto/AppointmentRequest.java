package com.petmatch.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {
    @NotNull(message = "recipientId is required")
    private Long recipientId;

    @NotNull(message = "meetingTime is required")
    @Future(message = "meetingTime must be in the future")
    private LocalDateTime meetingTime;

    @NotBlank(message = "location is required")
    @Size(max = 255, message = "location must be <= 255 chars")
    private String location;

    @Size(max = 1000, message = "notes must be <= 1000 chars")
    private String notes;
}
