package com.splusz.villigo.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.MyReservationDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.service.MyReservationService;
import com.splusz.villigo.service.UserService;
import com.splusz.villigo.util.SecurityUserUtil;

@Controller
public class MyReservationController {

    @Autowired
    private MyReservationService myReservationService;
    
    @Autowired
    private UserService userService;

    @GetMapping("/mypage")
    public String myPage(@AuthenticationPrincipal Object principal, Model model) { // @CurrentUser 대신 Object principal 사용
        User user = SecurityUserUtil.getUserFromPrincipal(principal); // SecurityUserUtil 사용

        // 사용자 정보가 없거나 ID가 null인 경우 로그인 페이지로 리다이렉트
        if (user == null || user.getId() == null) {
            System.out.println("MyReservationController: 로그인한 사용자가 없거나 사용자 ID가 null입니다. 로그인 페이지로 이동합니다.");
            return "redirect:/member/signin"; // 경로 통일
        }

        // 로그인한 사용자의 ID를 가져옴
        Long userId = user.getId();
        
        UserProfileDto userProfile = userService.getCurrentUserProfile(); // 이미 UserService에서 SecurityUserUtil 사용
        model.addAttribute("user", userProfile); // mypage.html에서 user.avatar, user.nickname 등으로 사용 가능

        // Service를 통해 최신순(생성시간 기준) 예약 정보를 가져옴
        List<MyReservationDto> myReservations = myReservationService.getMyReservations(userId);
        myReservations.forEach(System.out::println);
        
        model.addAttribute("myReservations", myReservations);
        model.addAttribute("userId", userId); // Thymeleaf에서 userId를 사용할 수 있도록 추가

        // 다른 탭에서 필요한 데이터도 추가 가능
        return "mypage"; // Thymeleaf 템플릿 이름
    }
}
