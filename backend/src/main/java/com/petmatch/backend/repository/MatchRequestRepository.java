package com.petmatch.backend.repository;

import com.petmatch.backend.entity.MatchRequest;
import com.petmatch.backend.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface MatchRequestRepository extends JpaRepository<MatchRequest, Long> {

    // Kiểm tra đã gửi request chưa
    boolean existsBySenderPetIdAndReceiverPetId(Long senderPetId, Long receiverPetId);

    // Danh sách request đã gửi
    List<MatchRequest> findBySenderPetIdOrderByCreatedAtDesc(Long senderPetId);

    // Danh sách request nhận được (đang pending)
    List<MatchRequest> findByReceiverPetIdAndStatusOrderByCreatedAtDesc(
            Long receiverPetId, MatchStatus status);

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
}
