package com.petmatch.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.enums.ReproductiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PetProfileRequest {
    @NotBlank
    private String name;
    @NotBlank private String species;
    private String breed;

    @NotNull
    private Gender gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    private BigDecimal weightKg;
    private String color;
    private String size;

    @NotNull private ReproductiveStatus reproductiveStatus;
    private Boolean isVaccinated = false;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastVaccineDate;

    @NotNull private HealthStatus healthStatus;
    private String healthNotes;
    private String personalityTags;   // JSON string

    @NotNull private LookingFor lookingFor;
    private String notes;
}
