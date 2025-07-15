package com.splusz.villigo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.ChatRoomParticipant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomDto {
    private Long id;
    private String name;
    private String status;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    
    // 상대방 유저 정보 추가
    private Long otherUserId;        // 상대방 유저 ID
    private String otherUserNickName;    // 상대방 이름
    private String otherUserAvatarImageUrl;  // 상대방 아바타 URL
    private boolean otherUserIsOnline; // 상대방 온라인 상태
    
    private List<ReservationDto> reservations;

}