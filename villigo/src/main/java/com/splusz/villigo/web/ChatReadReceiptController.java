package com.splusz.villigo.web;

import com.splusz.villigo.service.ChatReadReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat/read")
@RequiredArgsConstructor
public class ChatReadReceiptController {

    private final ChatReadReceiptService chatReadReceiptService;

    // 특정 메시지를 읽음 처리하는 API
    @PostMapping("/{messageId}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable(name = "messageId") Long messageId,
            @RequestParam(name = "userId") Long userId) {

        chatReadReceiptService.markMessageAsRead(messageId, userId);
        return ResponseEntity.ok().build();
    }

    // 특정 메시지가 읽혔는지 확인하는 API
    @GetMapping("/{messageId}")
    public ResponseEntity<Boolean> checkIfRead(@PathVariable(name = "messageId") Long messageId, @RequestParam(name = "userId") Long userId) {

        boolean isRead = chatReadReceiptService.hasBeenRead(messageId, userId);
        return ResponseEntity.ok(isRead);
    }
}
