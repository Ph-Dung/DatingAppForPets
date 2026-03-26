package com.petmatch.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PetVaccinationRequest {
    @NotBlank
    private String vaccineName;
    @NotNull
    private LocalDate vaccinatedDate;
    private LocalDate nextDueDate;
    private String clinicName;
    private String notes;
}