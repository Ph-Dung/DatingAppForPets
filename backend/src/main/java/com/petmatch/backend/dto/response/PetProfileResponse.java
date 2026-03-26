package com.petmatch.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PetProfileResponse {
    private UUID id;
    private UUID ownerId;
    private String ownerName;
    private String name;
    private String species;
    private String breed;
    private String gender;
    private LocalDate dateOfBirth;
    private BigDecimal weightKg;
    private String color;
    private String size;
    private String reproductiveStatus;
    private Boolean isVaccinated;
    private LocalDate lastVaccineDate;
    private String healthStatus;
    private String healthNotes;
    private String personalityTags;
    private String lookingFor;
    private String notes;
    private Boolean isHidden;
    private String avatarUrl;            // ảnh đại diện pet
    private List<String> photoUrls;
    private LocalDateTime createdAt;
}
