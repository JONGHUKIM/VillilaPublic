package com.splusz.villigo.web;

import com.splusz.villigo.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class ChatMigrationController {

    private final ChatService chatService;

    @PostMapping("/migrate-read-by")
    public ResponseEntity<String> migrateReadByData() {
        try {
            chatService.migrateReadByData();
            return ResponseEntity.ok("마이그레이션 완료");
        } catch (Exception e) {
            log.error("마이그레이션 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.status(500).body("마이그레이션 실패: " + e.getMessage());
        }
    }
}