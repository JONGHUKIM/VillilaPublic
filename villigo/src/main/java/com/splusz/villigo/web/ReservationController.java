package com.splusz.villigo.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ReservationCardDto;
import com.splusz.villigo.dto.ReservationCreateDto;
import com.splusz.villigo.service.*;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/reservation")
public class ReservationController {

    private final ReservationService reserveService;
    private final ProductService productService;
    private final CarService carService;
    private final BagService bagService;
    private final AlarmService alarmService;

    // 팝업으로 띄울 예약 페이지
    @GetMapping
    public String showReservationForm() {
        return "reservation/index";
    }

    // 예약 상세 페이지
    @GetMapping("/details")
    public String showReservationDetails() {
        return "reservation/details";
    }

    // 예약 검사
    @PostMapping("/check")
    public ResponseEntity<Boolean> checkReservation(@RequestBody ReservationCreateDto dto,
                                                    @AuthenticationPrincipal Object principal) {
        User user = SecurityUserUtil.getUserFromPrincipal(principal);

        if (user == null || user.getId() == null) {
            log.warn("checkReservation: 사용자 정보가 없거나 ID가 null입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        log.info("checkReservation(dto={}, user={})", dto, user);
        List<Reservation> reservations = reserveService.readAll(dto.getProductId());
        boolean isConflict = reservations.stream().anyMatch(reserv -> reserv.isOverlapping(dto));
        log.info("isConflict={}", isConflict);

        return ResponseEntity.ok(!isConflict);
    }

    // 예약 등록
    @PostMapping("/create")
    public ResponseEntity<Boolean> createReservation(@RequestBody ReservationCreateDto dto,
                                                     @AuthenticationPrincipal Object principal) {
        User user = SecurityUserUtil.getUserFromPrincipal(principal);

        if (user == null || user.getId() == null) {
            log.warn("createReservation: 사용자 정보가 없거나 ID가 null입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
        }

        log.info("createReservation(dto={}, user={})", dto, user);

        // (1) 예약 알람 생성 → (2) 저장 → (3) 알람 전송
        Alarm alarm = alarmService.reservationCreatedAlarmBuilder(dto, user);
        alarm = alarmService.create(alarm);
        log.info("alarm saved: {}", alarm);
        alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());

        return ResponseEntity.ok(true);
    }

    // 예약 대기중 - status: 1
    @GetMapping("/accept/{reservationId}")
    public ResponseEntity<Boolean> acceptResevation(@PathVariable Long reservationId) {
        log.info("acceptReservation(id={})", reservationId);
        Reservation entity = reserveService.update(reservationId, 1);
        return ResponseEntity.ok(entity != null);
    }

    // 예약 수락 - status: 2
    @GetMapping("/confirm/{reservationId}/{productId}")
    public ResponseEntity<Boolean> confirmReservation(@PathVariable Long reservationId,
                                                      @PathVariable Long productId) {
        log.info("confirmReservation(id={})", reservationId);
        Reservation entity = reserveService.update(reservationId, 2);

        if (entity != null) {
            Alarm alarm = alarmService.reservationConfirmAlarmBuilder(productId, entity);
            alarm = alarmService.create(alarm);
            alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
            return ResponseEntity.ok(true);
        }

        return ResponseEntity.ok(false);
    }

    // 예약 종료 - status: 3
    @GetMapping("/finish")
    public ResponseEntity<Boolean> finishReservation(@RequestParam Long reservationId) {
        log.info("finishReservation(id={})", reservationId);
        Reservation entity = reserveService.update(reservationId, 3);
        return ResponseEntity.ok(entity != null);
    }

    // 예약 거절 - status: 4
    @GetMapping("/refuse/{reservationId}/{productId}")
    public ResponseEntity<Boolean> refuseReservation(@PathVariable Long reservationId,
                                                     @PathVariable Long productId) {
        log.info("refuseReservation(id={})", reservationId);
        Reservation entity = reserveService.update(reservationId, 4);

        if (entity != null) {
            Alarm alarm = alarmService.reservationRefuseAlarmBuilder(productId, entity);
            alarm = alarmService.create(alarm);
            alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
            return ResponseEntity.ok(true);
        }

        return ResponseEntity.ok(false);
    }

    // 예약 삭제
    @DeleteMapping("/delete/{reservationId}")
    public ResponseEntity<Long> deleteReservation(@PathVariable Long reservationId,
                                                  @AuthenticationPrincipal Object principal) {
        User user = SecurityUserUtil.getUserFromPrincipal(principal);

        if (user == null || user.getId() == null) {
            log.warn("deleteReservation: 사용자 정보가 없거나 ID가 null입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("deleteReservation(id={}, user={})", reservationId, user);
        Reservation reservation = reserveService.read(reservationId);

        if (reservation.getRenter().getId().equals(user.getId())) {
            reserveService.delete(reservationId); // 하드 삭제
        } else {
            reserveService.changeStatusTodelete(reservationId); // 소프트 삭제
        }

        return ResponseEntity.ok(reservationId);
    }

    // 마이페이지 예약 요청 목록
    @GetMapping("/api/requestlist")
    public ResponseEntity<Object> getReservationRequestList(@AuthenticationPrincipal Object principal,
                                                            @RequestParam(name = "p", defaultValue = "0") int pageNo) {
        User user = SecurityUserUtil.getUserFromPrincipal(principal);

        if (user == null || user.getId() == null) {
            log.warn("getReservationRequestList: 사용자 정보가 없거나 ID가 null입니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("getReservationRequestList(pageNo={}, user={})", pageNo, user);
        Page<Reservation> page = reserveService.readAllByUserId(user.getId(), pageNo, Sort.by("id").descending());
        Page<ReservationCardDto> dtoPage = page.map(reserveService::convertToMyReservationDto);
        dtoPage.forEach(System.out::println);

        return ResponseEntity.ok(new PagedModel<>(dtoPage));
    }

    // 상품 상세에서 예약 시간 목록 조회
    @GetMapping("/api/reservations")
    public ResponseEntity<List<Map<String, String>>> getReservations(@RequestParam Long productId) {
        List<Reservation> reservations = reserveService.readAll(productId);

        List<Map<String, String>> list = reservations.stream().map(r -> {
            Map<String, String> map = new HashMap<>();
            map.put("start", r.getStartTime().toString());
            map.put("end", r.getEndTime().toString());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }
}
