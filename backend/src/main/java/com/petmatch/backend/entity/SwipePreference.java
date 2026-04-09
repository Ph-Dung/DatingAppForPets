package com.petmatch.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "swipe_preference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SwipePreference {

    @Id
    Long petId;        // 1-1 với PetProfile

    // JSON string: ["Poodle","Golden Retriever"]
    @Column(name = "preferred_breeds", columnDefinition = "TEXT")
    String preferredBreeds;

    @Column(name = "preferred_gender", length = 10)
    String preferredGender;    // MALE / FEMALE / null

    @Column(name = "avg_weight")
    Double avgWeight;

    @Column(name = "avg_age")
    Double avgAge;

    @Column(name = "prefer_healthy")
    Boolean preferHealthy;

    @Column(name = "preferred_looking_for", length = 20)
    String preferredLookingFor; // BREEDING / FRIENDSHIP / PLAY / null

    @Column(name = "total_likes")
    int totalLikes;

    @UpdateTimestamp
    @Column(name = "updated_at")
    LocalDateTime updatedAt;
}
