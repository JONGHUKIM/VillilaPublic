package com.splusz.villigo.repository;

import com.splusz.villigo.domain.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    @Query("SELECT cr FROM ChatRoom cr LEFT JOIN FETCH cr.participants WHERE cr.id = :id")
    Optional<ChatRoom> findByIdWithParticipants(@Param("id") Long id);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id IN (SELECT crr.chatRoom.id FROM ChatRoomReservation crr WHERE crr.reservationId = :reservationId)")
    Optional<ChatRoom> findByReservationId(@Param("reservationId") Long reservationId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id IN (SELECT crp.chatRoom.id FROM ChatRoomParticipant crp WHERE crp.user.id = :userId1) " +
           "AND cr.id IN (SELECT crp.chatRoom.id FROM ChatRoomParticipant crp WHERE crp.user.id = :userId2)")
    Optional<ChatRoom> findExistingChatRoom(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.id IN (SELECT crp.chatRoom.id FROM ChatRoomParticipant crp WHERE crp.user.id = :userId)")
    List<ChatRoom> findByParticipantId(@Param("userId") Long userId);
    
    @Query("""
    	    SELECT DISTINCT cr FROM ChatRoom cr
    	    JOIN cr.participants crp1
    	    JOIN cr.participants crp2
    	    JOIN ChatRoomReservation crr ON crr.chatRoom.id = cr.id
    	    JOIN Reservation r ON crr.reservation.id = r.id
    	    WHERE crp1.user.id = :userId1
    	    AND crp2.user.id = :userId2
    	    AND r.product.id = :productId
    	""")
    	List<ChatRoom> findChatRoomsByUsersAndProduct(@Param("userId1") long userId1,
    	                                              @Param("userId2") long userId2,
    	                                              @Param("productId") long productId);

}