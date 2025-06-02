package com.splusz.villigo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 온라인 사용자 목록을 관리하는 ConcurrentHashMap (스레드 안전)
    private static final Map<String, Boolean> onlineUsers = new ConcurrentHashMap<>();
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws") // "/ws" 경로로 WebSocket 연결 엔드포인트 생성
		        .setAllowedOrigins(
		                "http://localhost:3000", // React 개발 서버 (보통 3000 포트)
		                "http://localhost:8080", // Spring Boot 개발 서버 (보통 8080 포트)
		                "https://villila.store" // 실제 운영 도메인
		            )
		        // 커스텀 핸드셰이크 인터셉터 추가 (연결 전 검증이나 추가 로직 처리)
                .addInterceptors(new CustomHandshakeInterceptor())
                // SockJS 지원 활성화 (WebSocket을 지원하지 않는 구형 브라우저를 위한 폴백)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // "/topic": 여러 사용자에게 브로드캐스트하는 메시지용 (1:N 통신)
        // "/queue": 특정 사용자에게 보내는 개인 메시지용 (1:1 통신)
        config.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 서버로 메시지를 보낼 때 사용할 경로 prefix
        config.setApplicationDestinationPrefixes("/app");
        // 개인 사용자에게 메시지를 보낼 때 사용할 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String userId = accessor.getFirstNativeHeader("userId");
                    if (userId != null) {
                        accessor.getSessionAttributes().put("userId", userId);  // 세션에 저장
                        accessor.setUser(() -> userId);
                        WebSocketConfig.getOnlineUsers().put(userId, true);
                    }
                }
                return message;
            }
        });
    }


    // 사용자가 웹소켓에서 연결 해제될 때 호출
    public static void userDisconnected(String userId) {
        onlineUsers.remove(userId);
        System.out.println("사용자 오프라인: " + userId);
    }

    // 현재 온라인 사용자 리스트 반환
    public static Map<String, Boolean> getOnlineUsers() {
        return onlineUsers;
    }
}