package com.petmatch.backend.entity;

import com.petmatch.backend.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "full_name", nullable = false, length = 100)
    String fullName;

    @Column(nullable = false, unique = true, length = 255)
    String email;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(length = 20)
    String phone;

    @Column(name = "avatar_url")
    String avatarUrl;

    String address;

    Double latitude;
    Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    Role role = Role.USER;

    @Column(name = "is_locked", nullable = false)
    Boolean isLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    // Relationship: 1 user = 1 pet profile
    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL)
    PetProfile petProfile;
}
