package com.splusz.villigo.domain;

import java.io.Serializable;
import java.util.Objects;

public class ChatRoomParticipantId implements Serializable {
    private Long chatRoomId; // chat_room_id에 매핑
    private Long userId;     // user_id에 매핑

    public ChatRoomParticipantId() {}

    public ChatRoomParticipantId(Long chatRoomId, Long userId) {
        this.chatRoomId = chatRoomId;
        this.userId = userId;
    }

    public Long getChatRoomId() {
        return chatRoomId;
    }

    public void setChatRoomId(Long chatRoomId) {
        this.chatRoomId = chatRoomId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatRoomParticipantId that = (ChatRoomParticipantId) o;
        return Objects.equals(chatRoomId, that.chatRoomId) &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(chatRoomId, userId);
    }
}