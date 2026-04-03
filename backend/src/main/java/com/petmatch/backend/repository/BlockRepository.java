package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.Long;
import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
}
