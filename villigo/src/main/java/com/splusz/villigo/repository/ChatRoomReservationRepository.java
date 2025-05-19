package com.splusz.villigo.repository;

import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.ChatRoomReservation;
import com.splusz.villigo.domain.ChatRoomReservationId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatRoomReservationRepository extends JpaRepository<ChatRoomReservation, ChatRoomReservationId> {
    @Query("SELECT crr.chatRoom FROM ChatRoomReservation crr WHERE crr.reservationId = :reservationId")
    List<ChatRoom> findChatRoomsByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT crr FROM ChatRoomReservation crr WHERE crr.chatRoom.id = :chatRoomId")
    List<ChatRoomReservation> findByChatRoomId(@Param("chatRoomId") Long chatRoomId);
    
    @Modifying
    @Query("DELETE FROM ChatRoomReservation crr WHERE crr.chatRoom.id = :roomId")
    void deleteByChatRoomId(@Param("roomId") Long roomId);
    
    @Modifying
    @Query("DELETE FROM ChatRoomReservation crr WHERE crr.reservationId = :reservationId")
    void deleteByReservationId(@Param("reservationId") Long reservationId);
}