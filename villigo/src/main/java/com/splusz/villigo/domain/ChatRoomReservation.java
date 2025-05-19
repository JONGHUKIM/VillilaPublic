package com.splusz.villigo.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_room_reservations")
@IdClass(ChatRoomReservationId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomReservation {
    @Id
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Id
    @Column(name = "reservation_id")
    private Long reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", insertable = false, updatable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", insertable = false, updatable = false)
    private Reservation reservation;

    public ChatRoomReservation(ChatRoom chatRoom, Reservation reservation) {
        this.chatRoom = chatRoom;
        this.reservation = reservation;
        this.chatRoomId = chatRoom.getId();
        this.reservationId = reservation.getId();
    }
}