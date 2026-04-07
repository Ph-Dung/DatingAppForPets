package com.petmatch.backend.entity;

public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    COMPLETED,  // Cuộc hẹn đã diễn ra — có thể để lại Review
    CANCELLED
}
