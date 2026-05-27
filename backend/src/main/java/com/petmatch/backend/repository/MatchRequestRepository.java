package com.petmatch.backend.repository;

import com.petmatch.backend.entity.MatchRequest;
import com.petmatch.backend.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository: Query MatchRequest (pet-level ghép đôi)
 * 
 * Queries chính:
 * - Duplicate check: existsBySenderPetIdAndReceiverPetId
 * - Quota check: existsBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter
 * - Suggestions: findByReceiverPetIdOrderByIsSuperLikeDescCreatedAtDesc (super like first)
 * - Mutual: isMatched (cả 2 chiều ACCEPTED)
 */
@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    // ════════════════════════════════════════════════════════════════
    // VALIDATION QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Kiểm tra đã gửi request tới pet này chưa (tránh duplicate) */
    boolean existsBySenderPetIdAndReceiverPetId(Long senderPetId, Long receiverPetId);

    // ════════════════════════════════════════════════════════════════
    // SUGGESTION QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Danh sách đã gửi like → tìm response */
    List<MatchRequest> findBySenderPetIdOrderByCreatedAtDesc(Long senderPetId);

    /** Ai đã like/super-like mình → super like xếp trước, rồi mới nhất */
    List<MatchRequest> findByReceiverPetIdOrderByIsSuperLikeDescCreatedAtDesc(Long receiverPetId);

    // ════════════════════════════════════════════════════════════════
    // SUPER LIKE QUOTA QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Kiểm tra hôm nay đã dùng super like chưa (quota: 1/ngày) */
    boolean existsBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(
            Long senderPetId, LocalDateTime since);

    // ════════════════════════════════════════════════════════════════
    // MUTUAL MATCH QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Kiểm tra 2 pet có mutual match không (A ACCEPTED B AND B ACCEPTED A) */
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM MatchRequest m
        WHERE m.senderPet.id = :petA AND m.receiverPet.id = :petB
          AND m.status = 'ACCEPTED'
        """)
    boolean isMatched(@Param("petA") Long petA, @Param("petB") Long petB);

    /** Lấy danh sách match thành công (cả 2 chiều → có thể chat) */
    @Query("""
        SELECT m FROM MatchRequest m
        WHERE m.status = 'ACCEPTED'
          AND (m.senderPet.id = :petId OR m.receiverPet.id = :petId)
        """)
    List<MatchRequest> findAcceptedByPetId(@Param("petId") Long petId);

    // ════════════════════════════════════════════════════════════════
    // LOOKUP QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Tìm request 1 chiều: từ A sang B */
    Optional<MatchRequest> findBySenderPetIdAndReceiverPetId(
            Long senderPetId, Long receiverPetId);

    // ════════════════════════════════════════════════════════════════
    // DELETE QUERIES
    // ════════════════════════════════════════════════════════════════
    
    /** Xóa toàn bộ match của pet (khi xóa pet) */
    void deleteBySenderPetIdOrReceiverPetId(Long senderPetId, Long receiverPetId);
    @Query("""
        SELECT m.receiverPet FROM MatchRequest m
        WHERE m.senderPet.id = :petId
          AND m.status = 'ACCEPTED'
        ORDER BY m.createdAt DESC
        """)
    List<com.petmatch.backend.entity.PetProfile> findLikedPetsBySenderPetId(
            @Param("petId") Long petId);

    /** Đếm tổng số lần like (ACCEPTED) của pet */
    long countBySenderPetIdAndStatus(Long senderPetId, MatchStatus status);
}

