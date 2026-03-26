package com.petmatch.backend.repository;

import com.petmatch.backend.entity.MatchRequest;
import com.petmatch.backend.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRequestRepository extends JpaRepository<MatchRequest, UUID> {

    // Kiểm tra đã gửi request chưa
    boolean existsBySenderPetIdAndReceiverPetId(UUID senderPetId, UUID receiverPetId);

    // Danh sách request đã gửi
    List<MatchRequest> findBySenderPetIdOrderByCreatedAtDesc(UUID senderPetId);

    // Danh sách request nhận được (đang pending)
    List<MatchRequest> findByReceiverPetIdAndStatusOrderByCreatedAtDesc(
            UUID receiverPetId, MatchStatus status);

    // Kiểm tra 2 pet có mutual match không (cả 2 đều accepted)
    @Query("""
        SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END
        FROM MatchRequest m
        WHERE m.senderPet.id = :petA AND m.receiverPet.id = :petB
          AND m.status = 'ACCEPTED'
        """)
    boolean isMatched(@Param("petA") UUID petA, @Param("petB") UUID petB);

    // Lấy match thành công (cả 2 chiều)
    @Query("""
        SELECT m FROM MatchRequest m
        WHERE m.status = 'ACCEPTED'
          AND (m.senderPet.id = :petId OR m.receiverPet.id = :petId)
        """)
    List<MatchRequest> findAcceptedByPetId(@Param("petId") UUID petId);

    Optional<MatchRequest> findBySenderPetIdAndReceiverPetId(
            UUID senderPetId, UUID receiverPetId);
}
