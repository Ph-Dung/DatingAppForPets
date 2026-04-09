package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "dislikes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"disliker_pet_id","disliked_pet_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Dislike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disliker_pet_id", nullable = false)
    PetProfile dislikerPet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disliked_pet_id", nullable = false)
    PetProfile dislikedPet;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;
}
