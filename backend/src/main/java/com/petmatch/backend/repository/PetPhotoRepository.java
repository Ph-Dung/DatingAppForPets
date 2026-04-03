package com.petmatch.backend.repository;

import com.petmatch.backend.entity.PetPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.Long;
import java.util.List;
import java.util.Optional;

@Repository
public interface PetPhotoRepository extends JpaRepository<PetPhoto, Long> {
    List<PetPhoto> findByPetId(Long petId);
    Optional<PetPhoto> findByPetIdAndIsAvatarTrue(Long petId);
    int countByPetId(Long petId);
}