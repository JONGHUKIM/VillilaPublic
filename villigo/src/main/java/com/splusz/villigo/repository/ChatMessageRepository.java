package com.splusz.villigo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatMessageType;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    
    // 기존 기능: 채팅방 메시지 조회
	@Query("SELECT cm FROM ChatMessage cm " +
		       "JOIN FETCH cm.sender s " +
		       "JOIN FETCH cm.chatRoom cr " +
		       "JOIN FETCH cm.readBy " +
		       "WHERE cm.chatRoom.id = :roomId " +
		       "ORDER BY cm.createdTime ASC")
		List<ChatMessage> findByChatRoom_IdOrderByCreatedTimeAsc(@Param("roomId") Long roomId);

    // 최신 메시지를 가져오는 기능
    List<ChatMessage> findByChatRoom_IdOrderByCreatedTimeDesc(Long chatRoomId);

    // 상대방이 보낸 마지막 메시지를 가져오는 기능 추가
    @Query("SELECT cm FROM ChatMessage cm " +
           "WHERE cm.chatRoom.id = :chatRoomId " +
           "AND cm.sender.id != :userId " +
           "ORDER BY cm.createdTime DESC")
    List<ChatMessage> findLastMessageByChatRoomIdAndNotSenderId(
        @Param("chatRoomId") Long chatRoomId,
        @Param("userId") Long userId
    );

    // 특정 사용자가 읽지 않은 메시지 개수 조회
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND NOT EXISTS (SELECT r FROM ChatMessageReadBy r WHERE r.chatMessage.id = m.id AND r.user.id = :userId AND r.isRead = true)")
    long countUnreadMessages(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
    
    @Modifying
    @Query("DELETE FROM ChatMessage m WHERE m.chatRoom.id = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);

    // 중복 메시지 확인을 위한 메서드
    @Query("SELECT m FROM ChatMessage m " +
           "WHERE m.chatRoom.id = :chatRoomId " +
           "AND m.sender.id = :senderId " +
           "AND m.content = :content " +
           "AND m.createdTime BETWEEN :startTime AND :endTime")
    List<ChatMessage> findByChatRoomIdAndSenderIdAndContentAndCreatedTimeBetween(
        @Param("chatRoomId") Long chatRoomId,
        @Param("senderId") Long senderId,
        @Param("content") String content,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime
    );

    ChatMessage findTopByChatRoom_IdOrderByCreatedTimeDesc(Long chatRoomId);

    List<ChatMessage> findByMessageTypeIn(List<ChatMessageType> messageTypes);
    
    // content 필드가 S3 이미지 경로를 저장하며, messageType이 IMAGE 또는 IMAGE_GROUP인 경우
    @Query("SELECT cm.content FROM ChatMessage cm WHERE cm.content IS NOT NULL AND (cm.messageType = 'IMAGE' OR cm.messageType = 'IMAGE_GROUP')")
    List<String> findAllChatImageContents();
  
    
}