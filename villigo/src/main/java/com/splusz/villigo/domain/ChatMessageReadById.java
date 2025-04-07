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
public class ChatMessageReadById implements Serializable {

    @Column(name = "CHAT_MESSAGE_ID")
    private Long chatMessageId;

    @Column(name = "USER_ID")
    private Long userId;

    public ChatMessageReadById(Long chatMessageId, Long userId) {
        this.chatMessageId = chatMessageId;
        this.userId = userId;
    }
}