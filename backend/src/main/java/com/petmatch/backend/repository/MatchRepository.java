package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Match;
import com.petmatch.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ❤️ Tra cứu ghép đôi cấp user (dùng cho chat)
 * 
 * Dùng bởi: MatchRequestService.createOrUpdateUserMatch()
 * Dùng bởi: MessageService (tìm Match để quản lý conversations)
 * 
 * Lưu ý: Match (user-level) khác MatchRequest (pet-level)
 * - MatchRequest: Pet A like Pet B (PENDING/ACCEPTED/REJECTED)
 * - Match: User A + User B đã ghép đôi (tạo khi mutual MatchRequest ACCEPTED)
 * 
 * Tính năng:
 * - Tìm Match bidirectional: (user1, user2) hoặc (user2, user1)
 * - Danh sách tất cả Match của user (để load chat conversations)
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    @Query("SELECT m FROM Match m WHERE (m.user1.id = :user1Id AND m.user2.id = :user2Id) OR (m.user1.id = :user2Id AND m.user2.id = :user1Id)")
    Optional<Match> findMatchByUserIds(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Query("SELECT m FROM Match m WHERE m.user1 = :user OR m.user2 = :user")
    List<Match> findAllByUser(@Param("user") User user);
}
