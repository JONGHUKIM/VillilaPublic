package com.splusz.villigo.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.JjamReservationDto;
import com.splusz.villigo.service.JjamReservationService;
import com.splusz.villigo.service.UserJjamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/jjam/reservations")
@RequiredArgsConstructor
@Slf4j
public class JjamReservationController {

    private final JjamReservationService jjamReservationService;
    private final UserJjamService userJjamService;

    /**
     * 젤리 예약 실행
     */
    @PostMapping
    public ResponseEntity<?> makeReservation(@AuthenticationPrincipal User user, @RequestBody JjamReservationDto request) {
        int currentJjams = userJjamService.getUserTotalJjams(user.getId());
        int fee = request.getFee();
        Long productId = request.getProductId(); // DTO에 carId 필드가 존재해야 함
//        log.info(productId.toString());
        log.info("currentJjams={}, fee={}", currentJjams, fee);

        if (currentJjams < fee) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "젤리가 부족합니다."));
        }

        // 젤리 차감
        userJjamService.subtractJjams(user.getId(), fee);

        // 남은 젤리 계산
        int remainingJjams = currentJjams - fee;

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "예약이 완료되었습니다.",
                "usedJjams", fee,
                "remainingJjams", remainingJjams,
                "productId", productId
        ));
    }

    
        
    
}
