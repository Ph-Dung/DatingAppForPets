package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Dislike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tra cứu không thích cấp pet (đèo dõi bỏ qua)
 * 
 * Dùng bởi: MatchRequestService.recordDislike()
 * Dùng bởi: PetProfileService.getSuggestions() (lọc thú cưng bị không thích)
 */
@Repository
public interface DislikeRepository extends JpaRepository<Dislike, Long> {
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU ĐÈO DÕI KHÔNG THÍCH
    // ════════════════════════════════════════════════════════════════
    
    /** Check if pet A has already disliked pet B (prevent re-suggesting) */
    boolean existsByDislikerPetIdAndDislikedPetId(Long dislikerPetId, Long dislikedPetId);
}
