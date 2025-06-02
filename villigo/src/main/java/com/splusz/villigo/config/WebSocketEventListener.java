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

/**
 * WebSocket 연결/해제 이벤트를 처리하는 리스너 클래스
 * 
 * @Component: Spring Bean으로 등록하여 자동으로 인스턴스 생성 및 관리
 * @RequiredArgsConstructor: Lombok 어노테이션으로 final 필드에 대한 생성자 자동 생성
 */
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
	
	// 의존성 주입을 통해 사용할 Repository와 MessagingTemplate
	// final로 선언하여 RequiredArgsConstructor에 의해 생성자에서 초기화됨
	private final UserRepository userRepo;           // 사용자 정보 데이터베이스 조회용
	private final SimpMessagingTemplate messagingTemplate;  // WebSocket 메시지 전송용
    
    /**
     * 사용자 온라인 상태를 저장하는 ConcurrentHashMap
     * Key: 사용자 ID (Long 타입)
     * Value: 해당 사용자의 세션 ID 집합 (Set<String>)
     * 
     * 왜 Set을 사용하는가?
     * - 한 사용자가 여러 탭/브라우저로 동시 접속할 수 있기 때문
     * - 각 접속마다 다른 세션 ID가 생성되므로 Set으로 관리
     * 
     * static 사용 이유: 애플리케이션 전체에서 공유되는 단일 상태 관리
     * ConcurrentHashMap 사용 이유: 멀티스레드 환경에서 안전한 동시 접근
     */
    private static final ConcurrentHashMap<Long, Set<String>> onlineUsers = new ConcurrentHashMap<>();
    
    /**
     * WebSocket 연결 이벤트 리스너
     * 사용자가 WebSocket에 연결할 때 자동으로 호출됨
     * 
     * @EventListener: Spring의 이벤트 시스템을 통해 SessionConnectEvent 발생 시 자동 호출
     * @Transactional(readOnly = true): 읽기 전용 트랜잭션 (데이터 조회만 수행)
     */
    @EventListener
    @Transactional(readOnly = true)
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        // 이벤트에서 STOMP 헤더 정보를 추출하기 위한 accessor 생성
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // WebSocket 세션 속성 정보 가져오기
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        
        // 세션 속성이 존재하는지 확인 (null 체크)
        if (sessionAttributes != null) {
            // 클라이언트가 연결 시 헤더로 전송한 "userId" 추출
            String userIdStr = (String) headerAccessor.getFirstNativeHeader("userId");
            
            // userId가 존재하는지 확인
            if (userIdStr != null) {
                // 문자열로 받은 userId를 Long 타입으로 변환
                Long userId = Long.valueOf(userIdStr);
                
                /**
                 * computeIfAbsent 메소드 동작 설명:
                 * - userId가 onlineUsers에 없으면 새로운 ConcurrentHashMap.newKeySet() 생성
                 * - 이미 있으면 기존 Set 반환
                 * - 반환된 Set에 현재 세션 ID 추가
                 * 
                 * 결과: 한 사용자의 여러 세션을 안전하게 관리
                 */
                onlineUsers.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet())
                          .add(headerAccessor.getSessionId());
                
                // 콘솔에 접속 로그 출력 (디버깅 및 모니터링용)
                System.out.println("사용자 접속: " + userId + ", 세션 ID: " + headerAccessor.getSessionId());
                
                // 다른 사용자들에게 온라인 상태 변경 알림 전송
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("userId", userId);      // 접속한 사용자 ID
                statusUpdate.put("isOnline", true);      // 온라인 상태임을 표시
                
                // "/topic/userStatus"를 구독하고 있는 모든 클라이언트에게 브로드캐스트
                // 실시간으로 사용자 온라인 상태를 다른 사용자들이 볼 수 있게 함
                messagingTemplate.convertAndSend("/topic/userStatus", statusUpdate);
            }
        }
    }
    
    /**
     * WebSocket 연결 해제 이벤트 리스너
     * 사용자가 WebSocket 연결을 끊을 때 자동으로 호출됨
     * (브라우저 종료, 탭 닫기, 네트워크 끊김 등)
     * 
     * @EventListener: SessionDisconnectEvent 발생 시 자동 호출
     * @Transactional(readOnly = true): 읽기 전용 트랜잭션
     */
    @EventListener
    @Transactional(readOnly = true)
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        // 연결 해제 이벤트에서 헤더 정보 추출
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        
        // 연결 해제된 세션의 ID 가져오기
        String sessionId = headerAccessor.getSessionId();
        
        /**
         * removeIf 메소드를 사용한 복잡한 로직 설명:
         * 
         * 1. onlineUsers의 모든 항목(entry)을 순회
         * 2. 각 항목의 세션 Set에서 해제된 sessionId 제거
         * 3. 세션 Set이 비어있다면 해당 사용자는 완전히 오프라인
         * 4. 오프라인된 사용자에 대해 상태 알림 전송 후 항목 제거
         * 5. 세션이 남아있다면 다른 탭/브라우저로 여전히 접속 중이므로 유지
         */
        onlineUsers.entrySet().removeIf(entry -> {
            Set<String> sessions = entry.getValue();  // 해당 사용자의 세션 ID 집합
            sessions.remove(sessionId);               // 연결 해제된 세션 ID 제거
            
            // 모든 세션이 제거되었다면 (완전 오프라인)
            if (sessions.isEmpty()) {
                Long userId = entry.getKey();
                
                // 콘솔에 접속 해제 로그 출력
                System.out.println("사용자 접속 해제: " + userId + ", 세션 ID: " + sessionId);
                
                // 다른 사용자들에게 오프라인 상태 변경 알림 전송
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("userId", userId);      // 오프라인된 사용자 ID
                statusUpdate.put("isOnline", false);     // 오프라인 상태임을 표시
                
                // 상태 변경을 구독자들에게 브로드캐스트
                messagingTemplate.convertAndSend("/topic/userStatus", statusUpdate);
                
                // true 반환: 이 항목을 onlineUsers에서 완전히 제거
                return true;
            }
            
            // false 반환: 다른 세션이 남아있으므로 항목 유지
            return false;
        });
    }
    
    /**
     * 특정 사용자의 온라인 상태를 확인하는 정적 메소드
     * REST API나 다른 서비스에서 호출하여 사용자 온라인 상태 조회 가능
     * 
     * @param userId 확인하고 싶은 사용자의 ID
     * @return 온라인 상태 (true: 온라인, false: 오프라인)
     * 
     * 동작 원리:
     * 1. onlineUsers에 해당 userId가 키로 존재하는지 확인
     * 2. 존재한다면 해당 세션 Set이 비어있지 않은지 확인
     * 3. 둘 다 true여야 실제로 온라인 상태
     */
    public static boolean isUserOnline(Long userId) {
        return onlineUsers.containsKey(userId) && !onlineUsers.get(userId).isEmpty();
    }
    
    /**
     * 현재 온라인 상태인 모든 사용자 정보를 반환하는 정적 메소드
     * 관리자 페이지나 통계 기능에서 사용 가능
     * 
     * @return 온라인 사용자 맵 (Key: 사용자 ID, Value: 세션 ID 집합)
     * 
     * 주의: 실제 Map 인스턴스를 반환하므로 외부에서 수정 가능
     * 보안이 중요하다면 Collections.unmodifiableMap()으로 감싸는 것을 권장
     */
    public static Map<Long, Set<String>> getOnlineUsers() {
        return onlineUsers;
    }
}