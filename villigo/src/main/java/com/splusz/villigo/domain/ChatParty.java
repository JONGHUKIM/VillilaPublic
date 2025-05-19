package com.splusz.villigo.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_parties")
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ChatParty {
	
    @EmbeddedId
    private ChatPartyId id;  // 복합 키를 EmbeddedId로 사용

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatRoomId")  // EmbeddedId 내부 필드 매핑
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("participantId")  // EmbeddedId 내부 필드 매핑
    @JoinColumn(name = "participant_id")
    private User participant;

    public ChatParty(ChatRoom chatRoom, User participant) {
        this.id = new ChatPartyId(chatRoom.getId(), participant.getId());
        this.chatRoom = chatRoom;
        this.participant = participant;
    }
}
