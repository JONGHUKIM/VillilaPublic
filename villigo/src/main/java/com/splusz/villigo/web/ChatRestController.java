package com.splusz.villigo.web;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.config.WebSocketEventListener;
import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.ChatRoomParticipant;
import com.splusz.villigo.domain.ChatRoomReservation;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ChatMessageDto;
import com.splusz.villigo.dto.ChatRoomDto;
import com.splusz.villigo.repository.ChatRoomParticipantRepository;
import com.splusz.villigo.repository.ChatRoomRepository;
import com.splusz.villigo.repository.ChatRoomReservationRepository;
import com.splusz.villigo.service.ChatService;
import com.splusz.villigo.service.ReservationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatRestController {

	private final ChatService chatService;
    private final ReservationService reservationService;
    private final ChatRoomParticipantRepository chatRoomParticipantRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepo;
    private final ChatRoomReservationRepository chatRoomReservationRepo;

    @PostMapping("/rooms")
    public synchronized ResponseEntity<ChatRoomDto> createChatRoom(
        @RequestParam(name = "userId1") Long userId1, 
        @RequestParam(name = "userId2") Long userId2) {
        ChatRoom existingChatRoom = chatService.findExistingChatRoom(userId1, userId2);

        if (existingChatRoom != null) {
            Long currentUserId = userId1;
            ChatRoomDto chatRoomDto = chatService.getChatRoomDto(existingChatRoom.getId(), currentUserId);
            log.info("이미 존재하는 채팅방 반환: {}", chatRoomDto.getId());
            return ResponseEntity.ok(chatRoomDto);
        }

        ChatRoom chatRoom = chatService.createChatRoom(userId1, userId2);
        ChatRoomDto chatRoomDto = chatService.getChatRoomDto(chatRoom.getId(), userId1);
        log.info("새 채팅방 생성: {}", chatRoomDto.getId());
        return ResponseEntity.ok(chatRoomDto);
    }
    
    @GetMapping("/rooms/find")
    public ResponseEntity<ChatRoomDto> findChatRoomBetweenUsers(
        @RequestParam(name = "userId1") Long userId1,
        @RequestParam(name = "userId2") Long userId2
    ) {
        ChatRoom room = chatService.findExistingChatRoom(userId1, userId2);
        if (room != null) {
            ChatRoomDto dto = chatService.getChatRoomDto(room.getId(), userId1);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.notFound().build();
    }

    
    @PostMapping("/rooms/by-reservation")
    public ResponseEntity<ChatRoomDto> createChatRoomReservation(
            @RequestParam(name = "reservationId") Long reservationId,
            @RequestParam(name = "currentUserId") Long currentUserId) {
        Reservation reservation = reservationService.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예약입니다: " + reservationId));
        
        Long requesterId = reservation.getRenter().getId();
        Long ownerId = reservation.getProduct().getUser().getId();
        
        if (!currentUserId.equals(requesterId) && !currentUserId.equals(ownerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        
        // 두 사용자 간의 기존 채팅방 찾기
        ChatRoom existingChatRoom = chatService.findChatRoomByParticipants(requesterId, ownerId);
        if (existingChatRoom != null) {
            // 채팅방과 예약의 관계 추가
            ChatRoomReservation chatRoomReservation = new ChatRoomReservation(existingChatRoom, reservation);
            chatRoomReservationRepo.save(chatRoomReservation);
            log.info("기존 채팅방에 예약 연결: chatRoomId={}, reservationId={}", existingChatRoom.getId(), reservation.getId());
            
            ChatRoomDto chatRoomDto = chatService.getChatRoomDto(existingChatRoom.getId(), currentUserId);
            return ResponseEntity.ok(chatRoomDto);
        }

        // 새로운 채팅방 생성
        ChatRoom chatRoom = chatService.createChatRoom(requesterId, ownerId);
        ChatRoomReservation chatRoomReservation = new ChatRoomReservation(chatRoom, reservation);
        chatRoomReservationRepo.save(chatRoomReservation);
        log.info("새 채팅방 생성 및 예약 연결: chatRoomId={}, reservationId={}", chatRoom.getId(), reservationId);
        
        ChatRoomDto chatRoomDto = chatService.getChatRoomDto(chatRoom.getId(), currentUserId);
        return ResponseEntity.ok(chatRoomDto);
    }


    @GetMapping("/rooms/user/{userId}")
    public ResponseEntity<List<ChatRoomDto>> getUserChatRooms(@PathVariable(name = "userId") Long userId) {
    	log.info("사용자 {}의 채팅방 목록 조회 시작", userId);
        List<ChatRoomDto> chatRooms = chatService.getUserChatRooms(userId);
        log.info("조회된 채팅방 수: {}", chatRooms.size());
        chatRooms.forEach(room -> log.info("채팅방 ID: {}, otherUserName: {}", room.getId(), room.getOtherUserNickName()));
        return ResponseEntity.ok(chatRooms);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteChatRoom(
            @PathVariable(name = "roomId") Long roomId,
            @RequestParam(name = "userId") Long userId) { // userId 추가
        log.info("채팅방 나가기 요청: roomId={}, userId={}", roomId, userId);
        chatService.leaveChatRoom(roomId, userId);
        log.info("사용자 {}가 채팅방 {}에서 나감", userId, roomId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/rooms/{id}")
    public ResponseEntity<ChatRoomDto> getChatRoom(
            @PathVariable(name = "id") Long chatRoomId,
            @RequestParam(name = "currentUserId") Long currentUserId) {
        ChatRoomDto chatRoom = chatService.getChatRoomDto(chatRoomId, currentUserId);
        return ResponseEntity.ok(chatRoom);
    }
    
    @PostMapping("/rooms/{chatRoomId}/rejoin")
    public ResponseEntity<String> rejoinChatRoom(
            @PathVariable Long chatRoomId,
            @RequestParam Long currentUserId) {
        User user = chatService.getUser(currentUserId);
        ChatRoom chatRoom = chatService.getChatRoom(chatRoomId);

        Optional<ChatRoomParticipant> participantOpt = chatRoomParticipantRepo.findByRoomIdAndUserId(chatRoomId, currentUserId);
        ChatRoomParticipant participant;
        if (participantOpt.isPresent()) {
            participant = participantOpt.get();
            if (participant.isLeft()) {
                participant.rejoin();
                chatRoomParticipantRepo.save(participant);
                log.info("사용자 {}가 채팅방 {}에 재참여", currentUserId, chatRoomId);

                String content = user.getUsername() + "님이 입장했습니다.";
                ChatMessage entryMessage = new ChatMessage(user, chatRoom, content);
                messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
            } else { 
                return ResponseEntity.badRequest().body("이미 채팅방에 참여 중입니다.");
            }
        } else {
            participant = new ChatRoomParticipant(chatRoom, user);
            chatRoomParticipantRepo.save(participant);
            log.info("사용자 {}가 채팅방 {}에 새로 참여", currentUserId, chatRoomId);

            String content = user.getUsername() + "님이 입장했습니다.";
            ChatMessage entryMessage = new ChatMessage(user, chatRoom, content);
            messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
        }

        return ResponseEntity.ok("채팅방에 재참여했습니다.");
    }

    @GetMapping("/rooms/{roomId}/messages")
    public ResponseEntity<List<ChatMessageDto>> getChatMessages(
        @PathVariable(name = "roomId") Long roomId,
        @RequestParam(name = "currentUserId") Long currentUserId) {
        chatService.markAllMessagesAsRead(roomId, currentUserId);

        List<ChatMessageDto> messages = chatService.getChatMessages(roomId, currentUserId);
        messages.forEach(message -> log.info("메시지 ID: {}, senderName: {}", message.getId(), message.getSenderName()));
        if (messages.isEmpty()) {
            log.warn("채팅방 {}에 저장된 메시지가 없음", roomId);
        } else {
            log.info("채팅방 {}의 메시지 개수: {}개", roomId, messages.size());
        }
        return ResponseEntity.ok(messages.isEmpty() ? List.of() : messages);
    }
    
    @PostMapping("/messages/{messageId}/read")
    public ResponseEntity<Void> markMessageAsRead(
        @PathVariable(name = "messageId") Long messageId,
        @RequestParam(name = "userId") Long userId) {
        log.info("메시지 {} 읽음 처리 요청 (사용자: {})", messageId, userId);
        chatService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/user/{userId}/online")
    public ResponseEntity<Boolean> isUserOnline(@PathVariable(name = "userId") Long userId) {
        boolean isOnline = WebSocketEventListener.isUserOnline(userId);
        return ResponseEntity.ok(isOnline);
    }

    // ChatController에서 이동한 메서드
    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageDto>> getUserMessages(@RequestParam(name = "userId") Long userId) {
        List<ChatMessageDto> messages = chatService.getMessages(userId);
        return ResponseEntity.ok(messages);
    }
}