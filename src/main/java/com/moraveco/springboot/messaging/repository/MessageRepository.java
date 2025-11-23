package com.moraveco.springboot.messaging.repository;

import com.moraveco.springboot.messaging.entity.ChatMessage;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query(value = "SELECT * FROM ChatMessages", nativeQuery = true)
    List<ChatMessage> getAllMessages();

    @Query(value = "SELECT * FROM ChatMessages WHERE (sender_uid = :senderUid AND receiver_uid = :receiverUid) OR (sender_uid = :receiverUid AND receiver_uid = :senderUid) ORDER BY timestamp", nativeQuery = true)
    List<ChatMessage> getSecretChatMessages(
            @Param("senderUid") String senderUid,
            @Param("receiverUid") String receiverUid
    );

    @Query("SELECT m FROM ChatMessage m WHERE " +
            "(m.senderUid = :uid1 AND m.receiverUid = :uid2) OR " +
            "(m.senderUid = :uid2 AND m.receiverUid = :uid1) " +
            "ORDER BY m.timestamp DESC")
    Page<ChatMessage> findConversation(
            @Param("uid1") String uid1,
            @Param("uid2") String uid2,
            Pageable pageable
    );

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO ChatMessages (id, sender_uid, receiver_uid, content, timestamp, `read`, respond_message_id, image_url) " +
            "VALUES (:id, :senderUid, :receiverUid,  :content, :timestamp, :isRead, :respondMessageId, :imageUrl)", nativeQuery = true)
    void insertMessage(
            @Param("id") String id,
            @Param("senderUid") String senderUid,
            @Param("receiverUid") String receiverUid,
            @Param("content") String content,
            @Param("timestamp") String timestamp,
            @Param("isRead") boolean isRead,
            @Param("respondMessageId") String respondMessageId,
            @Param("imageUrl") String imageUrl
    );

    @Transactional
    @Modifying
    @Query(value = "DELETE FROM ChatMessages WHERE id = :messageId", nativeQuery = true)
    void deleteMessage(@Param("messageId") String messageId);

    @Transactional
    @Modifying
    @Query(value = "UPDATE ChatMessages SET `read` = true WHERE id = :messageId", nativeQuery = true)
    void markMessageAsRead(@Param("messageId") String messageId);

    @Query(value = """
    SELECT COUNT(*) 
    FROM blocked_users 
    WHERE (blocker_id = :blockerId AND blocked_id = :blockedId)
       OR (blocker_id = :blockedId AND blocked_id = :blockerId)
    """,
            nativeQuery = true)
    long isBlocked(@Param("blockerId") String blockerId,
                   @Param("blockedId") String blockedId);


    // Mark all ChatMessages as read for a conversation
    @Transactional
    @Modifying
    @Query(value = "UPDATE ChatMessages SET `read` = true WHERE sender_uid = :senderUid AND receiver_uid = :receiverUid", nativeQuery = true)
    void markConversationAsRead(@Param("senderUid") String senderUid, @Param("receiverUid") String receiverUid);

    // Get unread message count for a user
    @Query(value = "SELECT COUNT(*) FROM ChatMessages WHERE receiver_uid = :userUid AND `read` = false", nativeQuery = true)
    int getUnreadMessageCount(@Param("userUid") String userUid);

    // Get unread ChatMessages for a user
    @Query(value = "SELECT * FROM ChatMessages WHERE receiver_uid = :userUid AND `read` = false ORDER BY timestamp DESC", nativeQuery = true)
    List<ChatMessage> getUnreadChatMessages(@Param("userUid") String userUid);

    // Get unread count for specific conversation
    @Query(value = "SELECT COUNT(*) FROM ChatMessages WHERE sender_uid = :senderUid AND receiver_uid = :receiverUid AND `read` = false", nativeQuery = true)
    int getUnreadCountForConversation(@Param("senderUid") String senderUid, @Param("receiverUid") String receiverUid);

    // Get conversation ChatMessages with read status
    @Query(value = "SELECT * FROM ChatMessages WHERE " +
            "(sender_uid = :senderUid AND receiver_uid = :receiverUid) OR " +
            "(sender_uid = :receiverUid AND receiver_uid = :senderUid) " +
            "ORDER BY timestamp ASC", nativeQuery = true)
    List<ChatMessage> getConversationChatMessages(@Param("senderUid") String senderUid, @Param("receiverUid") String receiverUid);


}
