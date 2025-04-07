package com.splusz.villigo.web;

import com.splusz.villigo.config.CurrentUser;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.MyReservationDto;
import com.splusz.villigo.service.MyReservationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MyReservationController {

    @Autowired
    private MyReservationService myReservationService;

    @GetMapping("/mypage")
    public String myPage(@CurrentUser User user, Model model) {
        // 로그인한 사용자의 ID를 가져옴
    	Long userId = user.getId();

        // Service를 통해 최신순(생성시간 기준) 예약 정보를 가져옴
        List<MyReservationDto> myReservations = myReservationService.getMyReservations(userId);
        myReservations.forEach(System.out::println);
        model.addAttribute("myReservations", myReservations);

        // 다른 탭에서 필요한 데이터도 추가 가능
        return "mypage"; // Thymeleaf 템플릿 이름
    }

    // 인증 객체에서 사용자 ID를 추출하는 메서드
    private Long getUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("로그인한 사용자가 없습니다.");
        }

        // Authentication의 Principal에서 User 객체를 추출
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            User user = (User) principal;
            return user.getId();
        }

        throw new IllegalStateException("사용자 정보를 가져올 수 없습니다.");
    }
}
