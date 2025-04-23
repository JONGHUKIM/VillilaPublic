package com.splusz.villigo.service;

import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.dto.ReservationCardDto;
import com.splusz.villigo.repository.ProductRepository;
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
	
	public Reservation read(Long id) {
		log.info("read(id={})", id);
		
		Reservation reservation = reserveRepo.findById(id).orElseThrow();
		return reservation;
	}
	
	public List<Reservation> readAll(Long productId) {
		log.info("readAll(productId={})", productId);
		
		List<Reservation> result = reserveRepo.findByProductId(productId);
		result.forEach(System.out::println);
		return result;
	}
	
	public Page<Reservation> readAllByUserId(Long userId, int pageNo, Sort sort) {
		log.info("readAllByUserId(userId={}, pageNo={}, sort={})", userId, pageNo, sort);
		
		Pageable pageable = PageRequest.of(pageNo, 5, sort);
		Page<Reservation> page = reserveRepo.findByProductUserId(userId, pageable);
		page.forEach(System.out::println);		
		return page;
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
	
	public void delete(Long id) {
		log.info("delete(id={})");
		
		reserveRepo.deleteById(id);
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
	
	public ReservationCardDto convertToMyReservationDto(Reservation reservation) {
		log.info("createMyReservationDto(reservation={})", reservation);
		ReservationCardDto dto = new ReservationCardDto();
		
		dto.setRentalCategoryId(reservation.getRetalCategory().getId());
		
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
		
		// 대여 시간 범위 계산(예: 10:00 ~ 16:00)
		DateTimeFormatter formatRange = DateTimeFormatter.ofPattern("HH:mm");
		String rentalTimeRange = startTime.format(formatRange) + " ~ " + endTime.format(formatRange);
		dto.setRentalTimeRange(rentalTimeRange);
		
		// 대여 요금 계산
		long minutes = Duration.between(startTime, endTime).toMinutes(); // minutes: 대여 시간
		Long fee = product.getFee() * minutes; // 대여 요금(총 금액)
		String formattedFee = NumberFormat.getInstance().format(fee);
		dto.setFee(formattedFee);
		
		// 이미지 경로
		List<RentalImage> images = rentalimgRepo.findByProductId(productId);
		String imagePath = images.isEmpty() ? null : images.get(0).getFilePath();
		dto.setImagePath(imagePath);
		
		// 예약 상태
		dto.setStatus(reservation.getStatus());
		
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
    
    public List<Reservation> readAllExceptStatuses(Long productId, List<Integer> statuses) {
        log.info("readAllExceptStatuses(productId={}, statuses={})", productId, statuses);
        List<Reservation> result = reserveRepo.findByProductIdAndStatusNotIn(productId, statuses);
        result.forEach(System.out::println);
        return result;
    }

}
