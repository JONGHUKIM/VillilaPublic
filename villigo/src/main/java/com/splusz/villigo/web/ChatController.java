package com.splusz.villigo.web;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ChatMessageDto;
import com.splusz.villigo.repository.ChatMessageReadByRepository;
import com.splusz.villigo.repository.ChatMessageRepository;
import com.splusz.villigo.service.AlarmService;
import com.splusz.villigo.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/chat")
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatMessageReadByRepository chatMessageReadByRepo;
    private final ChatMessageRepository chatMessageRepo;
    private final AlarmService alarmService;

    @Transactional
    @MessageMapping("/chat/enter/{roomId}")
    @SendTo("/topic/chat/room/{roomId}")
    public ChatMessageDto enterChatRoom(@DestinationVariable("roomId") Long roomId, @Payload ChatMessage message) {
        log.info("채팅방 입장 시작 - roomId: {}, userId: {}", roomId, message.getId());
        Long userId = message.getId();
        ChatRoom chatRoom = chatService.getChatRoom(roomId);
        User user = chatService.getUser(userId);

        Hibernate.initialize(user);
        log.info("User 정보: username={}", user.getUsername());
        String content = user.getUsername() + "님이 입장했습니다.";
        ChatMessage chatMessage = new ChatMessage(user, chatRoom, content);
        log.info("채팅 메시지 생성 완료: {}", chatMessage);

        // ChatMessage를 저장하고 DTO로 변환
        ChatMessage savedMessage = chatMessageRepo.save(chatMessage);
        return chatService.convertToDto(savedMessage, userId);
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDto chatMessageDto) {
        try {
            LocalDateTime createdTime = chatMessageDto.getCreatedAt() != null
                ? chatMessageDto.getCreatedAt()
                : LocalDateTime.now();

            ChatMessage existingMessage = chatService.findExistingMessage(
                chatMessageDto.getChatRoomId(),
                chatMessageDto.getSenderId(),
                chatMessageDto.getContent(),
                createdTime
            );
            if (existingMessage != null) {
                log.warn("중복 메시지 감지: chatRoomId={}, senderId={}, content={}",
                    chatMessageDto.getChatRoomId(), chatMessageDto.getSenderId(), chatMessageDto.getContent());
                return;
            }

            ChatMessage savedMessage = chatService.saveMessage(chatMessageDto);
            Hibernate.initialize(savedMessage.getSender());
            ChatMessageDto responseDto = chatService.convertToDto(savedMessage, chatMessageDto.getSenderId());
            if (responseDto.getSenderName() == null) {
                responseDto.setSenderName("알 수 없는 사용자");
                log.warn("senderName이 null입니다. senderId={}", savedMessage.getSender().getId());
            }
            log.info("채팅방 {}에 메시지 전송 (보낸 사람: {}): {}",
                savedMessage.getChatRoom().getId(),
                savedMessage.getSender().getId(),
                savedMessage.getContent());

            messagingTemplate.convertAndSend(
                "/topic/chat." + savedMessage.getChatRoom().getId(),
                responseDto
            );

            ChatRoom chatRoom = chatService.getChatRoom(savedMessage.getChatRoom().getId());
            List<User> participants = chatRoom.getParticipantsAsUsers();
            Long senderId = savedMessage.getSender().getId();
            User reciver = participants.stream()
            		.filter(user -> !user.getId().equals(senderId))
            		.findFirst()
            		.orElseThrow(null);
            
            if (reciver != null) {
            	String receiverUsernmae = reciver.getUsername();
            	String toastMessage = responseDto.getSenderName() + "님으로부터 새 메시지가 도착했습니다.";
            	alarmService.sendNotification(receiverUsernmae, toastMessage);
            	log.info("상대방 {} 에게 알림 전송", receiverUsernmae);
            }
            
            
            List<Long> participantIds = chatRoom.getParticipantsAsUsers().stream()
                .map(User::getId)
                .collect(Collectors.toList());
            for (Long userId : participantIds) {
                log.info("채팅방 목록 업데이트 이벤트 전송: /topic/chatrooms.{}", userId);
                messagingTemplate.convertAndSend(
                    "/topic/chatrooms." + userId,
                    Map.of("chatRoomId", chatRoom.getId())
                );
            }
        } catch (Exception e) {
            log.error("메시지 전송 중 오류 발생: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("message", "메시지 전송 중 오류가 발생했습니다: " + e.getMessage());
            messagingTemplate.convertAndSend(
                "/topic/chat." + chatMessageDto.getChatRoomId(),
                errorResponse
            );
        }
    }

    @PostMapping("/rooms/{roomId}/leave")
    public ResponseEntity<Map<String, Object>> leaveRoom(
            @PathVariable(name = "roomId") Long roomId,
            @RequestBody Map<String, Long> payload) {
        try {
            Long userId = payload.get("userId");
            if (userId == null) {
                throw new IllegalArgumentException("userId가 요청에 포함되어야 합니다.");
            }

            boolean isDeleted = chatService.leaveChatRoom(roomId, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("isDeleted", isDeleted);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isDeleted", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("isDeleted", false);
            errorResponse.put("error", "서버 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessageDto chatMessageDto) {
        try {
            ChatMessage existingMessage = chatService.findExistingMessage(
                chatMessageDto.getChatRoomId(),
                chatMessageDto.getSenderId(),
                chatMessageDto.getContent(),
                chatMessageDto.getCreatedAt()
            );
            if (existingMessage != null) {
                log.warn("중복 메시지 감지: chatRoomId={}, senderId={}, content={}",
                    chatMessageDto.getChatRoomId(), chatMessageDto.getSenderId(), chatMessageDto.getContent());
                return;
            }

            ChatMessage savedMessage = chatService.saveMessage(chatMessageDto);
            Long receiverId = chatService.getReceiverId(savedMessage.getChatRoom(), savedMessage.getSender().getId());
            ChatMessageDto responseDto = chatService.convertToDto(savedMessage, chatMessageDto.getSenderId());
            log.info("사용자 {}에게 1:1 메시지 전송 (보낸 사람: {}, 채팅방: {}): {}",
                receiverId,
                savedMessage.getSender().getId(),
                savedMessage.getChatRoom().getId(),
                savedMessage.getContent());
            messagingTemplate.convertAndSendToUser(
                receiverId.toString(),
                "/queue/messages",
                responseDto
            );
        } catch (Exception e) {
            log.error("1:1 메시지 전송 중 오류 발생: {}", e.getMessage(), e);
        }
    }

    @MessageMapping("/chat.typing.{roomId}")
    @SendTo("/topic/typing.{roomId}")
    public Map<String, Object> typing(@DestinationVariable("roomId") Long roomId, @Payload Map<String, Long> payload) {
        Long senderId = payload.get("senderId");
        if (senderId == null) {
            log.error("타이핑 이벤트 처리 실패: senderId가 없음, roomId={}", roomId);
            return Map.of("error", "Invalid senderId");
        }
        log.info("타이핑 이벤트 전송: roomId={}, senderId={}", roomId, senderId);
        return Map.of(
            "senderId", senderId,
            "roomId", roomId
        );
    }

    @MessageMapping("/chat.enterRoom.{roomId}")
    public void enterRoom(@DestinationVariable("roomId") Long roomId, @Payload Map<String, Object> payload) {
        Long userId = Long.valueOf(payload.get("userId").toString());
        log.info("사용자 {}가 채팅방 {}에 입장", userId, roomId);

        chatService.markAllMessagesAsRead(roomId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("type", "READ_UPDATE");

        log.info("WebSocket으로 READ_UPDATE 전송: roomId={}, userId={}", roomId, userId);
        messagingTemplate.convertAndSend("/topic/chat." + roomId, response);

        ChatRoom chatRoom = chatService.getChatRoom(roomId);
        List<Long> participantIds = chatRoom.getParticipantsAsUsers().stream()
            .map(User::getId)
            .collect(Collectors.toList());
        for (Long participantId : participantIds) {
            log.info("채팅방 목록 업데이트 이벤트 전송: /topic/chatrooms.{}", participantId);
            messagingTemplate.convertAndSend(
                "/topic/chatrooms." + participantId,
                Map.of("chatRoomId", chatRoom.getId())
            );
        }
    }
}