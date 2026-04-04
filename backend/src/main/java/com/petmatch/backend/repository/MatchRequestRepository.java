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

@Repository
public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    // Kiểm tra đã gửi request chưa
    boolean existsBySenderPetIdAndReceiverPetId(Long senderPetId, Long receiverPetId);

    // Danh sách request đã gửi
    List<MatchRequest> findBySenderPetIdOrderByCreatedAtDesc(Long senderPetId);

    // Danh sách request nhận được với status cụ thể
    List<MatchRequest> findByReceiverPetIdAndStatusOrderByCreatedAtDesc(
            Long receiverPetId, MatchStatus status);

    // Ai đã like/super-like mình → super like xếp trước, rồi theo thời gian mới nhất
    List<MatchRequest> findByReceiverPetIdOrderByIsSuperLikeDescCreatedAtDesc(Long receiverPetId);

    // Kiểm tra 2 pet có mutual match không (cả 2 đều accepted)
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM MatchRequest m
        WHERE m.senderPet.id = :petA AND m.receiverPet.id = :petB
          AND m.status = 'ACCEPTED'
        """)
    boolean isMatched(@Param("petA") Long petA, @Param("petB") Long petB);

    // Lấy match thành công (cả 2 chiều)
    @Query("""
        SELECT m FROM MatchRequest m
        WHERE m.status = 'ACCEPTED'
          AND (m.senderPet.id = :petId OR m.receiverPet.id = :petId)
        """)
    List<MatchRequest> findAcceptedByPetId(@Param("petId") Long petId);

    Optional<MatchRequest> findBySenderPetIdAndReceiverPetId(
            Long senderPetId, Long receiverPetId);

    // Kiểm tra hôm nay đã dùng super like chưa
    boolean existsBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(
            Long senderPetId, LocalDateTime since);

    // Đếm số super like đã dùng từ thời điểm X
    long countBySenderPetIdAndIsSuperLikeTrueAndCreatedAtAfter(
            Long senderPetId, LocalDateTime since);
}
