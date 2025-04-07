package com.splusz.villigo.web;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.splusz.villigo.config.CurrentUser;
import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ReservationCardDto;
import com.splusz.villigo.dto.ReservationCreateDto;
import com.splusz.villigo.service.AlarmService;
import com.splusz.villigo.service.BagService;
import com.splusz.villigo.service.CarService;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.ReservationService;

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

    // 예약 상세 페이지 (/reservation/details)
    @GetMapping("/details")
    public String showReservationDetails() {
        return "reservation/details"; 
    }
    
    // 예약 검사
    @PostMapping("/check")
    public ResponseEntity<Boolean> checkReservation(
    		@RequestBody ReservationCreateDto dto, @CurrentUser User user){
    	log.info("checkReservation(dto={}, user={})", dto, user);
    	// productId로 해당 제품의 현재 예약들을 불러와서 시간이 중복되는 예약은 없는지 확인
    	Long productId = dto.getProductId();
    	List<Reservation> reservations = reserveService.readAll(productId);
    	boolean isConflict = reservations.stream().anyMatch(reserv -> reserv.isOverlapping(dto));
    	log.info("isConflict={}", isConflict);
    	if (!isConflict) {
        	// 중복되는 예약이 없으면 true를 리턴.
        	return ResponseEntity.ok(true);
    	}
    	// 중복되는 예약이 있으면(isConflict == true) false를 리턴.
		return ResponseEntity.ok(false);
    	
    }
    
    // 예약 등록
    @PostMapping("/create")
    public ResponseEntity<Boolean> createReservation(
    		@RequestBody ReservationCreateDto dto, @CurrentUser User user) {
    	log.info("createReservation(dto={}, user={})", dto, user);
		// (1) 예약 테이블에 데이터를 추가하고 알람 객체를 생성
		// (2) 알람 테이블에 데이터를 추가
		// (3) 예약된 상품 주인에게 알람 발송
		// (4) 클라이언트에게 true로 응답
    	Alarm alarm = alarmService.reservationCreatedAlarmBuilder(dto, user); // (1)
    	alarm = alarmService.create(alarm); // (2)
    	log.info("alarm saved: {}", alarm);
    	alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent()); // (3)
    	log.info("생성된 알람을 웹소캣으로 전송");
    	
    	return ResponseEntity.ok(true); // (4)
    }
    
	// 예약 대기중 - status: 1
    @GetMapping("/accept/{reservationId}")
    public ResponseEntity<Boolean> acceptResevation(@PathVariable(name = "reservationId") Long reservationId) {
    	log.info("accceptReservation(id={})", reservationId);
    	// 예약 데이터의 status를 1(대기중)로 변경.
    	Reservation entity = reserveService.update(reservationId, 1);
    	// 업데이트가 성공하면 true, 실패하면 false를 응답.
    	if (entity != null) return ResponseEntity.ok(true);
    	return ResponseEntity.ok(false);
    }
    
    // 예약 수락 - status: 2
    @GetMapping("/confirm/{reservationId}/{productId}")
    public ResponseEntity<Boolean> confirmReservation(@PathVariable(name = "reservationId") Long reservationId,
    		@PathVariable(name = "productId") Long productId) {
    	log.info("confirmReservation(id={})", reservationId);
    	// 예약 데이터의 status를 2(거래중)로 변경.
    	Reservation entity = reserveService.update(reservationId, 2);
    	// 업데이트가 성공하면 true, 실패하면 false를 응답.
    	if (entity != null) {
    		// 업데이트가 성공하면 예약 수락 결과를 상품을 대여한 사용자에게 알림
    		Alarm alarm = alarmService.reservationConfirmAlarmBuilder(productId, entity);
    		log.info("예약 신청 수락 알람 생성");
    		alarm = alarmService.create(alarm);
    		log.info("생성된 알람을 데이터베이스에 저장");
    		alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
    		log.info("생성된 알람을 웹소캣으로 전송");
    		
    		return ResponseEntity.ok(true);
    	}
    	
    	return ResponseEntity.ok(false);
    }
    
    // 예약 종료 - status: 3
    @GetMapping("/finish")
    public ResponseEntity<Boolean> finishReservation(@RequestParam("reservationId") Long reservationId) {
        log.info("finishReservation(id={})", reservationId);
    	// 예약 데이터의 status를 3(거래완료)로 변경.
    	Reservation entity = reserveService.update(reservationId, 3);
    	// 업데이트가 성공하면 true, 실패하면 false를 응답.
    	if (entity != null) return ResponseEntity.ok(true);
    	
    	return ResponseEntity.ok(false);
    }
    
    // 예약 거절 - status: 4
    @GetMapping("/refuse/{reservationId}/{productId}")
    public ResponseEntity<Boolean> refuseReservation(@PathVariable(name = "reservationId") Long reservationId,
    		@PathVariable(name = "productId") Long productId) {
    	log.info("refuseReservation(id={})", reservationId);
    	// 예약 데이터의 status를 4(거절됨)로 변경.
    	Reservation entity = reserveService.update(reservationId, 4);
    	// 업데이트가 성공하면 true, 실패하면 false를 응답.
    	if (entity != null) {
    		// 업데이트가 성공하면 예약 거절 결과를 상품을 대여한 사용자에게 알림
    		Alarm alarm = alarmService.reservationRefuseAlarmBuilder(productId, entity);
    		log.info("예약 거절 알람 생성");
    		alarm = alarmService.create(alarm);
    		log.info("생성된 알람을 데이터베이스에 저장");
    		alarmService.sendNotification(alarm.getReceiver().getUsername(), alarm.getContent());
    		log.info("생성된 알람을 웹소캣으로 전송");
    		
    		return ResponseEntity.ok(true);
    	}
    	
    	return ResponseEntity.ok(false);
    }
    
    // 예약 삭제
    @DeleteMapping("/delete/{reservationId}")
    public ResponseEntity<Long> deleteReservation(@PathVariable(name = "reservationId") Long reservationId) {
    	log.info("deleteReservation(id={})", reservationId);
    	
    	reserveService.changeStatusTodelete(reservationId);
    	
    	return ResponseEntity.ok(reservationId);
    }
    
    // 마이 페이지 - 예약현황: 현재 로그인한 사용자에게 들어온 예약들을 PagedModel로 리턴
    @GetMapping("/api/requestlist")
    public ResponseEntity<Object> getReservationRequestList(@CurrentUser User user,
    		@RequestParam(name="p", defaultValue = "0") int pageNo) {
    	log.info("getReservationRequestList(pageNo={}, user={})", pageNo, user);
    	Page<Reservation> page = reserveService.readAllByUserId(
    			user.getId(), pageNo, Sort.by("id").descending());
    	log.info("페이징 객체 생성 -> 타입을 ReservationCardDto로 변환");
    	Page<ReservationCardDto> dtoPage = page.map(reserveService::convertToMyReservationDto);
    	log.info("페이징 객체 타입 변환 완료");
    	dtoPage.forEach(System.out::println);
    	
    	return ResponseEntity.ok(new PagedModel<>(dtoPage));
    }

}


