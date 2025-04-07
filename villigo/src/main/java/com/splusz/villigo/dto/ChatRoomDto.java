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
    private String otherUserAvatar;  // 상대방 아바타 URL
    private boolean otherUserIsOnline; // 상대방 온라인 상태
    
    private List<ReservationDto> reservations;

    // 엔티티 -> DTO 변환
//    public static ChatRoomDto fromEntity(ChatRoom chatRoom, Long currentUserId) {
//        ChatRoomDto dto = new ChatRoomDto();
//        dto.setId(chatRoom.getId());
//        dto.setName(chatRoom.getName());
//        dto.setStatus(chatRoom.getStatus());
//        dto.setLastMessage(chatRoom.getLastMessage());
//        dto.setLastMessageTime(chatRoom.getLastMessageTime());
//        dto.setUnreadCount(chatRoom.getUnreadCount());
//
//        if (chatRoom.getParticipants() != null) {
//            chatRoom.getParticipants().stream()
//                .map(ChatRoomParticipant::getUser)
//                .filter(user -> !user.getId().equals(currentUserId))
//                .findFirst()
//                .ifPresent(user -> {
//                    dto.setOtherUserId(user.getId());
//                    dto.setOtherUserNickName(user.getNickname() != null ? user.getNickname() : "알 수 없는 사용자");
//                    dto.setOtherUserAvatar(user.getAvatar());
//                    dto.setOtherUserIsOnline(user.isOnline());
//                });
//        }
//
//        return dto;
//    }
}