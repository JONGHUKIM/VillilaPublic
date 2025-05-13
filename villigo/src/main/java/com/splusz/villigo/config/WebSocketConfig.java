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
        registry.addEndpoint("/ws")
		        .setAllowedOrigins(
		                "http://localhost:3000",
		                "http://localhost:8080",
		                "https://villila.store",
		                "http://villila.store"
		            )
                .addInterceptors(new CustomHandshakeInterceptor())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
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
                        accessor.getSessionAttributes().put("userId", userId);  // 🔧 세션에 저장
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