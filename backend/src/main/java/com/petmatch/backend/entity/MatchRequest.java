package com.petmatch.backend.entity;

import com.petmatch.backend.enums.MatchStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_requests",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sender_pet_id","receiver_pet_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MatchRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_pet_id", nullable = false)
    PetProfile senderPet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_pet_id", nullable = false)
    PetProfile receiverPet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    MatchStatus status = MatchStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}