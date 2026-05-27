package com.petmatch.backend.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO: Request thêm/cập nhật bản ghi vaccine
 * - POST /api/pets/vaccinations (add)
 * - PUT /api/pets/vaccinations/{vacId} (update)
 * 
 * Logic: Auto-update PetProfile.isVaccinated=true, lastVaccineDate=max(...)
 */
@Data
public class PetVaccinationRequest {
    @NotBlank
    private String vaccineName;  // VD: "Dại", "5in1", "Lepto"

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate vaccinatedDate;  // Ngày tiêm (không phải ngày ghi nhập)

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate nextDueDate;  // null = suốt đời

    private String clinicName;
    private String notes;
}