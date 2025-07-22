/**
 * mypage.html 파일에 포함 - 무한스크롤 예약 목록 (수정된 버전)
 */
document.addEventListener('DOMContentLoaded', () => {
    let currentPageNo = 0;
    let isLoading = false;
    let isLastPage = false;
    const divReservationReqList = document.getElementById('reservationReqList');

    // 디버깅을 위한 로그 함수
    function debugLog(message, data = null) {
        console.log(`[무한스크롤] ${message}`, data || '');
    }

    // 스크롤 컨테이너가 제대로 설정되었는지 확인
    if (!divReservationReqList) {
        console.error('reservationReqList 엘리먼트를 찾을 수 없습니다.');
        return;
    }

    debugLog('스크롤 컨테이너 설정됨', {
        height: divReservationReqList.style.height,
        overflow: getComputedStyle(divReservationReqList).overflow
    });

    // 최초 1페이지 불러오기
    getAllReservationRequests(0);

    // 쿼리 스트링에 따른 탭 자동 클릭
    const queryParams = new URLSearchParams(window.location.search);
    const dest = queryParams.get('dest');
    switch (dest) {
        case 'showtab1':
            document.querySelector('div#showtab1')?.click();
            break;
        case 'showtab2':
            document.querySelector('div#showtab2')?.click();
            break;
        case 'showtab4':
            document.querySelector('div#showtab4')?.click();
            break;
    }

    /* ------------------------------- 데이터 요청 ------------------------------- */
    function getAllReservationRequests(pageNo = 0) {
        if (isLoading || isLastPage) {
            debugLog(`요청 스킵: isLoading=${isLoading}, isLastPage=${isLastPage}`);
            return;
        }
        
        isLoading = true;
        debugLog(`페이지 ${pageNo} 요청 시작`);

        axios.get(`/reservation/api/requestlist?p=${pageNo}`)
            .then(({ data }) => {
                debugLog(`페이지 ${pageNo} 응답 받음`, {
                    totalElements: data.totalElements,
                    totalPages: data.totalPages,
                    currentPage: data.number,
                    contentLength: data.content?.length || 0,
                    isLast: data.last
                });

                currentPageNo = data.number;
                isLastPage = data.last || (currentPageNo + 1 >= data.totalPages);
                appendReservationReqElements(data, pageNo === 0);
                
                debugLog(`페이지 처리 완료: currentPageNo=${currentPageNo}, isLastPage=${isLastPage}`);
            })
            .catch((error) => {
                console.error('API 호출 오류:', error);
                debugLog('API 호출 실패', error.message);
            })
            .finally(() => {
                isLoading = false;
                debugLog('로딩 상태 해제');
            });
    }
	
	function formatPrice(price) {
	    if (typeof price !== 'number') {
	        price = parseFloat(price) || 0; // 숫자가 아니면 0으로 처리하거나 파싱 시도
	    }
	    
	    // 5% 인상
	    const increasedPrice = price * 1.05;
	    
	    // 10원 단위로 반올림
	    const roundedPrice = Math.round(increasedPrice / 10) * 10;
	    
	    // 콤마 형식으로 포맷
	    return roundedPrice.toLocaleString('ko-KR');
	}

    /* ------------------------------- DOM 생성 및 바인딩 ------------------------------- */
    function appendReservationReqElements(data, isInit = false) {
        const content = data.content;
        if (isInit) {
            divReservationReqList.innerHTML = '';
            debugLog('리스트 초기화됨');
        }
        
        let htmlStr = '';

        if (!content || content.length === 0) {
            if (isInit) {
                divReservationReqList.innerHTML = '<p style="text-align: center; padding: 50px 0; color: #666;">들어온 예약 신청이 없습니다.</p>';
            }
            isLastPage = true;
            debugLog('콘텐츠가 비어있음, 마지막 페이지로 설정');
            return;
        }

        for (const dto of content) {
            if (dto.status === 5) continue;

            let postDetailsUrl = '/post/details';
            switch (dto.rentalCategoryId) {
                case 1: postDetailsUrl += `/bag?id=${dto.productId}`; break;
                case 2: postDetailsUrl += `/car?id=${dto.productId}`; break;
                default: postDetailsUrl += `?id=${dto.productId}`; break;
            }

            htmlStr += `
                <div class="reservation-card" data-reservation-id="${dto.id}">
                  <div class="res-img">
                    <a href="${postDetailsUrl}">
                    <img src="${dto.imageUrl || '/images/default-product.png'}" alt="상품 이미지" onerror="this.src='/images/default-product.png'"> </a>
                  </div>
                  <div class="res-info">
                    <p class="car-name">
                    <a href="${postDetailsUrl}"><strong>${dto.productName || '상품명 없음'}</strong></a>
                    </p>
                    <p><strong>대여 날짜:</strong> ${dto.rentalDate || '날짜 정보 없음'}</p>
                    <p><strong>대여 시간:</strong> ${dto.rentalTimeRange || '시간 정보 없음'}</p>
                    <p><strong>요금:</strong> ${formatPrice(dto.fee || 0)} 원</p>
                    <p><strong>예약자:</strong> ${dto.renterNickname || '알 수 없음'}</p> `;

            switch (dto.status) {
                case 0: case 1:
                    htmlStr += `
                        <div class="res-buttons">
                          <button class="btn-decline" data-id="${dto.id}" data-product-id="${dto.productId}">거절</button>
                          <button class="btn-accept" data-id="${dto.id}" data-product-id="${dto.productId}">수락</button>
                          <button class="btn-chat" data-id="${dto.id}" data-room-id="${dto.chatRoomId || ''}">채팅</button>
                        </div>
                      </div>
                    </div>`;
                    break;
                case 2:
                    htmlStr += `
                        <div class="res-buttons">
                          <button class="btn-chat" data-id="${dto.id}" data-room-id="${dto.chatRoomId || ''}">채팅</button>
                        </div>
                      </div>
                    </div>`;
                    break;
                case 3:
                    htmlStr += `
                        <div class="res-buttons">
                          <button class="btn-complete" onclick="openCompletePopupForResReq(this)" 
                              data-id="${dto.id}" data-renter-id="${dto.renterId}">거래완료</button>
                          <button class="btn-review d-none" disabled>후기 작성 완료</button> 
                        </div>
                      </div>
                    </div>`;
                    break;
                case 4:
                    htmlStr += `
                        <div class="res-buttons">
                          <button class="btn-rejected" disabled>거절함</button>
                          <button class="btn-delete-reserv" data-id="${dto.id}">삭제</button>
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

        // 기존 리스트 뒤에 새 항목들 추가 (무한스크롤 핵심)
        divReservationReqList.insertAdjacentHTML('beforeend', htmlStr);
        debugLog(`${content.length}개 아이템 추가됨`);

        // DOM 업데이트 후 스크롤 상태 확인
        setTimeout(() => {
            checkScrollableState();
        }, 100);

        // 동적 생성된 버튼들에 이벤트 바인딩
        bindEventListeners();
    }

    /* ------------------------------- 스크롤 상태 체크 ------------------------------- */
    function checkScrollableState() {
        const scrollHeight = divReservationReqList.scrollHeight;
        const clientHeight = divReservationReqList.clientHeight;
        const isScrollable = scrollHeight > clientHeight;
        
        debugLog('스크롤 상태 체크', {
            scrollHeight,
            clientHeight,
            isScrollable,
            isLastPage,
            hasContent: divReservationReqList.children.length > 0
        });

        // 스크롤이 불가능하고 아직 더 불러올 데이터가 있다면 자동으로 다음 페이지 로드
        if (!isScrollable && !isLastPage && divReservationReqList.children.length > 0) {
            debugLog('스크롤이 불가능한 상태에서 더 많은 데이터 로드');
            getAllReservationRequests(currentPageNo + 1);
        }
    }

    /* ------------------------------- 이벤트 바인딩 ------------------------------- */
    function bindEventListeners() {
        // 새로 생성된 요소들에만 이벤트 바인딩 (중복 방지)
        const newCards = divReservationReqList.querySelectorAll('.reservation-card:not([data-bound])');
        
        newCards.forEach(card => {
            card.setAttribute('data-bound', 'true');
            
            // 거절 버튼
            const declineBtn = card.querySelector('button.btn-decline');
            if (declineBtn) declineBtn.onclick = declineReservation;
            
            // 수락 버튼
            const acceptBtn = card.querySelector('button.btn-accept');
            if (acceptBtn) acceptBtn.onclick = acceptReservation;
            
            // 삭제 버튼
            const deleteBtn = card.querySelector('button.btn-delete-reserv');
            if (deleteBtn) deleteBtn.onclick = deleteReservation;
            
            // 채팅 버튼
            const chatBtn = card.querySelector('button.btn-chat');
            if (chatBtn) {
                chatBtn.onclick = function () {
                    const roomId = this.getAttribute('data-room-id');
                    const reservationId = this.getAttribute('data-id');
                    const currentUserId = document.body.getAttribute('data-user-id');
                    
                    if (roomId) {
                        window.location.href = `/chat?chatRoomId=${roomId}`;
                        return;
                    }
                    
                    if (!reservationId || !currentUserId) {
                        alert("예약 정보 또는 사용자 정보가 누락되었습니다.");
                        return;
                    }
                    
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
                };
            }
        });
        
        debugLog(`${newCards.length}개 카드에 이벤트 바인딩 완료`);
    }

    /* ------------------------------- 무한스크롤 구현 (개선됨) ------------------------------- */
    let scrollTimeout;
    
    divReservationReqList.addEventListener('scroll', function (e) {
        // 스크롤 이벤트 디바운싱
        if (scrollTimeout) {
            clearTimeout(scrollTimeout);
        }
        
        scrollTimeout = setTimeout(() => {
            if (isLoading || isLastPage) {
                debugLog(`스크롤 이벤트 스킵: isLoading=${isLoading}, isLastPage=${isLastPage}`);
                return;
            }
            
            const scrollTop = this.scrollTop;
            const scrollHeight = this.scrollHeight;
            const clientHeight = this.clientHeight;
            const threshold = 200; // 하단에서 200px 전에 로드
            const scrollBottom = scrollTop + clientHeight;
            
            debugLog('스크롤 상태', {
                scrollTop,
                scrollHeight,
                clientHeight,
                scrollBottom,
                remainingScroll: scrollHeight - scrollBottom,
                threshold
            });
            
            // 스크롤이 거의 하단에 닿았을 때 다음 페이지 로드
            if (scrollBottom >= scrollHeight - threshold) {
                debugLog('스크롤 임계점 도달, 다음 페이지 로드');
                getAllReservationRequests(currentPageNo + 1);
            }
        }, 100); // 100ms 디바운싱
    });

    /* ------------------------------- 탭 클릭 시 처리 ------------------------------- */
    const showtab1Element = document.getElementById('showtab1');
    if (showtab1Element) {
        showtab1Element.addEventListener('click', function() {
            debugLog('예약현황 탭 클릭됨');
            // 탭 전환 시 상태 초기화 및 첫 페이지 로드
            resetPagination();
            getAllReservationRequests(0);
        });
    }

    /* ------------------------------- 유틸리티 함수 ------------------------------- */
    function resetPagination() {
        currentPageNo = 0;
        isLastPage = false;
        isLoading = false;
        debugLog('페이지네이션 상태 초기화');
    }

    /* ------------------------------- 예약 처리 함수들 ------------------------------- */
    function declineReservation(event) {
        const check = confirm('예약을 거절하시겠습니까?\n거절 후에는 취소가 불가능합니다.');
        if (!check) return;
        
        const reservationId = event.target.getAttribute('data-id');
        const productId = event.target.getAttribute('data-product-id');
        const uri = `/reservation/refuse/${reservationId}/${productId}`;
        
        debugLog('예약 거절 요청', { reservationId, productId });
        
        axios.get(uri)
            .then((response) => {
                debugLog('거절 요청 처리 결과', response.data);
                alert('예약이 거절되었습니다.');
                resetPagination();
                getAllReservationRequests(0);
            })
            .catch((error) => {
                console.error('예약 거절 오류:', error);
                alert('예약 거절 중 오류가 발생했습니다.');
            });
    }
    
    function acceptReservation(event) {
        const check = confirm('예약을 수락하시겠습니까?');
        if (!check) return;
        
        const reservationId = event.target.getAttribute('data-id');
        const productId = event.target.getAttribute('data-product-id');
        const uri = `/reservation/confirm/${reservationId}/${productId}`;
        
        debugLog('예약 수락 요청', { reservationId, productId });
        
        axios.get(uri)
            .then((response) => {
                debugLog('수락 요청 처리 결과', response.data);
                alert('예약이 수락되었습니다.\n채팅으로 대여자와 세부사항을 조율해보세요!');
                resetPagination();
                getAllReservationRequests(0);
            })
            .catch((error) => {
                console.error('예약 수락 오류:', error);
                alert('예약 수락 중 오류가 발생했습니다.');
            });
    }
    
    function deleteReservation(event) {
        const check = confirm('예약을 삭제하시겠습니까?\n삭제된 예약은 되돌릴 수 없습니다.');
        if (!check) return;
        
        const reservationId = event.target.getAttribute('data-id');
        const uri = `/reservation/delete/${reservationId}`;
        
        debugLog('예약 삭제 요청', { reservationId });
        
        axios.delete(uri)
            .then((response) => {
                debugLog('삭제된 예약 id', response.data);
                alert(`예약이 삭제되었습니다.\n삭제된 예약 ID: ${response.data}`);
                resetPagination();
                getAllReservationRequests(0);
            })
            .catch((error) => {
                console.error('예약 삭제 오류:', error);
                alert('예약 삭제 중 오류가 발생했습니다.');
            });
    }

    // 전역 함수로 노출 (디버깅용)
    window.debugInfiniteScroll = {
        getCurrentState: () => ({
            currentPageNo,
            isLoading,
            isLastPage,
            containerHeight: divReservationReqList.clientHeight,
            scrollHeight: divReservationReqList.scrollHeight,
            childrenCount: divReservationReqList.children.length
        }),
        forceLoadNext: () => getAllReservationRequests(currentPageNo + 1),
        reset: () => {
            resetPagination();
            getAllReservationRequests(0);
        }
    };
});