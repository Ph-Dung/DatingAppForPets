package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PetPhotoRepository extends JpaRepository<PetPhoto, UUID> {
    List<PetPhoto> findByPetId(UUID petId);
    Optional<PetPhoto> findByPetIdAndIsAvatarTrue(UUID petId);
    int countByPetId(UUID petId);
}