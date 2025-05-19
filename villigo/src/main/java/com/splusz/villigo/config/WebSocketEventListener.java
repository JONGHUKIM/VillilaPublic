package com.splusz.villigo.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
	
	private final UserRepository userRepo;
	private final SimpMessagingTemplate messagingTemplate;

    // 사용자 온라인 상태를 저장하는 HashMap (사용자별 세션 ID 집합)
    private static final ConcurrentHashMap<Long, Set<String>> onlineUsers = new ConcurrentHashMap<>();

    @EventListener
    @Transactional(readOnly = true)
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes != null) {
            String userIdStr = (String) headerAccessor.getFirstNativeHeader("userId");
            if (userIdStr != null) {
                Long userId = Long.valueOf(userIdStr);
                onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(headerAccessor.getSessionId());
                System.out.println("사용자 접속: " + userId + ", 세션 ID: " + headerAccessor.getSessionId());

                // 온라인 상태 변경 이벤트 전송
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("userId", userId);
                statusUpdate.put("isOnline", true);
                messagingTemplate.convertAndSend("/topic/userStatus", statusUpdate);
            }
        }
    }

    @EventListener
    @Transactional(readOnly = true)
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        onlineUsers.entrySet().removeIf(entry -> {
            Set<String> sessions = entry.getValue();
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                Long userId = entry.getKey();
                System.out.println("사용자 접속 해제: " + userId + ", 세션 ID: " + sessionId);

                // 오프라인 상태 변경 이벤트 전송
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("userId", userId);
                statusUpdate.put("isOnline", false);
                messagingTemplate.convertAndSend("/topic/userStatus", statusUpdate);
                return true;
            }
            return false;
        });
    }

    // ✅ 사용자 온라인 여부 조회 메서드 (REST API에서 호출 가능)
    public static boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId) && !onlineUsers.get(userId).isEmpty();
    }

    // ✅ 현재 온라인 상태인 사용자 목록 반환
    public static Map<Long, Set<String>> getOnlineUsers() {
        return onlineUsers;
    }
}