package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Dislike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DislikeRepository extends JpaRepository<Dislike, Long> {
    
    boolean existsByDislikerPetIdAndDislikedPetId(Long dislikerPetId, Long dislikedPetId);
}
