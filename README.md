해당 프로젝트는 공개용 저장소이며, GitHub Actions를 활용한 CI/CD 파이프라인은 보안상의 이유로 비공개 저장소에 별도로 관리되고 있습니다.

&nbsp;

## 프로젝트 소개

&nbsp;

Villila는 고가의 명품, 슈퍼카를<br>
촬영 또는 체험 목적으로 개인 간 대여(P2P) 가능한 직거래 기반 고가 자산 공유 플랫폼입니다.

사용자는 자신이 보유한 고가 아이템을 등록하여 수익을 창출할 수 있으며, <br>
대여를 원하는 유저는 중고거래처럼 간단한 절차로 예약 → 채팅 → 거래 확정까지
원스톱으로 진행할 수 있습니다.

🛠️ 배포 링크: https://villila.store/

### 주요 기능📸 
<br>

<p>
  <img src="https://github.com/user-attachments/assets/291f57e1-99c2-4981-8392-53e6c35c0cd5" alt="기능 이미지 1" width="350"/>
  <img src="https://github.com/user-attachments/assets/b559eef0-638c-4d7c-bd38-7a0677f0ff79" alt="기능 이미지 2" width="350"/>
</p>

<p>
  <img src="https://github.com/user-attachments/assets/d4ce1ff8-09ab-4f94-a815-04ac0893b50e" alt="기능 이미지 3" width="350"/>
  <img src="https://github.com/user-attachments/assets/b0647a2c-e735-4045-be89-de06069f4601" alt="기능 이미지 4" width="350"/>
</p>

&nbsp;

## 구현 기능  

&nbsp;

1. **회원가입 / 로그인 ( 30% 기여 )**
   - 정규표현식을 활용한 아이디/비밀번호 유효성 검증
   - 아이디 중복 확인 로직 추가
   - 개인정보 처리 약관 및 마케팅 동의 기능 구현
   - Google OAuth2 기반의 소셜 회원가입/로그인 기능 연동

2. **게시물 검색  ( 20% 기여 )**
   - 카테고리별 검색 필터링 오류 수정
   - 지역 기반 검색 기능 오류 수정
   - 가격 범위 필터링 로직 개선
   - 페이지네이션 삭제 및 무한 스크롤로 전환

3. **게시물 등록, 수정, 삭제 (CRUD) ( 10% 기여 )**
   - 이미지 첨부 오류 수정 및 개선

4. **실시간 예약 시스템 및 알림 기능  ( 10% 기여 )**
   - 예약 알림 클릭 시 alarm ID가 undefined로 전달되던 오류 수정
     
5. **실시간 채팅 ( 100% 기여 )**
   - 실시간 채팅 기능 
   - 온라인/오프라인 표시  
   - 읽음 처리  
   - 이미지 첨부  
   - 입력 중 상태 표시  

6. **챗봇 기능 ( 100% 기여 )**

7. **웹사이트 내 화폐(JJAM) 충전, 사용 기능 (구현 예정)**
   - 구현중인 내용:
     - JJAM은 호스트가 상품을 광고할 때 필요한 기능으로 수정 중
     - JJAM은 예약 시 필요없음

### 기술 스택⚙️
<br>

- Backend: Java 21, Spring Boot, Spring Security, JPA
- Frontend: Thymeleaf, JavaScript, Bootstrap
- Database: MySQL (AWS RDS)
- DevOps: GitHub Actions, Docker, AWS EC2/S3/ECR
- Realtime: WebSocket, STOMP, SockJS
- Auth: OAuth2 (Google)

### 아키텍처🏗️

<br>

<img src="https://github.com/user-attachments/assets/06c12e78-3984-49ec-a0a7-092d472d81fa" alt="서버 아키텍처" width="720"/>

<br>
&nbsp;

## 트러블슈팅 🧩

&nbsp;

 ### - 오류 상황: 호스트가 예약 알림 메시지 클릭 시 Bad Request(400)
 - **오류 원인**
   - 예약 알림 메시지 클릭 시, 클라이언트측에서 서버로 `alarmId`를 넘겨야 하는데 `undefined`가 전달되고 있음
 - **해결 방안**
   - 클릭 시점에 알람이 렌더링 되지 않았으면 클릭 못하게 막음
   
      		
     		document.querySelectorAll('a.alarm-link').forEach(link => {
			    const alarmId = link.dataset.id;
			    if (!alarmId || alarmId === "undefined") {
			        link.style.pointerEvents = 'none';
			        link.style.opacity = '0.6';
			        console.warn("비정상 링크 감지: data-id 없음", link);
			    } else {
			        link.addEventListener('click', checkAlarm);
			    }
			});

