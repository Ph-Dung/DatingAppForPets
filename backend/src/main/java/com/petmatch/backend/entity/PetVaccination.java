package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetVaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    PetProfile pet;

    @Column(name = "vaccine_name", nullable = false, length = 100)
    String vaccineName;       // Dại, 5in1, Lepto...

    @Column(name = "vaccinated_date", nullable = false)
    LocalDate vaccinatedDate;

    @Column(name = "next_due_date")
    LocalDate nextDueDate;

    @Column(name = "clinic_name", length = 150)
    String clinicName;

    @Column(columnDefinition = "TEXT")
    String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}