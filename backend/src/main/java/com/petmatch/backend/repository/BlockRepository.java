package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Block;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
    boolean existsByBlockerAndBlocked(User blocker, User blocked);
    List<Block> findByBlocker(User blocker);
}