<br>

   - **느낀점** <br>
   클라이언트와 서버 간의 데이터 전달 시 타입 일치 및 유효성 검증의 중요성을 느낌 <br>
   특히 API 호출 시 예상치 못한 `undefined` 값이나 잘못된 타입이 전달될 경우 <br>
   서버에서 `400 Bad Request`와 같은 오류가 발생하여 서비스의 안정성을 해칠 수 있음을 경험

<br>
<br>

 ### - 오류 상황: 채팅리스트가 계속 늘어나고 채팅방에 입장불가(405 Method Not Allowed), 온라인/오프라인 기능이 안됨
<br>
   
<p>
  <img src="https://github.com/user-attachments/assets/2d11a9dd-9638-43c7-9621-b3e9ac3d966e" alt="트러블슈팅 이미지 1" width="350"/>
  <img src="https://github.com/user-attachments/assets/d87a394e-73d4-47c0-a24f-d19f57842640" alt="트러블슈팅 이미지 2" width="350"/>
</p>

<br>

 - **오류 원인**
   - 채팅방 생성 요청(`/api/chat/rooms`)이 중복으로 발생
   - 채팅 리스트에서 userId를 전달받지 못하여 사용자 상태가 모두 오프라인으로 표시됨
 - **해결 방안**
   - 클라이언트측에서 먼저 중복 확인, `ensureChatRoom` 함수에서 <br>
     `chatRoomCreationLock` 이라는 `Map` 객체를 사용하여 <br>
     특정 두 사용자(`userId1, userId2`) 간의 채팅방 생성 요청이 이미 진행 중인지 확인

            async function ensureChatRoom(userId1, userId2) {
	            const key = `${userId1}-${userId2}`;
	            if (chatRoomCreationLock.has(key)) {
	                console.log(`이미 ${key}에 대한 채팅방 생성 요청 진행 중`);
	                return chatRoomCreationLock.get(key);
	            } (생략)

     <br>
   - 채팅방을 생성하기 전에 `/api/chat/rooms/find` 엔드포인트로 먼저 요청을 보내 <br>
     두 사용자 간에 이미 존재하는 채팅방이 있는지 조회
   - `chatRoomsCache`에 채팅방 객체를 추가하기 전에 동일한 id를 가진 채팅방이 이미 캐시에 있는지 확인 <br>
     이미 존재하면 추가하지 않고 클라이언트 메모리 내에서 중복된 채팅방 정보가 쌓이는 것을 방지
   - 서버측에서도 중복 확인 메서드 강화 `ChatRestController.createChatRoom`, `ChatService.createChatRoom`
   - 프론트엔드에서는 `/topic/userStatus`를 구독하고 있지만 <br>
     `userId`가 문자열로 전송되고 그 과정에서 문제가 발생하여 <br>
     `const userId = parseInt(statusUpdate.userId);` 숫자로 변환하여 전송 <br>
     
     <br>
     
   - [chat.js](https://github.com/JONGHUKIM/VillilaPublic/blob/main/villigo/src/main/resources/static/js/chat.js)
   - [ChatRestController](https://github.com/JONGHUKIM/VillilaPublic/blob/main/villigo/src/main/java/com/splusz/villigo/web/ChatRestController.java)
   - [ChatService](https://github.com/JONGHUKIM/VillilaPublic/blob/main/villigo/src/main/java/com/splusz/villigo/service/ChatService.java)

     <br>
- **느낀점** <br>
   이번 트러블슈팅을 통해 복잡한 실시간 통신 환경에서 <br>
   클라이언트-서버 간의 동시성 및 데이터 일관성 관리가 핵심임을 느낌 <br>

   특히 채팅방 중복 생성 방지를 위한 캐싱 전략(`chatRoomsCache`, `chatRoomCreationLock`)의 중요성과 <br>
   정확한 데이터 타입 변환(`parseInt`)이 기능의 안정성과 사용자 경험에 직결됨을 배움 <br>
   문제의 근본 원인을 찾아 구조적으로 해결하는 개발 습관의 필요성을 다시 한번 느낌

&nbsp;

## 저작권 안내  

본 프로젝트는 비상업적 포트폴리오 목적으로만 공개되었으며,  
무단 복제 및 상업적 사용을 금지합니다.  

This project is publicly shared for non-commercial portfolio purposes only.  
Unauthorized reproduction or commercial use is strictly prohibited.  
Any commercial use without explicit permission from the creator will be considered a violation of intellectual property rights.
