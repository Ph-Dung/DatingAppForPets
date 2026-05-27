package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.HealthStatus;
import com.petmatch.backend.enums.LookingFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Repository: Query PetProfile
 * 
 * Queries chính:
 * - findByOwnerId: 1 user = 1 pet (UNIQUE owner_id)
 * - findSuggestions: Cùng loài, chưa swipe, super like first
 * - search: Multi-filter (species, breed, gender, age range, weight, health)
 * - searchPetsForAdmin: Tìm kiếm admin với text query
 */
@Repository
public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {

    // ════════════════════════════════════════════════════════════════
    // LOOKUP QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Lấy pet của user (1-1 relationship, UNIQUE owner_id) */
    Optional<PetProfile> findByOwnerId(Long ownerId);

    /** Kiểm tra user đã có pet chưa */
    boolean existsByOwnerId(Long ownerId);

    // ════════════════════════════════════════════════════════════════
    // SUGGESTION QUERIES
    // ════════════════════════════════════════════════════════════════

    /**
     * Lấy suggestions: cùng loài, không ẩn, loại trừ:
     * - Chính mình
     * - Pet bị block / pet chặn mình
     * - Pet đã gửi swipe trước
     * 
     * Sắp xếp: super like (người super like mình) first
     */
    @Query("""
        SELECT p FROM PetProfile p
        WHERE p.isHidden = false
          AND p.owner.id != :currentUserId
          AND p.owner.isLocked = false
          AND p.species = :species
          AND p.owner.id NOT IN (
              SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :currentUserId
          )
          AND p.owner.id NOT IN (
              SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :currentUserId
          )
          AND p.id NOT IN (
              SELECT mr.receiverPet.id FROM MatchRequest mr WHERE mr.senderPet.id = :myPetId
          )
        ORDER BY (
          SELECT CASE WHEN COUNT(sl) > 0 THEN 1 ELSE 0 END
          FROM MatchRequest sl
          WHERE sl.receiverPet.id = :myPetId
            AND sl.senderPet.id = p.id
            AND sl.isSuperLike = true
        ) DESC, p.createdAt DESC
        """)
    Page<PetProfile> findSuggestions(
        @Param("currentUserId") Long currentUserId,
        @Param("myPetId") Long myPetId,
        @Param("species") String species,
        @Param("pendingStatus") com.petmatch.backend.enums.MatchStatus pendingStatus,
        Pageable pageable);

    // ════════════════════════════════════════════════════════════════
    // SEARCH QUERIES
    // ════════════════════════════════════════════════════════════════

    /** Tìm kiếm admin: text query trên name/species/breed/owner */
    @Query("""
        SELECT p FROM PetProfile p
        WHERE (:query IS NULL OR :query = ''
               OR LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.species) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(p.owner.fullName) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:hidden IS NULL OR p.isHidden = :hidden)
        ORDER BY p.createdAt DESC
        """)
    Page<PetProfile> searchPetsForAdmin(
        @Param("query") String query,
        @Param("hidden") Boolean hidden,
        Pageable pageable);

    /** 
     * Tìm kiếm nâng cao: species, breed, gender, age range, weight, health, lookingFor
     * 
     * Params như hasSpecies=false → không filter theo field đó (optional filter)
     * minDob/maxDob = range (dob nhỏ = tuổi cao)
     */
    @Query("""
        SELECT p FROM PetProfile p
        WHERE p.isHidden = false
          AND p.owner.isLocked = false
          AND p.owner.id != :currentUserId
          AND p.owner.id NOT IN (
              SELECT b.blocked.id FROM Block b WHERE b.blocker.id = :currentUserId
          )
          AND p.owner.id NOT IN (
              SELECT b.blocker.id FROM Block b WHERE b.blocked.id = :currentUserId
          )
          AND (:hasSpecies = false OR LOWER(p.species) = LOWER(:species))
          AND (:hasBreed = false OR LOWER(p.breed) LIKE CONCAT('%', LOWER(:breed), '%'))
          AND (:hasGender = false OR p.gender = :gender)
          AND (:hasLookingFor = false OR p.lookingFor = :lookingFor)
          AND (:hasHealthStatus = false OR p.healthStatus = :healthStatus)
          AND (:hasMinWeight = false OR p.weightKg >= :minWeight)
          AND (:hasMaxWeight = false OR p.weightKg <= :maxWeight)
          AND (:hasMinDob = false OR p.dateOfBirth >= :minDob)
          AND (:hasMaxDob = false OR p.dateOfBirth <= :maxDob)
        """)
    Page<PetProfile> search(
        @Param("currentUserId") Long currentUserId,
        @Param("hasSpecies") boolean hasSpecies, @Param("species") String species,
        @Param("hasBreed") boolean hasBreed, @Param("breed") String breed,
        @Param("hasGender") boolean hasGender, @Param("gender") Gender gender,
        @Param("hasLookingFor") boolean hasLookingFor, @Param("lookingFor") LookingFor lookingFor,
        @Param("hasHealthStatus") boolean hasHealthStatus, @Param("healthStatus") HealthStatus healthStatus,
        @Param("hasMinWeight") boolean hasMinWeight, @Param("minWeight") java.math.BigDecimal minWeight,
        @Param("hasMaxWeight") boolean hasMaxWeight, @Param("maxWeight") java.math.BigDecimal maxWeight,
        @Param("hasMinDob") boolean hasMinDob, @Param("minDob") LocalDate minDob,
        @Param("hasMaxDob") boolean hasMaxDob, @Param("maxDob") LocalDate maxDob,
        Pageable pageable);

    // ════════════════════════════════════════════════════════════════
    // ADMIN QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Đếm số pet ẩn (hidden) */
    long countByIsHiddenTrue();
}