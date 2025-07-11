package com.splusz.villigo.web;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ChatMessageDto;
import com.splusz.villigo.dto.ChatRoomDto;
import com.splusz.villigo.service.ChatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/chat")
@Slf4j
@RequiredArgsConstructor
public class ChatViewController {

    private final ChatService chatService;

    @GetMapping("")
    public String chatPage(Model model, @AuthenticationPrincipal Object principal) { // Object로 받기
        User user = null;

        if (principal instanceof CustomOAuth2User) {
            CustomOAuth2User oauthUser = (CustomOAuth2User) principal;
            user = oauthUser.getUser(); // CustomOAuth2User에서 User 객체 추출

            // OAuth2 사용자의 프로필 완성 여부 확인
            if (user == null || !oauthUser.isProfileComplete()) {
                log.info("Google 로그인 사용자의 프로필이 미완성 또는 User 객체 Null. 추가 정보 입력 페이지로 리다이렉트합니다.");
                // 리다이렉션 경로 수정 /member/signin 대신 /member/signup-social
                return "redirect:/member/signup-social"; 
            }

        } else if (principal instanceof User) { // 일반 로그인 사용자가 User 도메인 객체를 Principal로 사용하는 경우
            user = (User) principal; // 직접 User 객체로 캐스팅
            
            if (user.getId() == null) { // 일반 로그인 사용자도 ID가 없으면 문제
                log.warn("일반 로그인 사용자의 ID가 null입니다.");
                return "redirect:/member/signin";
            }

        } else { // 예상치 못한 Principal 타입이거나, Principal 자체가 null인 경우 (로그인 안 됨)
            log.warn("인증된 Principal이 없거나 예상치 못한 타입입니다. Principal: {}", principal);
            // 리다이렉션 경로 수정: /login 대신 /member/signin
            return "redirect:/member/signin"; 
        }

        // user 객체가 제대로 설정되었는지 최종 확인
        if (user == null || user.getId() == null) {
            log.warn("user 객체가 최종적으로 null이거나 ID가 null입니다. 다시 로그인 페이지로 리다이렉트합니다.");
            return "redirect:/member/signin";
        }

        List<ChatRoomDto> chatRooms = chatService.getUserChatRooms(user.getId());
        ChatRoomDto latestChatRoom = chatRooms.isEmpty() ? null : chatRooms.get(0);
        log.info("latestChatRoom ID: {}", latestChatRoom != null ? latestChatRoom.getId() : "null");
        List<ChatMessageDto> chatMessages = latestChatRoom != null && latestChatRoom.getId() != 0L
            ? chatService.getMessages(latestChatRoom.getId())
            : new ArrayList<>();
        model.addAttribute("chatList", chatRooms);
        model.addAttribute("chatRoomId", latestChatRoom != null ? latestChatRoom.getId() : null);
        
        // senderName이 null인 경우를 대비하여 user.getDisplayUsername() 또는 닉네임/유저네임 우선 사용
        String chatUserName = user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername();
        model.addAttribute("chatUserName", chatUserName); 
        
        model.addAttribute("userId", user.getId());
        model.addAttribute("chatMessages", chatMessages);
        return "chat";
    }
}
