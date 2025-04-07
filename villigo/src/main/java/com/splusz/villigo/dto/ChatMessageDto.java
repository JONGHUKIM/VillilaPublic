package com.splusz.villigo.dto;

import java.time.LocalDateTime;
import java.util.Map;

import com.splusz.villigo.domain.ChatMessageType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
	
	private Long id;
	private Long chatRoomId;
	private Long senderId;
	private String senderName;
	private String otherParticipantName;
	private ChatMessageType messageType;
	private String content;
	private LocalDateTime createdAt;
	private Map<Long, Boolean> readBy;

}
