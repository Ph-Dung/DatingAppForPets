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

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    List<Block> findByBlocker(User blocker);
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    /** Kiểm tra blocker có chặn blocked với level cụ thể hoặc ALL không */
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

    Optional<Block> findByBlockerAndBlocked(User blocker, User blocked);
}
