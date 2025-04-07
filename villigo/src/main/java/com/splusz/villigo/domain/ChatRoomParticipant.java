package com.splusz.villigo.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.splusz.villigo.repository.ChatRoomParticipantRepository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "chat_room_participants")
@IdClass(ChatRoomParticipantId.class)
@Getter
@Setter
@NoArgsConstructor
public class ChatRoomParticipant {

    @Id
    @Column(name = "chat_room_id")
    private Long chatRoomId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "chat_room_id", insertable = false, updatable = false, nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, nullable = false)
    @JsonBackReference
    private User user;
    
    @Column(name = "left_at")
    private LocalDateTime leftAt;

    public ChatRoomParticipant(ChatRoom chatRoom, User user) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.chatRoomId = chatRoom.getId();
        this.userId = user.getId();
        this.leftAt = null;
    }
    
    public boolean isLeft() {
        return leftAt != null;
    }

    public void markAsLeft() {
        this.leftAt = LocalDateTime.now();
    }

    public void rejoin() {
        this.leftAt = null;
    }
}