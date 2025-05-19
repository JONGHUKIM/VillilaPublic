package com.splusz.villigo.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.config.WebSocketEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/online")
public class OnlineStatusController {

    // 현재 온라인인 사용자 목록을 반환
    @GetMapping("/users")
    public Map<Long, Boolean> getOnlineUsers() {
        // Map<Long, Set<String>>를 Map<Long, Boolean>로 변환
        Map<Long, Set<String>> onlineUsers = WebSocketEventListener.getOnlineUsers();
        return onlineUsers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, // 사용자 ID (Long)
                        entry -> !entry.getValue().isEmpty() // Set<String>이 비어 있지 않으면 true (온라인)
                ));
    }

    // 특정 사용자의 온라인 상태 조회
    @GetMapping("/users/{userId}")
    public Map<String, Boolean> isUserOnline(@PathVariable(name = "userId") Long userId) {
        boolean isOnline = WebSocketEventListener.isUserOnline(userId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("online", isOnline);
        return response;
    }
}