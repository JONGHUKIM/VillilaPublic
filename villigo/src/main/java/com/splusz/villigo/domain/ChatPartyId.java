package com.splusz.villigo.domain;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
@Embeddable
public class ChatPartyId implements Serializable {  // 반드시 Serializable 구현

    @Column(name = "chat_room_id")
    private Long chatRoomId;  // 복합 키의 첫 번째 값

    @Column(name = "participant_id")
    private Long participantId;  // 복합 키의 두 번째 값

    public ChatPartyId(Long chatRoomId, Long participantId) {
        this.chatRoomId = chatRoomId;
        this.participantId = participantId;
    }
}
