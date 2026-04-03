package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetProfile;
import com.petmatch.backend.enums.Gender;
import com.petmatch.backend.enums.LookingFor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.lang.Long;
import java.util.Optional;

@Repository
public interface PetProfileRepository extends JpaRepository<PetProfile, Long> {

    Optional<PetProfile> findByOwnerId(Long ownerId);
    boolean existsByOwnerId(Long ownerId);

    // Lấy danh sách đề xuất: cùng loài, chưa bị ẩn,
    // loại trừ pet của chính mình + các pet bị block
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
        """)
    Page<PetProfile> findSuggestions(
            @Param("currentUserId") Long currentUserId,
            @Param("species") String species,
            Pageable pageable);

    // Tìm kiếm với filter
    @Query("""
        SELECT p FROM PetProfile p
        WHERE p.isHidden = false
          AND p.owner.isLocked = false
          AND (:species IS NULL OR p.species = :species)
          AND (:breed   IS NULL OR LOWER(p.breed) LIKE LOWER(CONCAT('%',:breed,'%')))
          AND (:gender  IS NULL OR p.gender = :gender)
          AND (:lookingFor IS NULL OR p.lookingFor = :lookingFor)
          AND p.owner.id != :currentUserId
        """)
    Page<PetProfile> search(
            @Param("currentUserId") Long currentUserId,
            @Param("species")    String species,
            @Param("breed")      String breed,
            @Param("gender") Gender gender,
            @Param("lookingFor") LookingFor lookingFor,
            Pageable pageable);
}