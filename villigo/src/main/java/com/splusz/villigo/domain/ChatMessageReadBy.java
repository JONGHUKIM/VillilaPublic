package com.splusz.villigo.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CHAT_MESSAGE_READ_BY")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ChatMessageReadBy {

    @EmbeddedId
    private ChatMessageReadById id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("chatMessageId")
    @JoinColumn(name = "CHAT_MESSAGE_ID")
    private ChatMessage chatMessage;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "IS_READ", nullable = false)
    private boolean isRead = false;

    @Column(name = "READ_TIME")
    private LocalDateTime readTime;

    public ChatMessageReadBy(ChatMessage chatMessage, User user) {
        this.id = new ChatMessageReadById(chatMessage.getId(), user.getId());
        this.chatMessage = chatMessage;
        this.user = user;
        this.isRead = false;
    }
}