package com.petmatch.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.petmatch.backend.entity.CommunityNotification;
import com.petmatch.backend.entity.Post;
import com.petmatch.backend.entity.User;
import com.petmatch.backend.enums.CommunityNotificationType;

@Repository
public interface CommunityNotificationRepository extends JpaRepository<CommunityNotification, Long> {

    List<CommunityNotification> findAllByRecipientOrderByCreatedAtDesc(User recipient);

    long countByRecipientAndIsReadFalse(User recipient);

    boolean existsByRecipientAndActorAndPostAndType(
            User recipient,
            User actor,
            Post post,
            CommunityNotificationType type
    );

    @Modifying
    @Query("update CommunityNotification n set n.isRead = true where n.recipient = :recipient and n.isRead = false")
    int markAllAsReadByRecipient(@Param("recipient") User recipient);
}
