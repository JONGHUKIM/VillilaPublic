package com.splusz.villigo.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "CHAT_MESSAGES")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ToString.Exclude 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CHAT_ROOM_ID")
    @Basic(optional = false)
    @JsonIgnore
    private ChatRoom chatRoom;

    @ToString.Exclude 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "SENDER_ID")
    @Basic(optional = false)
    @JsonIgnore
    private User sender;
    
    @Enumerated(EnumType.STRING)
    @Basic(optional = false)
    private ChatMessageType messageType;
    
    @Basic(optional = false)
    private String content;
    
    @Column(name = "CREATED_TIME", updatable = false, nullable = false)
    private LocalDateTime createdTime;
    
    @OneToMany(mappedBy = "chatMessage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMessageReadBy> readBy = new ArrayList<>();
    
    public ChatMessage(User sender, ChatRoom chatRoom, String content) {
        this.sender = sender;
        this.chatRoom = chatRoom;
        this.content = content;
        this.messageType = ChatMessageType.TEXT;
    }

    @PrePersist
    public void prePersist() {
        this.createdTime = LocalDateTime.now();
    }
    
    public Long getSenderId() {
        return sender != null ? sender.getId() : null;
    }

    public String getSenderUsername() {
        return sender != null ? sender.getUsername() : null;
    }

    public Long getChatRoomId() {
        return chatRoom != null ? chatRoom.getId() : null;
    }
}