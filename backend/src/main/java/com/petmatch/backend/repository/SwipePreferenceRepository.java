package com.petmatch.backend.repository;

import com.petmatch.backend.entity.SwipePreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SwipePreferenceRepository extends JpaRepository<SwipePreference, Long> {
}
