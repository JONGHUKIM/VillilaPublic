package com.splusz.villigo.web;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ReviewDto;
import com.splusz.villigo.service.AlarmService;
import com.splusz.villigo.service.ReviewService;
import com.splusz.villigo.service.UserService;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final AlarmService alarmService;

    /**
     * 후기 등록
     */
    @PostMapping
    public ResponseEntity<Void> submitReview(@AuthenticationPrincipal Object principal,
                                             @RequestBody ReviewDto dto) {
        User user = SecurityUserUtil.getUserFromPrincipal(principal);

        if (user == null || user.getId() == null) {
            log.warn("submitReview: 사용자 정보가 없거나 ID가 null입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = user.getId();
        log.info("submitReview: userId={}, dto={}", userId, dto);

        // 후기 등록
        Reservation reservation = reviewService.submitReview(userId, dto);

        // 매너 점수 반영
        int scoreDelta = dto.getScore();
        userService.updateMannerScore(dto.getTargetId(), scoreDelta);
        log.info("매너 점수 업데이트: targetId={}, delta={}", dto.getTargetId(), scoreDelta);

        // 알림 전송
        Alarm alarm;
        if (dto.getIsOwner() == 1) {
            alarm = alarmService.create(alarmService.reservationReviewAlarmBuilder(reservation));
            alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
            log.info("알람 전송 (예약자에게 후기 작성 알림)");
        } else {
            alarm = alarmService.create(alarmService.reservationFinishAlarmBuilder(reservation));
            alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
            log.info("알람 전송 (상품 주인에게 후기 알림)");
        }

        return ResponseEntity.ok().build();
    }

    /**
     * 특정 사용자에 대한 후기 리스트 조회 (마이페이지)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable("userId") Long userId) {
        log.info("getReviews: userId={}", userId);
        List<ReviewDto> reviews = reviewService.getReviewsForUser(userId);
        return ResponseEntity.ok(reviews);
    }

    /**
     * 특정 사용자의 매너 점수 조회
     */
    @GetMapping("/manner-score/{userId}")
    public ResponseEntity<Integer> getMannerScore(@PathVariable("userId") Long userId) {
        int score = userService.getMannerScore(userId);
        log.info("getMannerScore: userId={}, score={}", userId, score);
        return ResponseEntity.ok(score);
    }
}
