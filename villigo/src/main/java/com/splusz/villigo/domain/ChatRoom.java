package com.splusz.villigo.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chat_rooms")
@Getter
@Setter
@NoArgsConstructor
public class ChatRoom {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String status;

    @Transient
    private String lastMessage;

    @Transient
    private LocalDateTime lastMessageTime;

    @Transient
    private long unreadCount;

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomParticipant> participants = new ArrayList<>();

    public void addParticipant(User user) {
        ChatRoomParticipant participant = new ChatRoomParticipant(this, user);
        participant.setLeftAt(null);
        participants.add(participant);
    }

    public List<User> getParticipantsAsUsers() {
        return participants.stream()
                .map(ChatRoomParticipant::getUser)
                .toList();
    } 

    public void removeParticipant(User user) {
        participants.removeIf(participant -> participant.getUser().equals(user));
    }
}