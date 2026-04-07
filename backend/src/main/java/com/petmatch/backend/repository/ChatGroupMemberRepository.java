package com.petmatch.backend.repository;

import com.petmatch.backend.entity.ChatGroup;
import com.petmatch.backend.entity.ChatGroupMember;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatGroupMemberRepository extends JpaRepository<ChatGroupMember, Long> {

    List<ChatGroupMember> findByGroup(ChatGroup group);

    Optional<ChatGroupMember> findByGroupAndUser(ChatGroup group, User user);

    boolean existsByGroupAndUser(ChatGroup group, User user);

    void deleteByGroupAndUser(ChatGroup group, User user);
}
