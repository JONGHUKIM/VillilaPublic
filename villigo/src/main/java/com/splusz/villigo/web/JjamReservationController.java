package com.splusz.villigo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.JjamReservationDto;
import com.splusz.villigo.service.JjamReservationService;
import com.splusz.villigo.service.UserJjamService;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/jjam/reservations")
@RequiredArgsConstructor
@Slf4j
public class JjamReservationController {

    private final JjamReservationService jjamReservationService;
    // private final UserJjamService userJjamService; 결제 API 서비스 이용 시 다시 사용할 예정

    /**
     * 젤리 예약 실행
     */
    @PostMapping
    public ResponseEntity<?> makeReservation(@AuthenticationPrincipal Object principal,
                                             @RequestBody JjamReservationDto request) { // Object로 변경
        User user = SecurityUserUtil.getUserFromPrincipal(principal); // 헬퍼 메서드 사용

        // 사용자 정보가 없거나 ID가 null인 경우 인증되지 않은 응답 반환
        if (user == null || user.getId() == null) {
            log.warn("makeReservation: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }

        // int currentJjams = userJjamService.getUserTotalJjams(user.getId());
        int fee = request.getFee();
        Long productId = request.getProductId(); // DTO에 carId 필드가 존재해야 함

//      log.info(productId.toString());
        // log.info("currentJjams={}, fee={}", currentJjams, fee);

//        if (currentJjams < fee) {
//            log.warn("makeReservation: 젤리가 부족합니다. 현재 젤리: {}, 필요한 젤리: {}", currentJjams, fee);
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body(Map.of("success", false, "message", "젤리가 부족합니다."));
//        }
//
//        // 젤리 차감
//        userJjamService.subtractJjams(user.getId(), fee);
//
//        // 남은 젤리 계산
//        int remainingJjams = currentJjams - fee;

//        log.info("makeReservation: 예약 완료. 사용 젤리: {}, 남은 젤리: {}", fee, remainingJjams);
//        return ResponseEntity.ok(Map.of(
//                "success", true,
//                "message", "예약이 완료되었습니다.",
//                "usedJjams", fee,
//                "remainingJjams", remainingJjams,
//                "productId", productId
//        ));
        
        log.info("makeReservation: 예약 요청 성공 (JJAM 차감 없음). 요청 금액: {}", fee);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "예약 신청이 완료되었습니다. (결제는 추후 진행)", // 메시지 변경
                "usedJjams", fee, 
                "remainingJjams", 0, 
                "productId", productId
        ));
    }
}
