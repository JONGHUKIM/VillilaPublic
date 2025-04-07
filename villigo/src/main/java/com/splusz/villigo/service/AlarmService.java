package com.splusz.villigo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.AlarmCategory;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ReservationCreateDto;
import com.splusz.villigo.repository.AlarmCategoryRepository;
import com.splusz.villigo.repository.AlarmRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.ReservationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class AlarmService {
	
	private final AlarmRepository alarmRepo;
	private final ProductRepository productRepo;
	private final AlarmCategoryRepository alarmCatRepo;
	private final ReservationRepository reserveRepo;
	private final SimpMessagingTemplate messagingTemplate;
	private final SimpUserRegistry userRegistry;
	
	// 현재 연결된 사용자 확인용
	public void checkConnectedUsers() {
		Set<SimpUser> users = userRegistry.getUsers();
		log.info("현재 연결된 사용자 수: {}", users.size());
		
		for (SimpUser user : users) {
			log.info("사용자: {}, 세션 수: {}", user.getName(), user.getSessions().size());
	        for (SimpSession session : user.getSessions()) {
	            log.info("  세션 ID: {}", session.getId());
	            log.info("  구독 정보: {}", session.getSubscriptions());
	        }
		}
	}
	
	// 알람 생성
	public Alarm create(Alarm alarm) {
		log.info("create(alarm={})", alarm);
		
		Alarm entity = alarmRepo.save(alarm);
		return entity;
	}
	
	// 유저 id로 알람 목록 불러오기(List)
	public List<Alarm> read(Long userId) {
		log.info("read(userId={})", userId);
		
		return alarmRepo.findByReceiverId(userId);
	}
	
	// 유저 id로 알람 목록 불러오기(Page)
	public Page<Alarm> read(Long userId, int pageNo, Sort sort) {
		log.info("read(userId={}, pageNo={}, sort={})", userId);
		
		Pageable pageable = PageRequest.of(pageNo, 5, sort);
		Page<Alarm> page = alarmRepo.findByReceiverId(userId, pageable);
		page.forEach(System.out::println);
		
		return page;
	}
	
	// 안읽은 알람 목록 불러오기
	public Page<Alarm> readAlarmStatusFalse(Long userId, int pageNo, Sort sort) {
		log.info("readAlarmStatusFalse(userId={}, pageNo={}, sort={})", userId, pageNo, sort);
		
		Pageable pageable = PageRequest.of(pageNo, 5, sort);
		Page<Alarm> page = alarmRepo.findByReceiverIdAndStatus(userId, pageable);
		page.forEach(System.out::println);
		
		return page;
	}
	
	// 안읽은 알람 개수 불러오기
	public int countAlarmStatusFalse(Long userId) {
		log.info("readAlarmStatusFalse(userId={})", userId);

		List<Alarm> alarms = alarmRepo.findByReceiverIdAndStatus(userId);
		int count = alarms.size();
		log.info("{} 사용자의 안읽은 알람 개수: {}", userId, count);
		
		return count;
	}
	
	// 알람 상태 변경(안읽음 -> 읽음)
	public void updateAlarmStatus(Long id) {
		log.info("updateAlarmStatus(id={})", id);
		
		Alarm entity = alarmRepo.findById(id).orElseThrow();
		entity.setStatus(true);
		entity = alarmRepo.save(entity);
		log.info("상태 변경된 알람: {}", entity);
	}
	
	
	/**
	 * 웹소캣을 통해 알람을 실시간으로 전송합니다. 알람은 토스트 형식으로 표시 됩니다.
	 * 
	 * @param ownerUsername 알람을 수신할 사용자(User)의 username
	 * @param message 알람에 들어갈 텍스트
	 */
	public void sendNotification(String ownerUsername, String message) {
		log.info("sendNotification(ownerId={}, message={})", ownerUsername, message);
		
		// 현재 세션 정보 로깅
		SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
		log.debug("메시지 전송 전 헤더 확인: {}", accessor);
		
		// 현재 연결된 사용자 확인
		checkConnectedUsers();
		
		// /user/{username}/queue/alert 구독한 사용자에게 전송
		messagingTemplate.convertAndSendToUser(ownerUsername, "/queue/alert", message);
		log.info("Id가 {}인 유저에게 전송된 메시지: {}", ownerUsername, message);
	}
	
	// 알람 카테고리 검색
	public AlarmCategory readAlarmCategory(Long id) {
		log.info("readAlarmCategory(id={})", id);
		
		AlarmCategory alarmCategory = alarmCatRepo.findById(id).orElseThrow();
		return alarmCategory;
	}
	
	// 예약 추가 알람
	public Alarm reservationCreatedAlarmBuilder(ReservationCreateDto dto, User user) {
		log.info("reservationCreatedAlarmBuilder(dto={}, user={})", dto, user);

		AlarmCategory alarmCategory = alarmCatRepo.findById(1L).orElseThrow(); // 예약 현황
    	Long productId = dto.getProductId();
    	Product product = productRepo.findById(productId).orElseThrow();
    	RentalCategory rentalCategory = product.getRentalCategory(); 
    	log.info("product: {}, rentalCategory: {}", product, rentalCategory);
    	Reservation entity = dto.toEntity(product);
    	entity.setRenter(user);
    	entity.setRetalCategory(rentalCategory);
    	entity = reserveRepo.save(entity); // 예약 테이블에 예약 데이터를 추가
    	log.info("saved reservation: {}", entity);
    	
    	User owner = product.getUser();
    	
    	// 알람 메세지용 yyyy-MM-dd hh:mm 포맷 지정
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String startTime = dto.getStartTime().format(formatter);
        String endTime = dto.getEndTime().format(formatter);

    	String alarmText = "📦 새로운 예약신청이 도착했습니다.<br>"
    				+ "<strong>" + product.getProductName() + "</strong><br>"
    				+ "대여기간: " + startTime + " ~ " + endTime + "<br>"
    				+ "예약현황을 확인해보세요!";
    	
    	return Alarm.builder()
    			.alarmCategory(alarmCategory) // 알람카테고리(1: 대여)
    			.reservation(entity)
    			.receiver(owner)
    			.content(alarmText)
    			.status(false)
    			.build();	
	}
	
	
	// 예약 수락 알람
	public Alarm reservationConfirmAlarmBuilder(Long productId, Reservation entity) {
		log.info("reservationConfirmAlarmBuilder(productId={}, entity={})", productId, entity);
		
		AlarmCategory alarmCategory = alarmCatRepo.findById(2L).orElseThrow(); // 나의 예약
		// renter: 상품을 대여한 사용자
		User renter = entity.getRenter();
		// Product 엔터티를 이용하여 상품 주인의 nickname과 상품의 게시글 제목을 검색
		Product product = productRepo.findById(productId).orElseThrow();
		String owner = product.getUser().getNickname();
		String postName = product.getPostName();
		// 알람에 들어갈 내용
		String content = "✅ 예약이 확정되었습니다.<br>"
					+ owner.toString() + "님이 " + "<strong>" + postName + "</strong>" + " 예약을 확정했어요.<br>"
					+ "나의 예약을 확인해보세요!";
		
		return Alarm.builder()
				.alarmCategory(alarmCategory)
				.reservation(entity)
				.receiver(renter)
				.content(content)
				.status(false)
				.build();
	}
	
	// 예약 거절 알람
	public Alarm reservationRefuseAlarmBuilder(Long productId, Reservation entity) {
		log.info("reservationRefuseAlarmBuilder(productId={}, entity={})", productId, entity);
		
		AlarmCategory alarmCategory = alarmCatRepo.findById(2L).orElseThrow(); // 나의 예약
		// renter: 상품을 대여한 사용자
		User renter = entity.getRenter();
		// Product 엔터티를 이용하여 상품 주인의 nickname과 상품의 게시글 제목을 검색
		Product product = productRepo.findById(productId).orElseThrow();
		String owner = product.getUser().getNickname();
		String postName = product.getPostName();
		// 알람에 들어갈 내용
		String content = "📌 예약이 거절되었습니다.<br>"
					+ owner.toString() + "님이 " + "<strong>" + postName + "</strong>" + " 예약을 거절했어요.<br>"
					+ "나의 예약을 확인해보세요!";
		
		return Alarm.builder()
				.alarmCategory(alarmCategory)
				.reservation(entity)
				.receiver(renter)
				.content(content)
				.status(false)
				.build();
	}
	
	// 상품주인 -> 대여자 후기 알람
	public Alarm reservationReviewAlarmBuilder(Reservation entity) {
		log.info("reservationReviewAlarmBuilder(entity={})", entity);
		
		AlarmCategory alarmCategory = alarmCatRepo.findById(3L).orElseThrow(); // 후기
		// renterName: 상품을 대여한 사용자의 username
		User renter = entity.getRenter();
		String renterName = renter.getNickname();
		// Product 엔터티를 이용하여 상품 주인의 User 객체와 상품의 게시글 제목을 검색
		Product product = productRepo.findById(entity.getProduct().getId()).orElseThrow();
		User owner = product.getUser();
		String ownerName = owner.getNickname();
		String postName = product.getPostName();
		// 알람에 들어갈 내용
		String content = "📥 후기가 도착했습니다.<br>"
				        + renterName + "님에 대한 <strong>" + ownerName + "</strong> 님의 후기가 도착했어요.<br>" 
						+ "마이페이지에서 확인해보세요!";
		
		return Alarm.builder()
				.alarmCategory(alarmCategory)
				.reservation(entity)
				.receiver(renter)
				.content(content)
				.status(false)
				.build();
	}
	
	// 거래 완료 알람
	public Alarm reservationFinishAlarmBuilder(Reservation entity) {
		log.info("reservationFinishAlarmBuilder(entity={})", entity);
		
		AlarmCategory alarmCategory = alarmCatRepo.findById(1L).orElseThrow(); // 예약 현황
		// renterName: 상품을 대여한 사용자의 username
		String renterName = entity.getRenter().getUsername();
		// Product 엔터티를 이용하여 상품 주인의 User 객체와 상품의 게시글 제목을 검색
		Product product = productRepo.findById(entity.getProduct().getId()).orElseThrow();
		User owner = product.getUser();
		String postName = product.getPostName();
		// 알람에 들어갈 내용
		String content = "👏 거래가 완료되었습니다.<br>"  
				+ renterName + "님과의 <strong>" + postName + "</strong> 거래가 완료되었습니다.<br>"
				+ "거래는 어떠셨나요? 후기를 남겨주세요!";
		
		return Alarm.builder()
				.alarmCategory(alarmCategory)
				.reservation(entity)
				.receiver(owner)
				.content(content)
				.status(false)
				.build();
	}
	
	// 알람 삭제
	public int deleteAlarm(Long alarmId) {
		log.info("deleteAlarm(id={})", alarmId);
		
		alarmRepo.deleteById(alarmId);
		return 1;
	}
	
}
