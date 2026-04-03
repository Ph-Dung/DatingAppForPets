package com.petmatch.backend.repository;

import com.petmatch.backend.entity.CallHistory;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CallHistoryRepository extends JpaRepository<CallHistory, Long> {
    
    @Query("SELECT c FROM CallHistory c WHERE c.caller = :user OR c.callee = :user ORDER BY c.startedAt DESC")
    List<CallHistory> findByCallerOrCallee(@Param("user") User user);
}
