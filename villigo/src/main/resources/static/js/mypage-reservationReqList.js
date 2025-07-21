/**
 * mypage.html 파일에 포함
 */
document.addEventListener('DOMContentLoaded', () => {
    // 현재 페이지 번호 -> [더보기] 버튼 생성용
    let currentPageNo = 0;
    // 더보기 버튼
    const btnMore = document.getElementById('btnMore');
    
    getAllReservationRequests();
    btnMore.addEventListener('click', () => getAllReservationRequests(currentPageNo + 1));
    
    // 쿼리 스트링에 dest=showtab0이 있으면 쿼리 스트링에 따라 탭을 자동 클릭
    const queryParams = new URLSearchParams(window.location.search);
    const dest = queryParams.get('dest');
    switch (dest) {
        case 'showtab1':
            document.querySelector('div#showtab1').click();
            break;
        case 'showtab2':
            document.querySelector('div#showtab2').click();
            break;
        case 'showtab4':
            document.querySelector('div#showtab4').click();
            break;
    }
    
    /* ------------------------------- 함수 선언 ------------------------------- */
    function getAllReservationRequests(pageNo = 0) {
        const url = `/reservation/api/requestlist?p=${pageNo}`;
        
        axios
        .get(url)
        .then(({ data }) => {
            console.log(data);
            currentPageNo = data.page.number; // 현재 예약 현황의 페이지 번호
            console.log('현재 페이지(page.number): ', data.page.number, 
                ', 마지막 페이지: ', data.page.totalPages);
            makeReservationReqElements(data);   
        })
        .catch((error) => console.log(error));
    }
    
	function makeReservationReqElements({content, page}) { // content는 List<ReservationDto>
	    const divReservationReqList = document.getElementById('reservationReqList');
	    
	    divReservationReqList.innerHTML = ''; // HTML 내용 초기화 (딱 한 번)
	    let htmlStr = ''; // 동적으로 생성할 HTML 문자열

	    // 데이터가 아예 없는 경우 (컨텐츠 배열이 비어있으면)
	    if (content.length === 0) {
	        divReservationReqList.innerHTML = '들어온 예약 신청이 없습니다.';
	        document.getElementById('btnMore').style.display = 'none'; // 더보기 버튼 숨김
	        return; // 함수 종료
	    }
	            
	    // DTO의 데이터를 이용하여 예약 현황 카드 생성 (content가 비어있지 않은 경우에만 실행)
	    for (const dto of content) { // dto는 ReservationDto 객체
	        // 예약의 status가 5(삭제처리됨)이면 예약카드를 생성하지 않음
	        if (dto.status === 5) {
	            continue; 
	        }
	        
	        console.log('상품 카테고리 id: ', dto.rentalCategoryId);
	        let postDetailsUrl = '/post/details';
	        switch (dto.rentalCategoryId) {
	            case 1: postDetailsUrl += `/bag?id=${dto.productId}`; break;
	            case 2: postDetailsUrl += `/car?id=${dto.productId}`; break;
	            default: postDetailsUrl += `?id=${dto.productId}`; break;
	        }
	        
	        htmlStr += `
	            <div class="reservation-card">
	              <div class="res-img">
	                <a href="${postDetailsUrl}">
	                <img src="${dto.imageUrl}" alt="상품 이미지"> </a>
	              </div>
	              <div class="res-info">
	                <p class="car-name">
	                <a href="${postDetailsUrl}"><strong>${dto.productName}</strong></a>
	                </p>
	                <p><strong>대여 날짜:</strong> ${dto.rentalDate}</p>
	                <p><strong>대여 시간:</strong> ${dto.rentalTimeRange}</p>
	                <p><strong>요금:</strong> ${dto.fee} JJAM</p>
	                <p><strong>예약자:</strong> ${dto.renterNickname || '알 수 없음'}</p>
	        `;
	        // 예약 진행 상태(status)에 따라 버튼 추가
	        switch (dto.status) {
	            case 0: case 1:
	                htmlStr += `
	                    <div class="res-buttons">
	                      <button class="btn-decline" data-id="${dto.reservationId}" data-product-id="${dto.productId}">거절</button>
	                      <button class="btn-accept" data-id="${dto.reservationId}" data-product-id="${dto.productId}">수락</button>
	                      <button class="btn-chat" data-id="${dto.reservationId}">채팅</button>
	                    </div>
	                  </div>
	                </div>`;
	                break;
	            case 2:
	                htmlStr += `
	                    <div class="res-buttons">
	                      <button class="btn-chat" data-id="${dto.reservationId}">채팅</button>
	                    </div>
	                  </div>
	                </div>`;
	                break;
	            case 3:
	                htmlStr += `
	                    <div class="res-buttons">
	                      <button class="btn-complete" id="sendReviewToRenter" onclick="openCompletePopupForResReq(this)" 
	                          data-id="${dto.reservationId}" data-renter-id="${dto.renterId}">거래완료</button>
	                      <button class="btn-review d-none" disabled>후기 작성 완료</button> 
	                    </div>
	                  </div>
	                </div>`;
	                break;
	            case 4:
	                htmlStr += `
	                    <div class="res-buttons">
	                      <button class="btn-rejected" disabled>거절함</button>
	                      <button class="btn-delete-reserv" data-id="${dto.reservationId}">삭제</button>
	                    </div>
	                  </div>
	                </div>`;
	                break;
	            case 7:
	                htmlStr += `
	                    <div class="res-buttons">
	                        <button class="btn-review" disabled>후기 작성 완료</button> 
	                    </div>
	                  </div>
	                </div>`;
	                break;
	        }
	    }
	    
	    // 모든 HTML 문자열이 완성되면 한 번에 DOM에 삽입
	    divReservationReqList.innerHTML = htmlStr; 

	    // 더보기 버튼 로직 (page.number, page.totalPages 사용)
	    if (((page.number + 1) !== page.totalPages) && (page.totalPages !== 0)) {
	        document.getElementById('btnMore').style.display = 'block';    
	    } else {
	        document.getElementById('btnMore').style.display = 'none';
	    }
        
        // 표시해야하는 예약 현황이 없는 경우
        const contentLength = Object.keys(content).length;
        console.log('표시되는 예약 갯수: ', contentLength);
        if (contentLength === 0) {
            divReservationReqList.innerHTML = '들어온 예약 신청이 없습니다.';
        }

        // [거절], [수락], [삭제] 버튼을 찾고, click 이벤트 리스너를 설정
        const btnDecline = document.querySelectorAll('button.btn-decline');
        btnDecline.forEach((btn) => btn.addEventListener('click', declineReservation));

        const btnAccept = document.querySelectorAll('button.btn-accept');
        btnAccept.forEach((btn) => btn.addEventListener('click', acceptReservation));

        const btnDeleteReserv = document.querySelectorAll('button.btn-delete-reserv');
        btnDeleteReserv.forEach((btn) => btn.addEventListener('click', deleteReservation));
        
		const btnChatList = document.querySelectorAll('button.btn-chat');
		btnChatList.forEach((btn) => {
		    btn.addEventListener('click', function () {
		        const roomId = this.getAttribute('data-room-id');
		        const reservationId = this.getAttribute('data-id');
		        const currentUserId = document.body.getAttribute('data-user-id');

		        if (roomId) {
		            // 이미 채팅방이 존재하면 바로 이동
		            window.location.href = `/chat?chatRoomId=${roomId}`;
		            return;
		        }

		        if (!reservationId || !currentUserId) {
		            alert("예약 정보 또는 사용자 정보가 누락되었습니다.");
		            return;
		        }

		        // 없으면 서버에 요청하여 채팅방 생성 or 조회
		        axios.post('/api/chat/rooms/by-reservation', null, {
		            params: { reservationId, currentUserId }
		        })
		        .then(response => {
		            const chatRoomId = response.data.id;
		            window.location.href = `/chat?chatRoomId=${chatRoomId}`;
		        })
		        .catch(error => {
		            console.error("채팅방 열기 실패:", error);
		            alert("채팅방을 열 수 없습니다.");
		        });
		    });
		});

	}
    
    // "채팅" 버튼 클릭 시 호출되는 함수
    function openChatRoomByReservation(event) {
        const reservationId = event.target.getAttribute('data-id');
        if (!reservationId) {
            console.error('reservationId를 찾을 수 없습니다.');
            alert('예약 정보를 찾을 수 없습니다.');
            return;
        }

        // 현재 사용자 ID (HTML에서 가져옴)
        const currentUserId = document.body.getAttribute('data-user-id');
        if (!currentUserId) {
            console.error('currentUserId를 가져올 수 없습니다.');
            alert('사용자 정보를 가져올 수 없습니다. 로그인 상태를 확인해주세요.');
            return;
        }

        // 백엔드 API 호출
        axios.post('/api/chat/rooms/by-reservation', null, {
            params: { reservationId, currentUserId }
        })
        .then(response => {
            const chatRoom = response.data;
            const chatRoomId = chatRoom.id;
            console.log('채팅방 ID:', chatRoomId);
            // 채팅 페이지로 이동
            window.location.href = `/chat?chatRoomId=${chatRoomId}`;
        })
        .catch(error => {
            console.error('채팅방 생성/조회 실패:', error);
            if (error.response && error.response.status === 403) {
                alert('이 예약에 대한 채팅 권한이 없습니다.');
            } else {
                alert('채팅방을 여는 데 실패했습니다.');
            }
        });
    }
    
    // 예약 거절 요청 처리 함수
    function declineReservation(event) {
        const check = confirm('예약을 거절하시겠습니까?\n거절 후에는 취소가 불가능합니다.');
        if (!check) {
            return;
        }
        
        const uri = `/reservation/refuse/${event.target.getAttribute('data-id')}/${event.target.getAttribute('data-product-id')}`;
        
        axios
        .get(uri)
        .then((response) => {
            console.log('거절 요청 처리 결과: ', response.data);
            alert('예약이 거절되었습니다.');
            getAllReservationRequests(0);
        })
        .catch((error) => console.log(error));
    }
    
    // 예약 수락 요청 처리 함수
    function acceptReservation(event) {
        const check = confirm('예약을 수락하시겠습니까?');
        if (!check) {
            return;
        }
        
        const uri = `/reservation/confirm/${event.target.getAttribute('data-id')}/${event.target.getAttribute('data-product-id')}`;
        
        axios
        .get(uri)
        .then((response) => {
            console.log('수락 요청 처리 결과: ', response.data);
            alert('예약이 수락되었습니다.\n채팅으로 대여자와 세부사항을 조율해보세요!');
            getAllReservationRequests(0);
        })
        .catch((error) => console.log(error));
    }
    
    // 예약 삭제 요청 처리 함수
    function deleteReservation(event) {
        const check = confirm('예약을 삭제하시겠습니까?\n삭제된 예약은 되돌릴 수 없습니다.');
        if (!check) {
            return;
        }
        
        const uri = `/reservation/delete/${event.target.getAttribute('data-id')}`;
        
        axios
        .delete(uri)
        .then((response) => {
            console.log('삭제된 예약 id: ', response.data);
            alert(`예약이 삭제되었습니다.\n삭제된 예약 ID: ${response.data}`);
            getAllReservationRequests(0);
        })
        .catch((error => console.log(error)));
    }
});