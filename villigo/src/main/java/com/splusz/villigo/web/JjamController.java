package com.splusz.villigo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.splusz.villigo.dto.JjamPurchaseDto;
import com.splusz.villigo.service.UserJjamService;
import com.splusz.villigo.domain.UserJjam;
import com.splusz.villigo.domain.User;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/jjam")
public class JjamController {
    private final UserJjamService userJjamService;

    // 🔹 💰 젤리 충전 페이지 (현재 보유 젤리 개수 추가)
    @GetMapping("/charge")
    public String getJjamChargePage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login"; // 로그인되지 않은 경우 로그인 페이지로 이동
        }
        int totalJjams = userJjamService.getUserTotalJjams(user.getId()); // 현재 보유 젤리 조회
        model.addAttribute("totalJjams", totalJjams);
        return "jjam/charge"; // 💡 젤리 충전 페이지로 이동
    }

    // 🔹 젤리 결제 페이지 (로그인된 사용자 정보 추가)
    @GetMapping("/payment")
    public String getPaymentPage(Model model, @AuthenticationPrincipal User user) {
        if (user == null) {
            return "redirect:/login"; // 로그인되지 않은 경우 로그인 페이지로 이동
        }
        model.addAttribute("userId", user.getId()); // 현재 로그인한 유저 ID 전달
        return "jjam/payment"; // 💡 젤리 결제 페이지로 이동
    }

    // 🔹 특정 유저의 현재 보유 젤리 개수 조회 API (AJAX 요청용)
    @GetMapping("/api/jjams/total")
    @ResponseBody
    public ResponseEntity<Integer> getUserJjamTotal(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        int totalJjams = userJjamService.getUserTotalJjams(user.getId());
        return ResponseEntity.ok(totalJjams);
    }
    // 🔹 특정 유저의 현재 보유 젤리 개수 조회 API (AJAX 요청용)
    @GetMapping("/api/user/info")
    @ResponseBody
    public ResponseEntity<Long> getUserId(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        log.info("user: {}", user);
        long userId = user.getId();
        log.info("userId: {}", userId);
        return ResponseEntity.ok(userId);
    }
    
    // 🔹 젤리 충전 요청 API (AJAX 요청 - 결제 후 자동 반영)
    @PostMapping("/api/jjams/purchase")
    @ResponseBody
    public ResponseEntity<Long> purchaseJjam(@RequestBody JjamPurchaseDto request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized 반환
        }

        User user = (User) authentication.getPrincipal();
    	log.info("user: {}", user);
    	/*
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }*/

        log.info("🔄 젤리 충전 요청 - User ID: {}, Quantity: {}", user.getId(), request.getQuantity());

        // ✅ `UserJjamService`에서 `purchaseJjam()` 호출
        UserJjam userJjam = userJjamService.purchaseJjam(user.getId(), request.getQuantity());

        log.info("✅ 젤리 충전 완료 - User ID: {}, 충전 후 잔여 젤리: {}", 
                 user.getId(), userJjamService.getUserTotalJjams(user.getId()));

        return ResponseEntity.ok(user.getId());
    }

    // 🔹 마이페이지에서 보유 젤리 개수 조회 (API)
    @GetMapping("/api/jjams/my-page")
    @ResponseBody
    public ResponseEntity<Integer> getUserJjamTotalForMyPage(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        int totalJjams = userJjamService.getUserTotalJjams(user.getId());
        return ResponseEntity.ok(totalJjams);
    }
}
