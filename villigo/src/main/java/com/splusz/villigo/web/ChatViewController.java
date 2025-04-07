package com.splusz.villigo.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ChatMessageDto;
import com.splusz.villigo.dto.ChatRoomDto;
import com.splusz.villigo.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/chat")
@Slf4j
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatService chatService;

    @GetMapping("")
    public String chatPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null || user.getId() == null) {
            log.warn("사용자가 로그인되지 않았거나 userId가 null입니다.");
            return "redirect:/login";
        }

        List<ChatRoomDto> chatRooms = chatService.getUserChatRooms(user.getId());
        ChatRoomDto latestChatRoom = chatRooms.isEmpty() ? null : chatRooms.get(0);
        log.info("latestChatRoom ID: {}", latestChatRoom != null ? latestChatRoom.getId() : "null");
        List<ChatMessageDto> chatMessages = latestChatRoom != null && latestChatRoom.getId() != 0L
            ? chatService.getMessages(latestChatRoom.getId())
            : new ArrayList<>();
        model.addAttribute("chatList", chatRooms);
        model.addAttribute("chatRoomId", latestChatRoom != null ? latestChatRoom.getId() : null);
        model.addAttribute("chatUserName", latestChatRoom != null ? latestChatRoom.getName() : "");
        model.addAttribute("userId", user.getId());
        model.addAttribute("chatMessages", chatMessages);
        return "chat";
    }
}
