package com.petmatch.backend.repository;

import com.petmatch.backend.entity.SwipePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Tra cứu học AI sở thích
 * 
 * Dùng bởi: AiMatchingService.updatePreferences() (lưu/cập nhật sở thích)
 * Dùng bởi: AiMatchingService để tính điểm gợi ý thông minh
 */
@Repository
public interface SwipePreferenceRepository extends JpaRepository<SwipePreference, Long> {
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU TÌM KIẾM SỞ THÍCH
    // ════════════════════════════════════════════════════════════════
    
    // Ghi chú: petId là PK, không cần @Query tùy chỉnh
    // Truy cập qua: swipePreferenceRepository.findById(petId)
}
