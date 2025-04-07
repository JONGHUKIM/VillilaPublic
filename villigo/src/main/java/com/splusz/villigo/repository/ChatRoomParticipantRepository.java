package com.splusz.villigo.repository;

import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.ChatRoomParticipant;
import com.splusz.villigo.domain.ChatRoomParticipantId;
import com.splusz.villigo.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, ChatRoomParticipantId> {

    @Query("SELECT crp.user.id FROM ChatRoomParticipant crp WHERE crp.chatRoom.id = :chatRoomId")
    List<Long> findUserIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId);
    
    @Modifying
    @Query("UPDATE ChatRoomParticipant p SET p.leftAt = :leftAt WHERE p.chatRoom.id = :roomId AND p.user.id = :userId")
    void markParticipantAsLeft(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("leftAt") LocalDateTime leftAt);

    // leftAt 조건 제거
    @Query("SELECT p FROM ChatRoomParticipant p WHERE p.chatRoomId = :roomId AND p.userId = :userId")
    Optional<ChatRoomParticipant> findByRoomIdAndUserId(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Query("SELECT COUNT(p) FROM ChatRoomParticipant p WHERE p.chatRoom.id = :roomId AND p.leftAt IS NULL")
    long countActiveParticipants(@Param("roomId") Long roomId);
    
    @Query("SELECT crp.chatRoom FROM ChatRoomParticipant crp WHERE crp.user.id = :userId AND crp.leftAt IS NULL")
    List<ChatRoom> findActiveChatRoomsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ChatRoomParticipant crp WHERE crp.chatRoom.id = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);

    @Query("SELECT crp.user.id FROM ChatRoomParticipant crp WHERE crp.chatRoom.id = :chatRoomId")
    List<Long> findParticipantIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId);
    
    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);
}