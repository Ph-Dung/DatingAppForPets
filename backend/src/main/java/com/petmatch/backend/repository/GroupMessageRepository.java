package com.petmatch.backend.repository;

import com.petmatch.backend.entity.ChatGroup;
import com.petmatch.backend.entity.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMessageRepository extends JpaRepository<GroupMessage, Long> {

    /** Lấy tin nhắn của nhóm với phân trang, mới nhất trước */
    Page<GroupMessage> findByGroupOrderBySentAtDesc(ChatGroup group, Pageable pageable);
}
