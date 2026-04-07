package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class PetProfileResponse {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private LocalDate dateOfBirth;
    private Integer age;             // tính từ dateOfBirth (năm)
    private BigDecimal weightKg;
    private String color;
    private String size;
    private String reproductiveStatus;
    private Boolean isVaccinated;
    private LocalDate lastVaccineDate;
    private Integer vaccinationCount; // tổng số lần tiêm đã lưu
    private String healthStatus;
    private String healthNotes;
    private String personalityTags;
    private String lookingFor;
    private String notes;
    private Boolean isHidden;
    private String avatarUrl;
    private List<String> photoUrls;
    private LocalDateTime createdAt;
    // Địa lý
    private Double distanceKm;    // null nếu chưa có GPS
    private String ownerAddress;  // địa chỉ văn bản của chủ
}
