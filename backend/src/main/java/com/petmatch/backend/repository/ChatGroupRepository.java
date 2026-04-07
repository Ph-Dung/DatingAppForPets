package com.petmatch.backend.repository;

import com.petmatch.backend.entity.ChatGroup;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatGroupRepository extends JpaRepository<ChatGroup, Long> {

    /** Lấy tất cả nhóm mà user là thành viên */
    @Query("SELECT g FROM ChatGroup g JOIN g.members m WHERE m.user = :user ORDER BY g.createdAt DESC")
    List<ChatGroup> findGroupsByUser(@Param("user") User user);
}
