package com.splusz.villigo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.ChatMessageReadBy;
import com.splusz.villigo.domain.ChatMessageReadById;

public interface ChatMessageReadByRepository extends JpaRepository<ChatMessageReadBy, ChatMessageReadById> {

    @Query("SELECT cmrb FROM ChatMessageReadBy cmrb WHERE cmrb.chatMessage.id = :chatMessageId")
    List<ChatMessageReadBy> findByChatMessageId(@Param("chatMessageId") Long chatMessageId);

    @Query("SELECT COUNT(r) FROM ChatMessageReadBy r " +
           "WHERE r.chatMessage.chatRoom.id = :chatRoomId " +
           "AND r.user.id = :userId " +
           "AND r.isRead = false")
    long countUnreadMessagesByUserIdAndChatRoomId(
        @Param("userId") Long userId,
        @Param("chatRoomId") Long chatRoomId
    );

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ChatMessageReadBy r " +
           "SET r.isRead = true, r.readTime = :readTime " +
           "WHERE r.chatMessage.chatRoom.id = :chatRoomId " +
           "AND r.user.id = :userId " +
           "AND r.isRead = false")
    void markAllAsReadByUserIdAndChatRoomId(
        @Param("userId") Long userId,
        @Param("chatRoomId") Long chatRoomId,
        @Param("readTime") LocalDateTime readTime
    );

    @Modifying
    @Query("DELETE FROM ChatMessageReadBy r WHERE r.chatMessage.id = :chatMessageId")
    void deleteByChatMessageId(@Param("chatMessageId") Long chatMessageId);

    @Modifying
    @Query("DELETE FROM ChatMessageReadBy r WHERE r.chatMessage.chatRoom.id = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(m) FROM ChatMessage m " +
    	       "LEFT JOIN ChatMessageReadBy r ON r.chatMessage.id = m.id AND r.user.id = :userId " +
    	       "WHERE m.chatRoom.id = :chatRoomId AND (r.id IS NULL OR r.isRead = false)")
    	long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    
    @Query("SELECT COUNT(r) FROM ChatMessageReadBy r " +
    	       "WHERE r.user.id = :userId AND r.isRead = false")
    	long countUnreadMessagesByUserId(@Param("userId") Long userId);

    
}