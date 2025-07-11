package com.splusz.villigo.web;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.splusz.villigo.domain.Alarm;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.AlarmListDto;
import com.splusz.villigo.service.AlarmService;
import com.splusz.villigo.service.ChatService;
import com.splusz.villigo.service.ReservationService;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class AlarmController {
	
	private final AlarmService alarmService;
	private final ReservationService reserveService;
	private final ChatService chatService;
	private final SimpMessagingTemplate messagingTemplate;

	// 모든 알람들을 PagedModel로 리턴
	@GetMapping("/alarm/list")
	public ResponseEntity<List<Object>> getAlarmListAll(@AuthenticationPrincipal Object principal, // Object로 변경
			@RequestParam(name="p", defaultValue = "0") int pageNo) {
		
		User user = SecurityUserUtil.getUserFromPrincipal(principal); // 헬퍼 메서드 사용
		
		// 인증되지 않았거나 유효한 User 객체를 얻지 못한 경우
		if (user == null || user.getId() == null) {
			log.warn("getAlarmListAll: 인증되지 않은 사용자 또는 User 객체/ID를 가져올 수 없습니다. 401 Unauthorized 반환.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
		}

		log.info("getAlarmListAll(user ID={})", user.getId());
		long userId = user.getId();
		Page<Alarm> alarms = alarmService.read(
				userId, pageNo, Sort.by("createdTime").descending());
		Page<AlarmListDto> page = alarms.map(AlarmListDto::new);
		long unreadChatMessages = chatService.countAllUnreadMessages(userId); // 안 읽은 채팅 개수
		List<Object> alarmPopUpObject = new ArrayList<Object>();
		alarmPopUpObject.add(new PagedModel<>(page));
		alarmPopUpObject.add(unreadChatMessages);

		return ResponseEntity.ok(alarmPopUpObject);
	}
	
	// 읽지 않은 알람들을 PagedModel로 리턴
	@GetMapping("/alarm/list/preforward")
	public ResponseEntity<List<Object>> getAlarmList(@AuthenticationPrincipal Object principal, // Object로 변경
			@RequestParam(name="p", defaultValue = "0") int pageNo) {
		
		User user = SecurityUserUtil.getUserFromPrincipal(principal); // 헬퍼 메서드 사용

		// 인증되지 않았거나 유효한 User 객체를 얻지 못한 경우
		if (user == null || user.getId() == null) {
			log.warn("getAlarmList (Unread): 인증되지 않은 사용자 또는 User 객체/ID를 가져올 수 없습니다. 401 Unauthorized 반환.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); 
		}

		log.info("getAlarmList (Unread)(user ID={})", user.getId());
		long userId = user.getId();
		Page<Alarm> alarms = alarmService.readAlarmStatusFalse(
				userId, pageNo, Sort.by("createdTime").descending());
		Page<AlarmListDto> page = alarms.map(AlarmListDto::new);
		long unreadChatMessages = chatService.countAllUnreadMessages(userId); // 안 읽은 채팅 개수
		List<Object> alarmPopUpObject = new ArrayList<Object>();
		alarmPopUpObject.add(new PagedModel<>(page));
		alarmPopUpObject.add(unreadChatMessages);

		return ResponseEntity.ok(alarmPopUpObject);
	}
	
	// 알람의 상태를 읽지 않음(false)에서 읽음(true)로 변경
	@GetMapping("/alarm/check/{alarmId}")
	public ResponseEntity<Long> checkAlarm(@PathVariable(name = "alarmId") Long alarmId) {
	    log.info("checkAlarm(id={})", alarmId);
	    try {
	        alarmService.updateAlarmStatus(alarmId);
	        return ResponseEntity.ok(alarmId);
	    } catch (NoSuchElementException e) {
	        log.error("ID가 {}인 알림을 찾을 수 없거나 유효하지 않은 ID", alarmId, e);
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	    } catch (Exception e) {
	        log.error("ID가 {}인 알림을 확인하는 중 오류가 발생", alarmId, e);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
	    }
	}
	
	// 알람을 삭제
	@GetMapping("/alarm/delete/{alarmId}")
	public ResponseEntity<Integer> deleteAlarm(@PathVariable(name = "alarmId") Long alarmId) {
		log.info("deleteAlarm(id={})", alarmId);
		
		int result = alarmService.deleteAlarm(alarmId);
		return ResponseEntity.ok(result);
	}
	
	
	// [TEST용] 클라이언트에서 /app/rent로 메시지 보내면 실행
	@MessageMapping("/rent")
	public void notifyToOwner() {
		log.info("notifyOwner()");
		
		Alarm a1 = Alarm.builder()
				.alarmCategory(alarmService.readAlarmCategory(1L))
				.reservation(reserveService.read(3L))
				.content("testv2 계정에게 발송되는 Test용 알람입니다.")
				.status(false)
				.build();
//		Long a1ProductOwnerId = a1.getReservation().getProduct().getUser().getId();
//		log.info("a1ProductOwnerId: {}", a1ProductOwnerId);
		
		// 실시간으로 특정 판매자에게 메시지 전달
		// * 7: testv2 계정 컬럼 아이디
		alarmService.sendNotification("testv2", a1.getContent());
		// messagingTemplate.convertAndSend("/topic/alert/" + a1ProductOwnerId, a1);
	}
	
	@GetMapping("/alarmtest")
	public String alarmTest(Model model, @AuthenticationPrincipal User user) {
		log.info("alarmTest(Principal(user)={})", user);
		
		if (user == null) {
            return "redirect:/login"; // 로그인되지 않은 경우 로그인 페이지로 이동.
        }
		model.addAttribute("userId", user.getId()); // model 객체에 로그인한 유저의 Id값 전달.
		
		return "/hstest/alarmtest";
	}
	

}
