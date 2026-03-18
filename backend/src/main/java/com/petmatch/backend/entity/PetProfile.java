package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;
    String species; // dog, cat
    String breed;
    String gender;

    int age;
    double weight;

    String vaccinationStatus;
    int vaccinationCount;

    String healthStatus;
    String medicalHistory;

    String location; // để match gần

    boolean isVisible = true;

    @OneToOne
    @JoinColumn(name = "user_id")
    User owner;
}
