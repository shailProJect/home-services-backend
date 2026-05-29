package com.homeservices.repository;

import com.homeservices.entity.SupportMessage;
import com.homeservices.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SupportMessageRepository extends JpaRepository<SupportMessage, UUID> {

    /** All messages in a user's thread, oldest first. */
    List<SupportMessage> findByUserOrderByCreatedAtAsc(User user);

    /** Count unread ADMIN messages for a user (shown as badge on user widget). */
    long countByUserAndSenderRoleAndReadFalse(
            User user,
            SupportMessage.SenderRole senderRole
    );

    /** Mark all messages in a thread as read by role (user marks admin msgs; admin marks user msgs). */
    @Modifying
    @Query("UPDATE SupportMessage m SET m.read = true " +
           "WHERE m.user = :user AND m.senderRole = :role AND m.read = false")
    void markReadByUserAndRole(
            @Param("user") User user,
            @Param("role") SupportMessage.SenderRole role
    );

    /** Get latest message per user thread (for admin thread list). */
    @Query("SELECT m FROM SupportMessage m " +
           "WHERE m.createdAt = (" +
           "  SELECT MAX(m2.createdAt) FROM SupportMessage m2 WHERE m2.user = m.user" +
           ") ORDER BY m.createdAt DESC")
    List<SupportMessage> findLatestPerUser();

    /** Count unread USER messages per user (shown as badge on admin side). */
    @Query("SELECT COUNT(m) FROM SupportMessage m " +
           "WHERE m.user = :user AND m.senderRole = 'USER' AND m.read = false")
    long countUnreadUserMessages(@Param("user") User user);
}
