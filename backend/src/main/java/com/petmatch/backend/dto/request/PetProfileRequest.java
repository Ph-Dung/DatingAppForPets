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

/**
 * DTO: Request tạo/cập nhật hồ sơ pet
 * - POST /api/pets (create)
 * - PUT /api/pets (update)
 * 
 * Validation: @NotBlank/@NotNull tại các field bắt buộc
 * Enum mapping tự động (MALE → Gender.MALE)
 */
@Data
public class PetProfileRequest {
    // ========== Bắt buộc ==========
    @NotBlank
    private String name;
    @NotBlank private String species;
    @NotNull private Gender gender;
    @NotNull private ReproductiveStatus reproductiveStatus;
    @NotNull private HealthStatus healthStatus;
    @NotNull private LookingFor lookingFor;

    // ========== Tuỳ chọn ==========
    private String breed;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;  // Age tính từ field này

    private BigDecimal weightKg;
    private String color;
    private String size;

    private Boolean isVaccinated = false;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastVaccineDate;

    private String healthNotes;
    private String personalityTags;   // JSON array: ["friendly","playful"]
    private String notes;
}
