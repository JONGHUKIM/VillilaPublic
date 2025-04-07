package com.splusz.villigo.web;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.dto.ReviewDto;
import com.splusz.villigo.service.AlarmService;
import com.splusz.villigo.service.ReviewService;
import com.splusz.villigo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;  // UserService 주입
    private final AlarmService alarmService;
 
    // 후기 등록
    @PostMapping
    public void submitReview(@AuthenticationPrincipal(expression = "id") Long userId,
                             @RequestBody ReviewDto dto) {
        Reservation reservation = reviewService.submitReview(userId, dto);
        
        // 후기 작성 후 매너 점수 업데이트 (예: 리뷰 점수에 따라 매너 점수 증가/감소)
        int scoreDelta = dto.getScore();  // 예시로 점수를 덧셈 혹은 뺄셈 처리
        userService.updateMannerScore(dto.getTargetId(), scoreDelta);  // 매너 점수 업데이트
        
        Alarm alarm = null;
        System.out.println(">>>>>>>>>>>>> isOwner: " + dto.getIsOwner());
        if (dto.getIsOwner() == 1) {
        	alarm = alarmService.create(alarmService.reservationReviewAlarmBuilder(reservation));
        	System.out.println(">>>>> 알람 생성 완료");
        	alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
        	System.out.println(">>>>> 예약자에게 후기 알람 발송");
        } else {
        	alarm = alarmService.create(alarmService.reservationFinishAlarmBuilder(reservation));
        	System.out.println(">>>>> 알람 생성 완료");
        	alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
        	System.out.println(">>>>> 상품 주인에게 후기 알람 발송");
        }
    }

    // 후기 조회 (마이페이지용)
    @GetMapping("/{userId}")
    public List<ReviewDto> getReviews(@PathVariable("userId") Long userId) {
        return reviewService.getReviewsForUser(userId);
    }

    // 매너 점수 조회
    @GetMapping("/manner-score/{userId}")
    public int getMannerScore(@PathVariable("userId") Long userId) {
        return userService.getMannerScore(userId);  // 매너 점수 반환
    }

    
}
