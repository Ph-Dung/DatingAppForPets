package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.BlockLevel;
import com.petmatch.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Tra cứu chặn người dùng
 * 
 * Dùng bởi: InteractionService.blockUser(), unblockUser(), getMyBlocks()
 * Dùng bởi: PetProfileService.getSuggestions/search (lọc người bị chặn)
 */
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    
    // ════════════════════════════════════════════════════════════════
    // TRA CỨU XÁC THỰC
    // ════════════════════════════════════════════════════════════════
    
    /** Check if blocker has blocked blocked (any level) */
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    
    /** Check by IDs (used when fetching from SecurityContext) */
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    /** Check blocker-blocked with specific level(s) (e.g., ALL, MESSAGING_ONLY) */
    @Query("""
        SELECT COUNT(b) > 0 FROM Block b
        WHERE b.blocker = :blocker AND b.blocked = :blocked
          AND b.level IN :levels
    """)
    boolean existsByBlockerAndBlockedAndLevelIn(
        @Param("blocker") User blocker,
        @Param("blocked") User blocked,
        @Param("levels") List<BlockLevel> levels
    );

    // ════════════════════════════════════════════════════════════════
    // TRA CỨU TÌM KIẾM
    // ════════════════════════════════════════════════════════════════
    
    /** Get all users blocked by blocker (for "My Blocks" list) */
    List<Block> findByBlocker(User blocker);
    
    /** Find block record by blocker and blocked (for update/delete) */
    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
    
    /** Find by IDs (alternative lookup) */
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
