package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlockRepository extends JpaRepository<Block, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    Optional<Block> findByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
