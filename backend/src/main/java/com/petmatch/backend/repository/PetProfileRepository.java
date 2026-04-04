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

@Repository
public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {

    Optional<PetProfile> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);

    /**
     * Lấy suggestions: cùng loài, chưa bị ẩn, loại trừ pet của chính mình và pet bị block.
     * Loại trừ pet đã từng swipe (đã có match request từ mình).
     * Pet được super-like (có người super like mình) xếp trước.
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
            Pageable pageable);

    /**
     * Tìm kiếm nâng cao với nhiều filter.
     * minDob / maxDob = tương ứng với maxAge / minAge (dob nhỏ = tuổi cao hơn).
     */
    @Query("""
        SELECT p FROM PetProfile p
        WHERE p.isHidden = false
          AND p.owner.isLocked = false
          AND p.owner.id != :currentUserId
          AND (:species IS NULL OR p.species = :species)
          AND (:breed IS NULL OR LOWER(p.breed) LIKE LOWER(CONCAT('%',:breed,'%')))
          AND (:gender IS NULL OR p.gender = :gender)
          AND (:lookingFor IS NULL OR p.lookingFor = :lookingFor)
          AND (:healthStatus IS NULL OR p.healthStatus = :healthStatus)
          AND (:minWeight IS NULL OR p.weightKg >= :minWeight)
          AND (:maxWeight IS NULL OR p.weightKg <= :maxWeight)
          AND (:minDob IS NULL OR p.dateOfBirth <= :minDob)
          AND (:maxDob IS NULL OR p.dateOfBirth >= :maxDob)
        """)
    Page<PetProfile> search(
            @Param("currentUserId") Long currentUserId,
            @Param("species")       String species,
            @Param("breed")         String breed,
            @Param("gender")        Gender gender,
            @Param("lookingFor")    LookingFor lookingFor,
            @Param("healthStatus")  HealthStatus healthStatus,
            @Param("minWeight")     java.math.BigDecimal minWeight,
            @Param("maxWeight")     java.math.BigDecimal maxWeight,
            @Param("minDob")        LocalDate minDob,   // = LocalDate.now().minusYears(maxAge)
            @Param("maxDob")        LocalDate maxDob,   // = LocalDate.now().minusYears(minAge)
            Pageable pageable);
}