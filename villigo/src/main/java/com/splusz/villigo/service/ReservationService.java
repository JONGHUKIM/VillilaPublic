package com.splusz.villigo.service;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.RentalImageDto;
import com.splusz.villigo.dto.ReservationCardDto;
<<<<<<< HEAD
<<<<<<< HEAD
=======
import com.splusz.villigo.dto.ReservationCreateDto;
import com.splusz.villigo.dto.ReservationDto;
>>>>>>> 4b57c67 (Reservation<DTO>로 수정)
=======
import com.splusz.villigo.dto.ReservationDto;
>>>>>>> 6ba8a3a (MyPage 기존 로컬 -> S3 이미지로 변경)
import com.splusz.villigo.repository.AlarmCategoryRepository;
import com.splusz.villigo.repository.AlarmRepository;
import com.splusz.villigo.repository.ChatRoomReservationRepository;
import com.splusz.villigo.repository.RentalImageRepository;
import com.splusz.villigo.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationService {

	private final ReservationRepository reserveRepo;
	private final RentalImageRepository rentalimgRepo;
	private final ChatRoomReservationRepository chatRoomReservationRepo;
	private final SimpMessagingTemplate messagingTemplate; // 알림용
	private final AlarmCategoryRepository alarmCatRepo;
	private final AlarmRepository alarmRepo;
<<<<<<< HEAD
<<<<<<< HEAD
=======
	private final RentalImageService rentalImgServ;
	private final ProductRepository prodRepo;
>>>>>>> 4b57c67 (Reservation<DTO>로 수정)
=======
	private final RentalImageService rentalImgServ;
>>>>>>> 6ba8a3a (MyPage 기존 로컬 -> S3 이미지로 변경)
	
	public Reservation read(Long id) {
		log.info("예약 조회 요청: 예약 ID={}", id); // 로그 메시지 한글화
		
		Reservation reservation = reserveRepo.findById(id).orElseThrow();
		return reservation;
	}
	
	@Transactional(readOnly = true)
	public List<ReservationDto> readAll(Long productId) {
	    log.info("상품 ID로 모든 예약 조회 요청: 상품 ID={}", productId); // 로그 메시지 한글화
	    return reserveRepo.findByProductId(productId).stream()
	            .map(this::convertToReservationDto)
	            .collect(Collectors.toList());
	}
	
 @Transactional(readOnly = true)
 public Page<ReservationDto> readAllByUserId(Long userId, int pageNo, Sort sort) {
     log.info("사용자 ID로 모든 예약 페이지 조회 요청: 사용자 ID={}, 페이지 번호={}, 정렬={}", userId, pageNo, sort); // 로그 메시지 한글화
     Pageable pageable = PageRequest.of(pageNo, 5, sort);
     Page<Reservation> page = reserveRepo.findByProductUserId(userId, pageable);
     
     log.info("readAllByUserId: 상품 주인이 {}인 {}개의 예약 엔티티 조회.", userId, page.getTotalElements()); // 로그 메시지 한글화
     page.getContent().forEach(res -> log.info("  - 원본 예약 엔티티: ID={}, 상태={}, 상품ID={}, 예약자ID={}", // 로그 메시지 한글화
                                         res.getId(), res.getStatus(), res.getProduct().getId(), res.getRenter().getId()));

     List<ReservationDto> dtoList = page.getContent().stream()
                                         .map(this::convertToReservationDto)
                                         .collect(Collectors.toList());

     log.info("readAllByUserId: 사용자 ID {}에 대해 {}개의 예약 DTO 변환 완료.", userId, dtoList.size()); // 로그 메시지 한글화
     dtoList.forEach(dto -> log.info("  - 변환된 DTO: ID={}, 상태={}, 예약자닉네임={}, 이미지URL={}", // 로그 메시지 한글화
                                         dto.getId(), dto.getStatus(), dto.getRenterNickname(), dto.getImageUrl()));

     long nonStatus5Count = dtoList.stream().filter(dto -> dto.getStatus() != 5).count();
     log.info("readAllByUserId: 렌더링될 (상태 5가 아닌) 예약 DTO 개수: {}", nonStatus5Count); // 로그 메시지 한글화

     return new PageImpl<>(dtoList, pageable, page.getTotalElements());
 }

	public Reservation create(Reservation entity) {
		log.info("예약 엔티티 생성 요청: 엔티티={}", entity); // 로그 메시지 한글화
		
		entity = reserveRepo.save(entity);
		return entity;
	}
	
	public Reservation update(Long id, int status) {
		log.info("예약 상태 업데이트 요청: 예약 ID={}, 새 상태={}", id, status); // 로그 메시지 한글화
		
		Reservation entity = reserveRepo.findById(id).orElseThrow();
		entity.setStatus(status);
		entity = reserveRepo.save(entity);
		log.info("예약 엔티티 업데이트 완료: {}", entity); // 로그 메시지 한글화
		return entity;
	}
	
	@Transactional
	public void delete(Long id) {
	    log.info("예약 삭제 요청: 예약 ID={}", id); // 로그 메시지 한글화

	    Reservation reservation = reserveRepo.findById(id).orElseThrow();
	    int status = reservation.getStatus();
	    if (!(status == 0 || status == 1 || status == 5)) {
	        throw new IllegalStateException("현재 상태에서는 예약을 취소/삭제할 수 없습니다.");
	    }

	    String cancellerNickname = reservation.getRenter().getNickname();
	    User host = reservation.getProduct().getUser();
	    String productName = reservation.getProduct().getProductName();
	    String hostUsername = host.getUsername();

	    chatRoomReservationRepo.deleteByReservationId(id);
	    reserveRepo.deleteById(id);

	    String content = "❌ 예약 취소 알림\n" +
	            cancellerNickname + "님이 [" + productName + "] 예약을 취소했어요.";

	    Alarm alarm = Alarm.builder()
	            .alarmCategory(alarmCatRepo.findById(1L).orElseThrow())
	            .receiver(host)
	            .content(content)
	            .status(false)
	            .build();
	    alarmRepo.save(alarm);

	    messagingTemplate.convertAndSendToUser(hostUsername, "/queue/alert", content);

	    log.info("알림 전송 및 저장 완료: {} → {}", cancellerNickname, hostUsername); // 로그 메시지 한글화
	}


	
	public Reservation changeStatusTodelete(Long id) {
		log.info("예약 상태 삭제로 변경 요청: 예약 ID={}", id); // 로그 메시지 한글화
		
		Reservation entity = reserveRepo.findById(id).orElseThrow();
		entity.setStatus(5);
		entity = reserveRepo.save(entity);
		log.info("예약 엔티티 상태 삭제(5)로 변경 완료: {}", entity); // 로그 메시지 한글화
		return entity;
	}
	
 // Reservation 엔티티를 ReservationCardDto로 변환하는 헬퍼 메서드
 public ReservationCardDto convertToMyReservationDto(Reservation reservation) {
     log.info("ReservationCardDto 변환 요청: 예약={}", reservation); // 로그 메시지 한글화
     ReservationCardDto dto = new ReservationCardDto();

     if (reservation.getRetalCategory() != null) {
         dto.setRentalCategoryId(reservation.getRetalCategory().getId());
     }

     dto.setReservationId(reservation.getId());
     dto.setRenterNickname(reservation.getRenter().getNickname());
     dto.setRenterId(reservation.getRenter().getId());

     Product product = reservation.getProduct();
     Long productId = product.getId();
     dto.setProductId(productId);
     dto.setProductName(product.getProductName());

     LocalDateTime startTime = reservation.getStartTime();
     LocalDateTime endTime = reservation.getEndTime();
     DateTimeFormatter formatStart = DateTimeFormatter.ofPattern("yyyy-MM-dd");
     dto.setRentalDate(startTime.format(formatStart));

     DateTimeFormatter formatRange = DateTimeFormatter.ofPattern("HH:mm");
     String rentalTimeRange = startTime.format(formatRange) + " ~ " + endTime.format(formatRange);
     dto.setRentalTimeRange(rentalTimeRange);

     long minutes = Duration.between(startTime, endTime).toMinutes();
     Long fee = product.getFee() * minutes;
     String formattedFee = NumberFormat.getInstance().format(fee);
     dto.setFee(formattedFee);

     // S3 Pre-signed URL을 가져와서 imageUrl 설정
     List<RentalImageDto> images = rentalImgServ.readByProductId(productId);
     String imageUrl = images.isEmpty() ? "/images/default-product.png" : images.get(0).getImageUrl();
     dto.setImageUrl(imageUrl);

     dto.setStatus(reservation.getStatus());

     return dto;
 }
 
 // ReservationDto로 변환하는 헬퍼 메서드 (예약 현황 API 등에서 사용)
 private ReservationDto convertToReservationDto(Reservation reservation) {
     log.info("ReservationDto 변환 요청: 예약={}", reservation); // 로그 메시지 한글화
     ReservationDto dto = new ReservationDto();
     dto.setId(reservation.getId());
     
     if(reservation.getProduct() != null && reservation.getProduct().getRentalCategory() != null) {
         dto.setRentalCategoryId(reservation.getProduct().getRentalCategory().getId());
     }
     dto.setProductName(reservation.getProduct().getProductName());

     // S3 Pre-signed URL을 가져와서 imageUrl 설정
     List<RentalImageDto> images = rentalImgServ.readByProductId(reservation.getProduct().getId());
     if (images != null && !images.isEmpty()) {
         dto.setImageUrl(images.get(0).getImageUrl());
     } else {
         dto.setImageUrl("/images/default-product.png");
     }
     
     dto.setFee(reservation.getProduct().getFee());
     
     DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
     DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
     dto.setRentalDate(reservation.getStartTime().format(dateFormatter));
     dto.setRentalTimeRange(reservation.getStartTime().format(timeFormatter) + " ~ " + reservation.getEndTime().format(timeFormatter));

     dto.setStatus(reservation.getStatus());
     dto.setChatRoomId(null);
     dto.setProductOwnerId(reservation.getProduct().getUser().getId());
     dto.setRenterNickname(reservation.getRenter().getNickname());
     dto.setRenterId(reservation.getRenter().getId());
     dto.setProductId(reservation.getProduct().getId());

     dto.setDetails("대여 날짜: " + dto.getRentalDate());
     
     return dto;
 }

	public Optional<Reservation> findById(Long reservationId) {
     log.info("예약 ID로 조회 요청: 예약 ID={}", reservationId); // 로그 메시지 한글화
     return reserveRepo.findById(reservationId);
 }
	
	// 새로 추가: productId로 가장 최근 예약을 찾는 메서드
 public Reservation findByProductId(Long productId) {
     log.info("상품 ID로 예약 찾기 요청: 상품 ID={}", productId); // 로그 메시지 한글화
     
     List<Reservation> reservations = reserveRepo.findByProductId(productId);
     if (reservations.isEmpty()) {
         log.warn("상품 ID {}에 해당하는 예약이 없습니다.", productId); // 로그 메시지 한글화
         return null;
     }
     
     return reservations.stream()
             .sorted((r1, r2) -> r2.getCreatedTime().compareTo(r1.getCreatedTime()))
             .findFirst()
             .orElse(null);
 }
 
 @Transactional(readOnly = true)
 public List<ReservationDto> readAllExceptStatuses(Long productId, List<Integer> statuses) {
     log.info("상품 ID {}의 특정 상태({})를 제외한 모든 예약 조회 요청.", productId, statuses); // 로그 메시지 한글화
     return reserveRepo.findByProductIdAndStatusNotIn(productId, statuses).stream()
             .map(this::convertToReservationDto)
             .collect(Collectors.toList());
 }
 
 @Transactional(readOnly = true) // 데이터베이스 읽기만 수행하므로 readOnly
 public boolean checkIfReservationConflicts(ReservationCreateDto newReservationDto, User currentUser) {
     log.info("예약 충돌 확인 요청: 상품 ID={}, 사용자={}", newReservationDto.getProductId(), currentUser.getUsername()); // 로그 메시지 한글화
     List<Reservation> existingReservations = reserveRepo.findByProductId(newReservationDto.getProductId());

     boolean isConflict = existingReservations.stream().anyMatch(existingReserv -> {
         Product product = prodRepo.findById(newReservationDto.getProductId())
                                   .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
         Reservation newReservationEntity = newReservationDto.toEntity(product);
         newReservationEntity.setRenter(currentUser);
         return existingReserv.isOverlapping(newReservationDto);
     });

     log.info("예약 충돌 확인 결과: 상품 ID={}, 충돌 여부={}", newReservationDto.getProductId(), isConflict); // 로그 메시지 한글화
     return isConflict;
 }

}