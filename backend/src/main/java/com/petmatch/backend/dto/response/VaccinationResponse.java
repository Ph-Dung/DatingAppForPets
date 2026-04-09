package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class VaccinationResponse {
    private Long id;
    private String vaccineName;
    private LocalDate vaccinatedDate;
    private LocalDate nextDueDate;
    private String clinicName;
    private String notes;
    private LocalDateTime createdAt;
}
