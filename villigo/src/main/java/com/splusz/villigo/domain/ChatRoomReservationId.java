package com.splusz.villigo.domain;

import java.io.Serializable;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class ChatRoomReservationId implements Serializable {
    private Long chatRoomId;
    private Long reservationId;
}
