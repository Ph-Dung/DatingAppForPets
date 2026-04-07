package com.petmatch.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PetVaccinationRequest {
    @NotBlank
    private String vaccineName;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate vaccinatedDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;

    private String clinicName;
    private String notes;
}