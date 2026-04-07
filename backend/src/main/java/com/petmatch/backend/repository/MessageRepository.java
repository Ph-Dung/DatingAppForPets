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



@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Lấy lịch sử chat giữa 2 người với phân trang */
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender = :user1 AND m.receiver = :user2) OR " +
           "(m.sender = :user2 AND m.receiver = :user1) " +
           "ORDER BY m.sentAt ASC")
    Page<Message> findChatHistory(@Param("user1") User user1,
                                  @Param("user2") User user2,
                                  Pageable pageable);

    /** Đếm số tin nhắn chưa đọc từ sender gửi cho receiver */
    @Query("SELECT COUNT(m) FROM Message m WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    long countUnread(@Param("sender") User sender, @Param("receiver") User receiver);

    /** Đánh dấu tất cả tin nhắn từ sender gửi cho receiver là đã đọc */
    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    void markAllAsRead(@Param("sender") User sender, @Param("receiver") User receiver);
}

