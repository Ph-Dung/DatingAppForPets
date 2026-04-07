package com.petmatch.backend.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.petmatch.backend.enums.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

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
    @Builder.Default
    Role role = Role.USER;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    Boolean isLocked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    // Relationship: 1 user = 1 pet profile
    @OneToOne(mappedBy = "owner", cascade = CascadeType.ALL)
    PetProfile petProfile;
}
