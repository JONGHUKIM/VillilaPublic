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
		log.info("read(id={})", id);
		
		Reservation reservation = reserveRepo.findById(id).orElseThrow();
		return reservation;
	}
	
	@Transactional(readOnly = true)
	public List<ReservationDto> readAll(Long productId) { // <--- 반환 타입이 ReservationDto 리스트여야 함
	    return reserveRepo.findByProductId(productId).stream()
	            .map(this::convertToReservationDto) // <--- 여기서 호출
	            .collect(Collectors.toList());
	}
	
    @Transactional(readOnly = true)
    public Page<ReservationDto> readAllByUserId(Long userId, int pageNo, Sort sort) {
        Pageable pageable = PageRequest.of(pageNo, 5, sort);
        Page<Reservation> page = reserveRepo.findByProductUserId(userId, pageable);
        
        List<ReservationDto> dtoList = page.getContent().stream()
                                            .map(this::convertToReservationDto)
                                            .collect(Collectors.toList());
        return new PageImpl<>(dtoList, pageable, page.getTotalElements());
    }

	public Reservation create(Reservation entity) {
		log.info("create(entity={})", entity);
		
		entity = reserveRepo.save(entity);
		return entity;
	}
	
	public Reservation update(Long id, int status) {
		log.info("update(id={}, status={})", id, status);
		
		Reservation entity = reserveRepo.findById(id).orElseThrow();
		// 엔터티의 status 필드를 변경 후 저장
		entity.setStatus(status);
		entity = reserveRepo.save(entity);
		log.info("updated entity: {}", entity);
		return entity;
	}
	
	@Transactional
	public void delete(Long id) {
	    log.info("delete(id={})", id);

	    Reservation reservation = reserveRepo.findById(id).orElseThrow();
	    int status = reservation.getStatus();
	    if (!(status == 0 || status == 1 || status == 5)) {
	        throw new IllegalStateException("현재 상태에서는 예약을 취소/삭제할 수 없습니다.");
	    }

	    // 필요한 데이터 미리 꺼내기
	    String cancellerNickname = reservation.getRenter().getNickname();
	    User host = reservation.getProduct().getUser();
	    String productName = reservation.getProduct().getProductName();
	    String hostUsername = host.getUsername();

	    // CHAT_ROOM_RESERVATIONS 삭제
	    chatRoomReservationRepo.deleteByReservationId(id);

	    // 예약 삭제
	    reserveRepo.deleteById(id);

	    // 알림 저장 및 전송
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

	    log.info("알림 전송 및 저장 완료: {} → {}", cancellerNickname, hostUsername);
	}


	
	public Reservation changeStatusTodelete(Long id) {
		log.info("changeStatusTodelete(id={})");
		
		Reservation entity = reserveRepo.findById(id).orElseThrow();
		// 엔터티의 status 필드를 5(삭제)로 변경 후 저장
		entity.setStatus(5);
		entity = reserveRepo.save(entity);
		log.info("entity changed to delete: {}", entity);
		return entity;
	}
	
    // Reservation 엔티티를 ReservationCardDto로 변환하는 헬퍼 메서드
    public ReservationCardDto convertToMyReservationDto(Reservation reservation) {
        log.info("convertToMyReservationDto(reservation={})", reservation);
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
        List<RentalImageDto> images = rentalImgServ.readByProductId(productId); // RentalImageService 사용
        String imageUrl = images.isEmpty() ? "/images/default-product.png" : images.get(0).getImageUrl();
        dto.setImageUrl(imageUrl);

        dto.setStatus(reservation.getStatus());

        return dto;
    }
    
    // ReservationDto로 변환하는 헬퍼 메서드 (예약 현황 API 등에서 사용)
    private ReservationDto convertToReservationDto(Reservation reservation) {
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

        dto.setDetails("대여 날짜: " + dto.getRentalDate());
        
        return dto;
    }

	public Optional<Reservation> findById(Long reservationId) {
        return reserveRepo.findById(reservationId);
    }
	
	// 새로 추가: productId로 가장 최근 예약을 찾는 메서드
    public Reservation findByProductId(Long productId) {
        log.info("findByProductId(productId={})", productId);
        
        // productId로 예약을 조회하고, createdAt 기준으로 최신순 정렬 후 첫 번째 반환
        List<Reservation> reservations = reserveRepo.findByProductId(productId);
        if (reservations.isEmpty()) {
            log.warn("productId {}에 해당하는 예약이 없습니다.", productId);
            return null; // 또는 예외를 던질 수 있음: throw new IllegalArgumentException("예약이 없습니다.");
        }
        
        // 가장 최근 예약 반환 (BaseTimeEntity의 createdAt 사용 가정)
        return reservations.stream()
                .sorted((r1, r2) -> r2.getCreatedTime().compareTo(r1.getCreatedTime())) // 최신순 정렬
                .findFirst()
                .orElse(null);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationDto> readAllExceptStatuses(Long productId, List<Integer> statuses) {
        return reserveRepo.findByProductIdAndStatusNotIn(productId, statuses).stream()
                .map(this::convertToReservationDto)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true) // 데이터베이스 읽기만 수행하므로 readOnly
    public boolean checkIfReservationConflicts(ReservationCreateDto newReservationDto, User currentUser) {
        // 현재 상품에 대한 모든 예약 엔티티를 조회
        // DTO가 아닌 실제 Reservation 엔티티 리스트를 가져와야 isOverlapping 사용 가능
        List<Reservation> existingReservations = reserveRepo.findByProductId(newReservationDto.getProductId());

        // 조회된 예약들과 새로운 예약 요청(dto)이 겹치는지 확인
        boolean isConflict = existingReservations.stream().anyMatch(existingReserv -> {
            Product product = prodRepo.findById(newReservationDto.getProductId())
                                      .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));
            Reservation newReservationEntity = newReservationDto.toEntity(product);
            newReservationEntity.setRenter(currentUser); // isOverlapping에 Renter 정보가 필요할 경우 설정
            return existingReserv.isOverlapping(newReservationDto); // Reservation 엔티티의 isOverlapping 메서드 사용
        });

        log.info("checkIfReservationConflicts: productId={}, isConflict={}", newReservationDto.getProductId(), isConflict);
        return isConflict;
    }

}
