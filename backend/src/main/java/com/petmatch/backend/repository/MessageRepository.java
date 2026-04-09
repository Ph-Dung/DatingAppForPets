package com.petmatch.backend.repository;

import com.petmatch.backend.entity.Message;
import com.petmatch.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Lịch sử chat giữa 2 người, lọc tin nhắn không bị xóa mềm.
     * Nếu sender xóa conversation lúc T1 → chỉ lấy tin nhắn sau T1 (khi nhắn lại).
     * Nếu chưa xóa lần nào → deletedAt = null → lấy tất cả.
     */
    @Query("""
        SELECT m FROM Message m WHERE
        ((m.sender = :user1 AND m.receiver = :user2 AND (m.deletedBySenderAt IS NULL OR m.sentAt > m.deletedBySenderAt))
        OR
        (m.sender = :user2 AND m.receiver = :user1 AND (m.deletedByReceiverAt IS NULL OR m.sentAt > m.deletedByReceiverAt)))
        ORDER BY m.sentAt ASC
    """)
    Page<Message> findChatHistory(@Param("user1") User user1,
                                  @Param("user2") User user2,
                                  Pageable pageable);

    /** Tin nhắn cuối cùng giữa 2 người (để build conversation list) */
    @Query("""
        SELECT m FROM Message m WHERE
        (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)
        ORDER BY m.sentAt DESC LIMIT 1
    """)
    Optional<Message> findLastMessage(@Param("u1") User u1, @Param("u2") User u2);

    /** Đếm tin nhắn chưa đọc từ sender gửi cho receiver */
    @Query("""
        SELECT COUNT(m) FROM Message m
        WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false
        AND (m.deletedByReceiverAt IS NULL OR m.sentAt > m.deletedByReceiverAt)
    """)
    long countUnread(@Param("sender") User sender, @Param("receiver") User receiver);

    /** Đánh dấu tất cả tin nhắn từ sender → receiver là đã đọc */
    @Modifying
    @Query("""
        UPDATE Message m SET m.isRead = true
        WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false
    """)
    void markAllAsRead(@Param("sender") User sender, @Param("receiver") User receiver);

    /** Kiểm tra 2 người đã từng nhắn tin chưa (dùng cho review gate) */
    @Query("""
        SELECT COUNT(m) > 0 FROM Message m
        WHERE (m.sender = :u1 AND m.receiver = :u2) OR (m.sender = :u2 AND m.receiver = :u1)
    """)
    boolean existsChat(@Param("u1") User u1, @Param("u2") User u2);

    /** Soft-delete từ phía sender: đặt deletedBySenderAt = now */
    @Modifying
    @Query("""
        UPDATE Message m SET m.deletedBySenderAt = :now
        WHERE m.sender = :me AND m.receiver = :other AND m.deletedBySenderAt IS NULL
    """)
    void softDeleteBySender(@Param("me") User me, @Param("other") User other, @Param("now") LocalDateTime now);

    /** Soft-delete từ phía receiver: đặt deletedByReceiverAt = now */
    @Modifying
    @Query("""
        UPDATE Message m SET m.deletedByReceiverAt = :now
        WHERE m.sender = :other AND m.receiver = :me AND m.deletedByReceiverAt IS NULL
    """)
    void softDeleteByReceiver(@Param("me") User me, @Param("other") User other, @Param("now") LocalDateTime now);
}
