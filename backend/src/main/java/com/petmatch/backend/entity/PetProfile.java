package com.petmatch.backend.entity;

import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import com.petmatch.backend.enums.ReproductiveStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    // UNIQUE → đảm bảo 1 chủ 1 pet ở tầng DB
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false, unique = true)
    User owner;

    @Column(nullable = false, length = 100)
    String name;

    @Column(nullable = false, length = 50)
    String species;           // dog, cat, rabbit...

    @Column(length = 100)
    String breed;             // Poodle, Mèo Anh, Lai...

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    Gender gender;

    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;

    @Column(name = "weight_kg", precision = 5, scale = 2)
    BigDecimal weightKg;

    @Column(length = 100)
    String color;

    @Column(length = 20)
    String size;              // small, medium, large

    @Enumerated(EnumType.STRING)
    @Column(name = "reproductive_status", nullable = false, length = 30)
    ReproductiveStatus reproductiveStatus;

    @Column(name = "is_vaccinated", nullable = false)
    Boolean isVaccinated = false;

    @Column(name = "last_vaccine_date")
    LocalDate lastVaccineDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", nullable = false, length = 30)
    HealthStatus healthStatus = HealthStatus.HEALTHY;

    @Column(name = "health_notes", columnDefinition = "TEXT")
    String healthNotes;

    // Lưu JSON string: ["friendly","playful"]
    @Column(name = "personality_tags", columnDefinition = "TEXT")
    String personalityTags;

    @Enumerated(EnumType.STRING)
    @Column(name = "looking_for", nullable = false, length = 30)
    LookingFor lookingFor;

    @Column(columnDefinition = "TEXT")
    String notes;

    @Column(name = "is_hidden", nullable = false)
    Boolean isHidden = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PetPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PetVaccination> vaccinations = new ArrayList<>();

    @OneToMany(mappedBy = "senderPet", cascade = CascadeType.ALL)
    List<MatchRequest> sentRequests = new ArrayList<>();

    @OneToMany(mappedBy = "receiverPet", cascade = CascadeType.ALL)
    List<MatchRequest> receivedRequests = new ArrayList<>();
}