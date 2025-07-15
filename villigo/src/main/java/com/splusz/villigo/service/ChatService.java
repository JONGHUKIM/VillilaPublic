package com.splusz.villigo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splusz.villigo.config.WebSocketEventListener;
import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatMessageReadBy;
import com.splusz.villigo.domain.ChatMessageReadById;
import com.splusz.villigo.domain.ChatMessageType;
import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.ChatRoomParticipant;
import com.splusz.villigo.domain.ChatRoomReservation;
import com.splusz.villigo.domain.DuplicateChatRoomException;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ChatMessageDto;
import com.splusz.villigo.dto.ChatRoomDto;
import com.splusz.villigo.dto.ReservationDto;
import com.splusz.villigo.repository.ChatMessageReadByRepository;
import com.splusz.villigo.repository.ChatMessageRepository;
import com.splusz.villigo.repository.ChatRoomParticipantRepository;
import com.splusz.villigo.repository.ChatRoomRepository;
import com.splusz.villigo.repository.ChatRoomReservationRepository;
import com.splusz.villigo.repository.UserRepository;
import com.splusz.villigo.storage.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

	private final ChatMessageRepository chatMessageRepo;
    private final ChatMessageReadByRepository chatMessageReadByRepo;
    private final ChatRoomRepository chatRoomRepo;
    private final ChatRoomReservationRepository chatRoomReservationRepo;
    private final ReservationService reservationService;
    private final UserService userService;
    private final UserRepository userRepo;
    private final ChatRoomParticipantRepository chatRoomParticipantRepo;
    private final SimpMessagingTemplate messagingTemplate;
    private final S3FileStorageService s3FileStorageService; // <--- S3FileStorageService 주입!
    private final ObjectMapper objectMapper; // JSON 처리

    @Transactional(readOnly = true)
    public ChatRoom getChatRoom(Long roomId) {
        return chatRoomRepo.findByIdWithParticipants(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
    }
    
    public void addParticipant(ChatRoom chatRoom, User user) {
        if (!chatRoomParticipantRepo.existsByChatRoomAndUser(chatRoom, user)) {
            ChatRoomParticipant participant = new ChatRoomParticipant();
            participant.setChatRoom(chatRoom);
            participant.setUser(user);
            chatRoomParticipantRepo.save(participant);
        }
    }
    
    @Transactional
    public ChatRoom getChatRoomByUsersAndProduct(long userId1, long userId2, long productId) {
        List<ChatRoom> chatRooms = chatRoomRepo.findChatRoomsByUsersAndProduct(userId1, userId2, productId);
        if (chatRooms.size() == 1) {
            return chatRooms.get(0);
        } else if (chatRooms.isEmpty()) {
            // 사용자 정보 조회
            User user1 = userRepo.findById(userId1).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId1));
            User user2 = userRepo.findById(userId2).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId2));

            // 새로운 ChatRoom 생성
            ChatRoom newChatRoom = new ChatRoom();
            newChatRoom.setName(user1.getUsername() + ", " + user2.getUsername());
            newChatRoom.setStatus("ACTIVE");
            chatRoomRepo.save(newChatRoom);

            // 참가자 추가
            addParticipant(newChatRoom, user1);
            addParticipant(newChatRoom, user2);
            
            // WebSocket으로 채팅방 생성 알림 전송
            notifyChatRoomUpdate(userId1, newChatRoom.getId());
            notifyChatRoomUpdate(userId2, newChatRoom.getId());

            // 예약 연결 (필요 시)
            Reservation reservation = reservationService.findByProductId(productId); 
            if (reservation != null) {
                ChatRoomReservation crr = new ChatRoomReservation(newChatRoom, reservation);
                chatRoomReservationRepo.save(crr);
            }

            return newChatRoom;
        } else {
            throw new DuplicateChatRoomException("같은 사용자 두 명과 같은 상품에 대한 채팅방이 여러 개 발견됨");
        }
    }
    
    private void notifyChatRoomUpdate(Long userId, Long chatRoomId) {
        messagingTemplate.convertAndSend(
            "/topic/chatrooms." + userId,
            Map.of("chatRoomId", chatRoomId, "action", "create")
        );
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ChatRoom createChatRoom(Long userId1, Long userId2, Reservation reservation) {
        User user1 = userRepo.findById(userId1)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId1));
        User user2 = userRepo.findById(userId2)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId2));

        // 기존 채팅방 확인
        ChatRoom existingChatRoom = findChatRoomByParticipants(userId1, userId2);
        if (existingChatRoom != null) {
            // 기존 채팅방에 예약 추가
            ChatRoomReservation crr = new ChatRoomReservation(existingChatRoom, reservation);
            chatRoomReservationRepo.save(crr);
            log.info("기존 채팅방에 예약 추가: ID={}, reservationId={}", existingChatRoom.getId(), reservation.getId());

            // 참가자 처리 (기존 로직 유지)
            handleParticipants(existingChatRoom, user1, userId1, user2, userId2);
            return existingChatRoom;
        }

        // 새 채팅방 생성
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(user1.getUsername() + ", " + user2.getUsername());
        chatRoom.setStatus("ACTIVE");
        
        chatRoomRepo.save(chatRoom);

        // 참가자 추가
        chatRoom.addParticipant(user1);
        chatRoom.addParticipant(user2);
        chatRoomRepo.save(chatRoom);

        // 예약 연결
        ChatRoomReservation crr = new ChatRoomReservation(chatRoom, reservation);
        chatRoomReservationRepo.save(crr);

        log.info("새 채팅방 생성: ID={}, reservationId={}", chatRoom.getId(), reservation.getId());
        return chatRoom;
    }
    
    private void handleParticipants(ChatRoom chatRoom, User user1, Long userId1, User user2, Long userId2) {
        ChatRoomParticipant participant1 = chatRoomParticipantRepo
                .findByRoomIdAndUserId(chatRoom.getId(), userId1)
                .orElseGet(() -> {
                    ChatRoomParticipant newParticipant = new ChatRoomParticipant(chatRoom, user1);
                    chatRoomParticipantRepo.save(newParticipant);
                    return newParticipant;
                });
        ChatRoomParticipant participant2 = chatRoomParticipantRepo
                .findByRoomIdAndUserId(chatRoom.getId(), userId2)
                .orElseGet(() -> {
                    ChatRoomParticipant newParticipant = new ChatRoomParticipant(chatRoom, user2);
                    chatRoomParticipantRepo.save(newParticipant);
                    return newParticipant;
                });

        if (participant1.isLeft()) {
            participant1.rejoin();
            chatRoomParticipantRepo.save(participant1);
            log.info("사용자 {}가 채팅방 {}에 재입장", userId1, chatRoom.getId());
        }
        if (participant2.isLeft()) {
            participant2.rejoin();
            chatRoomParticipantRepo.save(participant2);
            log.info("사용자 {}가 채팅방 {}에 재입장", userId2, chatRoom.getId());
        }
    } 
 
    @Transactional(readOnly = true)
    public ChatRoom findExistingChatRoomByReservation(Long reservationId) {
        return chatRoomRepo.findByReservationId(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("예약 ID " + reservationId + "에 해당하는 채팅방을 찾을 수 없습니다."));
    }
    
    @Transactional
    public ChatRoom createChatRoom(Long userId1, Long userId2) {
        User user1 = userRepo.findById(userId1)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId1));
        User user2 = userRepo.findById(userId2)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId2));

        ChatRoom existingChatRoom = findExistingChatRoom(userId1, userId2);
        if (existingChatRoom != null) {
            log.info("기존 채팅방 반환: {}", existingChatRoom.getId());
            return existingChatRoom;
        }

        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setName(user1.getUsername() + ", " + user2.getUsername());
        chatRoom.setStatus("ACTIVE");

        chatRoomRepo.save(chatRoom);

        chatRoom.addParticipant(user1);
        chatRoom.addParticipant(user2);

        chatRoomRepo.save(chatRoom);
        log.info("새 채팅방 생성: ID={}", chatRoom.getId());
        return chatRoom;
    }

    @Transactional
    public ChatMessage saveMessage(ChatMessageDto messageDto) {
        ChatRoom chatRoom = chatRoomRepo.findById(messageDto.getChatRoomId())
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + messageDto.getChatRoomId()));

        User sender = userRepo.findById(messageDto.getSenderId())
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + messageDto.getSenderId()));

        ChatRoomParticipant senderParticipant = chatRoomParticipantRepo
            .findByRoomIdAndUserId(chatRoom.getId(), sender.getId())
            .orElseThrow(() -> new IllegalArgumentException("사용자가 채팅방에 없습니다."));

        boolean wasSenderLeft = senderParticipant.isLeft();
        if (wasSenderLeft) {
            senderParticipant.rejoin();
            chatRoomParticipantRepo.save(senderParticipant);
            log.info("사용자 {}가 메시지를 보내면서 채팅방 {}에 재참여", sender.getId(), chatRoom.getId());

            String content = sender.getUsername() + "님이 입장했습니다.";
            ChatMessage entryMessage = new ChatMessage(sender, chatRoom, content);
            messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
        }

        Long receiverId = getReceiverId(chatRoom, sender.getId());

        ChatRoomParticipant receiverParticipant = chatRoomParticipantRepo
            .findByRoomIdAndUserId(chatRoom.getId(), receiverId)
            .orElseGet(() -> {
                User receiver = userRepo.findById(receiverId)
                    .orElseThrow(() -> new IllegalArgumentException("수신자를 찾을 수 없습니다."));
                ChatRoomParticipant newParticipant = new ChatRoomParticipant(chatRoom, receiver);
                chatRoomParticipantRepo.save(newParticipant);
                log.info(" 수신자 {}가 채팅방 {}에 새로 참여됨", receiverId, chatRoom.getId());
                return newParticipant;
            });

        boolean wasReceiverLeft = receiverParticipant.isLeft();
        if (wasReceiverLeft) {
            receiverParticipant.rejoin();
            chatRoomParticipantRepo.save(receiverParticipant);
            log.info("사용자 {}가 메시지를 받으면서 채팅방 {}에 재참여", receiverId, chatRoom.getId());

            User receiver = userRepo.findById(receiverId).orElseThrow();
            String content = receiver.getUsername() + "님이 입장했습니다.";
            ChatMessage entryMessage = new ChatMessage(receiver, chatRoom, content);
            messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
        }

        if (wasSenderLeft || wasReceiverLeft) {
            chatRoom.setStatus("ACTIVE");
            chatRoomRepo.save(chatRoom);
            log.info("채팅방 {}의 상태가 ACTIVE로 변경됨", chatRoom.getId());
        }

        ChatMessage existingMessage = findExistingMessage(
            messageDto.getChatRoomId(),
            messageDto.getSenderId(),
            messageDto.getContent(),
            messageDto.getCreatedAt() != null ? messageDto.getCreatedAt() : LocalDateTime.now()
        );
        if (existingMessage != null) {
            log.info("중복 메시지 발견, 저장하지 않음: messageId={}", existingMessage.getId());
            return existingMessage;
        }

        ChatMessage message = new ChatMessage();
        message.setChatRoom(chatRoom);
        message.setSender(sender);

        try {
            message.setMessageType(messageDto.getMessageType());
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 메시지 타입: {}", messageDto.getMessageType());
            throw new IllegalArgumentException("유효하지 않은 메시지 타입: " + messageDto.getMessageType());
        }
        
        // 프론트엔드에서 S3에 직접 업로드 후 S3 Key(들)을 content에 담아 보냈다고 가정
        // 프론트엔드에서 S3에 직접 업로드 후 S3 Key(들)을 'content'에 담아 보냈다고 가정
        if (messageDto.getMessageType() == ChatMessageType.IMAGE || messageDto.getMessageType() == ChatMessageType.IMAGE_GROUP) {
            message.setContent(messageDto.getContent()); // S3 Key(들)을 그대로 저장
            log.info("S3 이미지 메시지 저장: chatRoom={} | sender={} | S3 Key(s)={}",
                    chatRoom.getId(), sender.getId(), messageDto.getContent());
        } else {
            message.setContent(messageDto.getContent());
        }

        message.setCreatedTime(messageDto.getCreatedAt() != null ? messageDto.getCreatedAt() : LocalDateTime.now());
        ChatMessage savedMessage = chatMessageRepo.save(message);
        log.info("채팅 메시지 저장: 채팅방={} | 보낸 사람={} | 내용={}",
            chatRoom.getId(), sender.getId(), message.getContent());

        for (User user : chatRoom.getParticipantsAsUsers()) {
            ChatMessageReadBy readBy = new ChatMessageReadBy(savedMessage, user);
            readBy.setRead(user.getId().equals(sender.getId()));
            readBy.setReadTime(user.getId().equals(sender.getId()) ? LocalDateTime.now() : null);
            chatMessageReadByRepo.save(readBy);
        }

        return savedMessage;
    }


    @Transactional(readOnly = true)
    public List<ChatRoomDto> getUserChatRooms(Long userId) {
        List<ChatRoom> chatRooms = chatRoomParticipantRepo.findActiveChatRoomsByUserId(userId);
        return chatRooms.stream()
            .map(chatRoom -> convertToChatRoomDto(chatRoom, userId)) // convertToChatRoomDto 메서드 재사용
            .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<ChatMessageDto> getChatMessages(Long chatRoomId, Long currentUserId) {
        List<ChatMessage> messages = chatMessageRepo.findByChatRoom_IdOrderByCreatedTimeAsc(chatRoomId);
        return messages.stream().map(message -> convertToDto(message, currentUserId))
            .collect(Collectors.toList());
    }

    public ChatMessageDto convertToDto(ChatMessage message, Long currentUserId) {
        String senderName = userRepo.findById(message.getSenderId())
            .map(user -> {
                String nickname = user.getNickname();
                String username = user.getUsername();
                if (nickname != null && !nickname.trim().isEmpty()) {
                    return nickname;
                } else if (username != null && !username.trim().isEmpty()) {
                    log.info("사용자 ID {}의 nickname이 비어 있어 username을 사용합니다: {}", message.getSenderId(), username);
                    return username;
                } else {
                    log.warn("사용자 ID {}의 nickname과 username이 모두 비어 있습니다. 기본 이름으로 설정합니다.", message.getSenderId());
                    return "사용자_" + message.getSenderId();
                }
            })
            .orElseGet(() -> {
                log.error("사용자 ID {}를 찾을 수 없습니다. 데이터베이스 상태를 확인하세요.", message.getSenderId());
                return "알 수 없는 사용자";
            });

        Long chatRoomId = message.getChatRoomId();
        List<Long> participantIds = chatRoomParticipantRepo.findParticipantIdsByChatRoomId(chatRoomId);

        Long otherParticipantId = participantIds.stream()
            .filter(id -> !id.equals(currentUserId))
            .findFirst()
            .orElse(null);

        String otherParticipantName = "알 수 없는 사용자";
        if (otherParticipantId != null) {
            otherParticipantName = userRepo.findById(otherParticipantId)
                .map(user -> {
                    String nickname = user.getNickname();
                    String username = user.getUsername();
                    if (nickname != null && !nickname.trim().isEmpty()) {
                        return nickname;
                    } else if (username != null && !username.trim().isEmpty()) {
                        log.info("사용자 ID {}의 nickname이 비어 있어 username을 사용합니다: {}", otherParticipantId, username);
                        return username;
                    } else {
                        log.warn("사용자 ID {}의 nickname과 username이 모두 비어 있습니다. 기본 이름으로 설정합니다.", otherParticipantId);
                        return "사용자_" + otherParticipantId;
                    }
                })
                .orElseGet(() -> {
                    log.error("사용자 ID {}를 찾을 수 없습니다. 데이터베이스 상태를 확인하세요.", otherParticipantId);
                    return "알 수 없는 사용자";
                });
        }

        Map<Long, Boolean> readByMap = chatMessageReadByRepo.findByChatMessageId(message.getId())
            .stream()
            .collect(Collectors.toMap(
                readBy -> readBy.getUser().getId(),
                ChatMessageReadBy::isRead,
                (oldValue, newValue) -> oldValue
            ));

        String contentToDisplay = message.getContent();
        
        // 이미지 메시지 CONTENT 변환 시작
        if (message.getMessageType() == ChatMessageType.IMAGE || message.getMessageType() == ChatMessageType.IMAGE_GROUP) {
            try {
                if (message.getMessageType() == ChatMessageType.IMAGE_GROUP) {
                    List<String> s3Keys = objectMapper.readValue(message.getContent(), List.class);
                    List<String> presignedUrls = s3Keys.stream()
                        .map(key -> {
                            try {
                                return s3FileStorageService.generateDownloadPresignedUrl(key, Duration.ofMinutes(5));
                            } catch (FileStorageException e) {
                                log.error("Failed to generate presigned URL for group image {}: {}", key, e.getMessage());
                                return "/images/error-image.png";
                            }
                        })
                        .collect(Collectors.toList());
                    contentToDisplay = objectMapper.writeValueAsString(presignedUrls);
                } else { // 단일 이미지
                    contentToDisplay = s3FileStorageService.generateDownloadPresignedUrl(message.getContent(), Duration.ofMinutes(5));
                }
            } catch (FileStorageException e) {
                log.error("Failed to generate presigned URL for chat image {}: {}", message.getContent(), e.getMessage());
                contentToDisplay = "/images/error-image.png";
            } catch (JsonProcessingException e) {
                log.error("Failed to parse IMAGE_GROUP content for chat message {}: {}", message.getId(), e.getMessage());
                contentToDisplay = "{\"error\": \"Invalid image group content\"}";
            }
        }

        return new ChatMessageDto(
            message.getId(),
            message.getChatRoomId(),
            message.getSenderId(),
            senderName,
            otherParticipantName,
            message.getMessageType(),
            contentToDisplay, // 변환된 content 사용
            message.getCreatedTime(),
            readByMap
        );
    }
    
    @Transactional(readOnly = true)
    public ChatRoom findChatRoomByParticipants(Long userId1, Long userId2) {
        ChatRoom chatRoom = chatRoomRepo.findExistingChatRoom(userId1, userId2).orElse(null);
        if (chatRoom != null) {
            log.info("기존 채팅방 조회 성공: ID={}", chatRoom.getId());
        }
        return chatRoom;
    }

    public Long getReceiverId(ChatRoom chatRoom, Long senderId) {
        List<User> participants = chatRoom.getParticipantsAsUsers();

        if (participants.size() != 2) {
            log.error("1:1 채팅방이 아닙니다. 참가자 수: {}", participants.size());
            throw new IllegalArgumentException("1:1 채팅방이 아닙니다.");
        }

        return participants.stream()
            .map(User::getId)
            .filter(id -> !id.equals(senderId))
            .findFirst()
            .orElseThrow(() -> {
                log.error("상대방을 찾을 수 없습니다. 채팅방: {} | 보낸 사람: {}", chatRoom.getId(), senderId);
                return new IllegalArgumentException("상대방을 찾을 수 없습니다.");
            });
    }

    @Transactional(readOnly = true)
    public ChatRoom findExistingChatRoom(Long userId1, Long userId2) {
        ChatRoom chatRoom = chatRoomRepo.findExistingChatRoom(userId1, userId2).orElse(null);
        if (chatRoom != null) {
            log.info("기존 채팅방 조회 성공: ID={}", chatRoom.getId());
        }
        return chatRoom;
    }

    @Transactional(readOnly = true)
    public ChatMessageDto getLastMessageFromReceiver(Long chatRoomId, Long userId) {
        List<ChatMessage> messages = chatMessageRepo.findLastMessageByChatRoomIdAndNotSenderId(chatRoomId, userId);
        if (messages.isEmpty()) {
            log.info("채팅방 {}에서 사용자 {}가 아닌 사용자가 보낸 메시지가 없음", chatRoomId, userId);
            return null;
        }
        ChatMessage lastMessage = messages.get(0);
        Hibernate.initialize(lastMessage.getSender());
        Hibernate.initialize(lastMessage.getChatRoom());
        return convertToDto(lastMessage, userId);
    }

    @Transactional(readOnly = true)
    public ChatMessageDto getLastMessage(Long chatRoomId, Long currentUserId) {
        List<ChatMessage> messages = chatMessageRepo.findByChatRoom_IdOrderByCreatedTimeDesc(chatRoomId);
        return messages.isEmpty() ? null : convertToDto(messages.get(0), currentUserId);
    }

    @Transactional(readOnly = true)
    public long countUnreadMessages(Long chatRoomId, Long userId) {
        return chatMessageReadByRepo.countUnreadMessagesByUserIdAndChatRoomId(userId, chatRoomId);
    }

    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        ChatMessage message = chatMessageRepo.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("메시지를 찾을 수 없습니다: " + messageId));

        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        ChatMessageReadById id = new ChatMessageReadById(messageId, userId);
        ChatMessageReadBy readBy = chatMessageReadByRepo.findById(id)
            .orElse(new ChatMessageReadBy(message, user));

        readBy.setRead(true);
        readBy.setReadTime(LocalDateTime.now());
        chatMessageReadByRepo.save(readBy);
        log.info("메시지 {}가 사용자 {}에 의해 읽음 처리됨", messageId, userId);
    }

    @Transactional
    public void deleteChatRoom(Long roomId) {
        ChatRoom chatRoom = chatRoomRepo.findByIdWithParticipants(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        long activeCount = chatRoomParticipantRepo.countActiveParticipants(roomId);
        log.info("채팅방 {}의 활성 참가자 수: {}", roomId, activeCount);

        if (activeCount > 0) {
            log.warn("채팅방 {}에 아직 {}명의 참가자가 남아 있습니다. 삭제 불가", roomId, activeCount);
            throw new IllegalStateException("모든 참가자가 나간 후에만 채팅방을 삭제할 수 있습니다.");
        }

        log.info("채팅방 {}의 모든 사용자가 나갔습니다. 채팅방 삭제 실행", roomId);
        chatRoom.setStatus("INACTIVE");
        chatRoomRepo.save(chatRoom);
        deleteChatRoomForce(roomId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageDto> getMessages(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepo.findByParticipantId(userId);
        if (chatRooms.isEmpty()) {
            log.warn("사용자 {}가 참여한 채팅방이 없음", userId);
            return List.of();
        }

        return chatRooms.stream()
            .flatMap(chatRoom -> chatMessageRepo.findByChatRoom_IdOrderByCreatedTimeAsc(chatRoom.getId()).stream())
            .map(message -> convertToDto(message, userId))
            .collect(Collectors.toList());
    }

    public ChatRoomDto getChatRoomDto(Long chatRoomId, Long currentUserId) {
        ChatRoom chatRoom = getChatRoom(chatRoomId);
        return convertToChatRoomDto(chatRoom, currentUserId);
    }

    private ChatRoomDto convertToChatRoomDto(ChatRoom chatRoom, Long currentUserId) {
        ChatRoomDto dto = new ChatRoomDto();
        dto.setId(chatRoom.getId());
        dto.setName(chatRoom.getName() != null ? chatRoom.getName() : "이름 없음");
        dto.setStatus(chatRoom.getStatus() != null ? chatRoom.getStatus() : "UNKNOWN");

        // ChatRoomReservation을 통해 연관된 예약 조회
        List<ReservationDto> reservationDtos = chatRoomReservationRepo.findByChatRoomId(chatRoom.getId())
            .stream()
            .map(crr -> {
                Reservation reservation = reservationService.findById(crr.getReservationId()).orElse(null);
                if (reservation == null) {
                    return null;
                }
                try {
                    String details = String.format("대여 기간: %s ~ %s, 상태: %d",
                        reservation.getStartTime() != null ? reservation.getStartTime().toLocalDate().toString() : "미정",
                        reservation.getEndTime() != null ? reservation.getEndTime().toLocalDate().toString() : "미정",
                        reservation.getStatus());
                    return ReservationDto.builder()
                        .id(reservation.getId())
                        .details(details)
                        .build();
                } catch (Exception e) {
                    log.error("예약 DTO 변환 중 오류: reservationId={}, error={}", reservation.getId(), e.getMessage());
                    return ReservationDto.builder()
                        .id(reservation.getId())
                        .details("예약 정보 오류")
                        .build();
                }
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        dto.setReservations(reservationDtos);

        ChatMessage lastMessage = chatMessageRepo.findTopByChatRoom_IdOrderByCreatedTimeDesc(chatRoom.getId());
        if (lastMessage != null) {
            dto.setLastMessage(lastMessage.getContent());
            dto.setLastMessageTime(lastMessage.getCreatedTime());
        }

        long unreadCount = chatMessageReadByRepo.countUnreadMessages(chatRoom.getId(), currentUserId);
        dto.setUnreadCount(unreadCount);

        ChatRoomParticipant otherParticipant = chatRoom.getParticipants() != null
                ? chatRoom.getParticipants().stream()
                    .filter(p -> p != null && p.getUser() != null && !p.getUser().getId().equals(currentUserId))
                    .findFirst()
                    .orElse(null)
                : null;
            if (otherParticipant != null) {
                User otherUser = otherParticipant.getUser();
                dto.setOtherUserId(otherUser.getId());
                dto.setOtherUserNickName(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
                if (dto.getOtherUserNickName() == null || dto.getOtherUserNickName().trim().isEmpty()) {
                    dto.setOtherUserNickName("사용자_" + otherUser.getId());
                } else if (dto.getOtherUserNickName().startsWith("탈퇴회원_")) {
                    dto.setOtherUserNickName("탈퇴회원");
                }

                String otherUserAvatarS3Key = otherUser.getAvatar(); // User 엔티티에서 S3 Key 가져옴
                if (StringUtils.hasText(otherUserAvatarS3Key)) { // S3 Key가 존재하면
                    try {
                        // S3 Pre-signed URL 생성 (5분 유효)
                        String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(otherUserAvatarS3Key, Duration.ofMinutes(5));
                        dto.setOtherUserAvatarImageUrl(presignedUrl); // <--- 이 부분에 Pre-signed URL 설정
                    } catch (FileStorageException e) {
                        log.error("Failed to generate presigned URL for other user avatar {}: {}", otherUserAvatarS3Key, e.getMessage());
                        dto.setOtherUserAvatarImageUrl("/images/default-avatar.png"); // 오류 시 기본 이미지 URL (클라이언트에서 처리할 경로)
                    }
                } else {
                    // 아바타 S3 Key가 없는 경우 (아바타 미설정)
                    dto.setOtherUserAvatarImageUrl("/images/default-avatar.png"); // 기본 이미지 URL 설정 (클라이언트에서 처리할 경로)
                }

                dto.setOtherUserIsOnline(WebSocketEventListener.isUserOnline(otherUser.getId()));
            }

            return dto;
        }

    @Transactional
    public void markAllMessagesAsRead(Long chatRoomId, Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        ChatRoom chatRoom = chatRoomRepo.findById(chatRoomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + chatRoomId));

        // 참여자 정보 조회 또는 생성
        Optional<ChatRoomParticipant> participantOpt = chatRoomParticipantRepo.findByRoomIdAndUserId(chatRoomId, userId);
        ChatRoomParticipant participant;
        if (participantOpt.isPresent()) {
            participant = participantOpt.get();
            if (participant.isLeft()) {
                participant.rejoin();
                chatRoomParticipantRepo.save(participant);
                log.info("사용자 {}가 채팅방 {}에 입장하면서 재참여", userId, chatRoomId);

                String content = user.getUsername() + "님이 입장했습니다.";
                ChatMessage entryMessage = new ChatMessage(user, chatRoom, content);
                messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
            }
        } else {
            // 참여 레코드가 없는 경우 새로 생성
            participant = new ChatRoomParticipant(chatRoom, user);
            chatRoomParticipantRepo.save(participant);
            log.info("사용자 {}가 채팅방 {}에 새로 참여", userId, chatRoomId);

            String content = user.getUsername() + "님이 입장했습니다.";
            ChatMessage entryMessage = new ChatMessage(user, chatRoom, content);
            messagingTemplate.convertAndSend("/topic/chat." + chatRoom.getId(), entryMessage);
        }

        long unreadCount = chatMessageReadByRepo.countUnreadMessagesByUserIdAndChatRoomId(userId, chatRoomId);
        if (unreadCount == 0) {
            log.info("채팅방 {}에서 사용자 {}가 읽지 않은 메시지가 없음", chatRoomId, userId);
            return;
        }

        chatMessageReadByRepo.markAllAsReadByUserIdAndChatRoomId(userId, chatRoomId, LocalDateTime.now());
        log.info("채팅방 {}에서 사용자 {}에 의해 {}개의 메시지가 읽음 처리됨", chatRoomId, userId, unreadCount);
    }

    @Transactional
    public void migrateReadByData() {
        List<ChatMessage> messages = chatMessageRepo.findAll();
        int totalMigrated = 0;

        for (ChatMessage message : messages) {
            Long messageId = message.getId();
            Long senderId = message.getSender().getId();
            ChatRoom chatRoom = message.getChatRoom();

            List<User> participants = chatRoom.getParticipantsAsUsers();
            if (participants.isEmpty()) {
                log.warn("메시지 {}의 채팅방 {}에 참가자가 없음", messageId, chatRoom.getId());
                continue;
            }

            for (User participant : participants) {
                Long userId = participant.getId();

                ChatMessageReadById id = new ChatMessageReadById(messageId, userId);
                if (chatMessageReadByRepo.existsById(id)) {
//                    log.info("메시지 {}의 사용자 {}에 대한 읽음 상태 이미 존재", messageId, userId);
                    continue;
                }

                ChatMessageReadBy readByEntity = new ChatMessageReadBy(message, participant);
                boolean isSender = userId.equals(senderId);
                readByEntity.setRead(isSender);
                readByEntity.setReadTime(isSender ? LocalDateTime.now() : null);
                chatMessageReadByRepo.save(readByEntity);
                totalMigrated++;
                log.info("메시지 {}의 사용자 {} 읽음 상태 마이그레이션 완료 (isRead: {})", messageId, userId, isSender);
            }
        }

        log.info("총 {}개의 읽음 상태 데이터 마이그레이션 완료", totalMigrated);
    }

    @Transactional(readOnly = true)
    public ChatMessage findExistingMessage(Long chatRoomId, Long senderId, String content, LocalDateTime createdTime) {
        if (createdTime == null) {
            log.warn("createdTime이 null입니다. 중복 메시지 확인 불가.");
            return null;
        }

        LocalDateTime startTime = createdTime.minus(1, ChronoUnit.SECONDS);
        LocalDateTime endTime = createdTime.plus(1, ChronoUnit.SECONDS);

        log.info("중복 메시지 확인: chatRoomId={}, senderId={}, content={}, createdTime={} (범위: {} ~ {})",
                chatRoomId, senderId, content, createdTime, startTime, endTime);

        List<ChatMessage> messages = chatMessageRepo.findByChatRoomIdAndSenderIdAndContentAndCreatedTimeBetween(
            chatRoomId, senderId, content, startTime, endTime
        );

        if (!messages.isEmpty()) {
            log.info("중복 메시지 발견: messageId={}", messages.get(0).getId());
        } else {
            log.info("중복 메시지 없음");
        }

        return messages.isEmpty() ? null : messages.get(0);
    }

    @Transactional
    public boolean leaveChatRoom(Long roomId, Long userId) {
        ChatRoomParticipant participant = chatRoomParticipantRepo
            .findByRoomIdAndUserId(roomId, userId)
            .orElseThrow(() -> new IllegalArgumentException("참가자 정보를 찾을 수 없습니다."));
        
        participant.markAsLeft();
        chatRoomParticipantRepo.markParticipantAsLeft(roomId, userId, participant.getLeftAt());
        log.info("사용자 {}가 채팅방 {}에서 나감 처리됨, leftAt: {}", userId, roomId, participant.getLeftAt());

        ChatRoom chatRoom = chatRoomRepo.findByIdWithParticipants(roomId)
            .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다: " + roomId));

        long activeCount = chatRoomParticipantRepo.countActiveParticipants(roomId);
        log.info("채팅방 {}의 활성 참가자 수: {}", roomId, activeCount);

        boolean isDeleted = false;
        if (activeCount == 0) {
            log.info("채팅방 {}의 모든 사용자가 나갔습니다. 메시지 및 방 자동 삭제 실행", roomId);
            chatRoom.setStatus("INACTIVE");
            chatRoomRepo.save(chatRoom);
            deleteChatRoomForce(roomId);
            isDeleted = true;
        } else {
            log.info("채팅방 {}에 아직 {}명의 참가자가 남아 있습니다. 채팅방 유지", roomId, activeCount);
        }

        Long otherUserId = getReceiverId(chatRoom, userId);
        messagingTemplate.convertAndSend(
            "/topic/chatrooms." + otherUserId,
            Map.of("chatRoomId", roomId, "action", "leave", "userId", userId, "isDeleted", isDeleted)
        );
        messagingTemplate.convertAndSend(
            "/topic/chatrooms." + userId,
            Map.of("chatRoomId", roomId, "action", "leave", "userId", userId, "isDeleted", isDeleted)
        );

        return isDeleted;
    }
    
    @Transactional(readOnly = true)
    public long countAllUnreadMessages(Long userId) {
        long count = chatMessageReadByRepo.countUnreadMessagesByUserId(userId);
        log.info("사용자 {}의 전체 안읽은 메시지 수: {}", userId, count);
        return count;
    }


    @Transactional
    public void deleteChatRoomForce(Long roomId) {
        chatMessageReadByRepo.deleteByChatRoomId(roomId);
        chatMessageRepo.deleteByChatRoomId(roomId);
        chatRoomReservationRepo.deleteByChatRoomId(roomId);
        // chatRoomParticipantRepo.deleteByChatRoomId(roomId);
        chatRoomRepo.deleteById(roomId);
        log.info("채팅방 {} 및 관련 데이터 완전 삭제", roomId);
    }
}