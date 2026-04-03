package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PetPhoto {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    PetProfile pet;

    @Column(name = "photo_url", nullable = false, columnDefinition = "TEXT")
    String photoUrl;

    @Column(name = "is_avatar", nullable = false)
    Boolean isAvatar = false;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    LocalDateTime uploadedAt;
}
