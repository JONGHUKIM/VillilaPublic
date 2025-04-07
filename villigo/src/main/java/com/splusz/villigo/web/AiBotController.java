package com.splusz.villigo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.service.FaqService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bot") // 챗봇 API
public class AiBotController {

    private final FaqService faqService;

    @PostMapping("/chat")
    public ResponseEntity<?> chatbot(@RequestBody ChatRequest request) {
        try {
            String userMessage = request.getMessage();

            // DB에서 FAQ 조회
            return faqService.getAnswer(userMessage)
                    .map(answer -> ResponseEntity.ok(new ChatResponse(answer))) // FAQ에서 찾은 경우
                    .orElse(ResponseEntity.ok(new ChatResponse("궁금하신 사항에 대한 정보가 없습니다. 다른 질문을 입력해주세요."))); // FAQ에 없을 경우
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("오류 발생: " + e.getMessage());
        }
    }

    // 요청 데이터 모델
    public static class ChatRequest {
        private String message;
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    // 응답 데이터 모델
    public static class ChatResponse {
        private String response;
        public ChatResponse(String response) { this.response = response; }
        public String getResponse() { return response; }
    }
}
