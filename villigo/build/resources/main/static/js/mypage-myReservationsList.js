/**
 * 예약 삭제 기능을 처리하는 함수
 * @param {Event} event - 클릭 이벤트 객체
 */
function deleteReservation(event) {
  console.log('deleteReservation called');
  const reservationId = event.target.getAttribute('data-reservation-id');
  console.log('reservationId:', reservationId);

  if (!reservationId) {
    console.error('reservationId is undefined or null');
    alert('예약 ID가 없습니다. 삭제를 진행할 수 없습니다.');
    return;
  }

  // 사용자에게 삭제 확인 요청
  const check = confirm('정말로 이 예약을 삭제하시겠습니까?');
  if (!check) {
    return;
  }

  // mypage-reservationReqList.js와 동일한 엔드포인트 사용
  const uri = `/reservation/delete/${reservationId}`;
  console.log('Sending DELETE request to:', uri);

  // CSRF 토큰 가져오기 (Spring Security는 현재 비활성화 상태)
  const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
  const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

  const config = {
    headers: {
      'Content-Type': 'application/json'
    }
  };

  if (csrfToken && csrfHeader) {
    config.headers[csrfHeader] = csrfToken;
    console.log('CSRF token included:', csrfHeader, '=', csrfToken);
  } else {
    console.warn('CSRF token not found in meta tags (CSRF is disabled in config)');
  }

  axios
    .delete(uri, config)
    .then((response) => {
      console.log('삭제된 예약 id:', response.data);
      alert(`예약이 삭제되었습니다.\n삭제된 예약 ID: ${reservationId}`);
      // 삭제된 카드 제거 및 UI 갱신
      const card = document.querySelector(`.reservation-card[data-reservation-id="${reservationId}"]`);
      if (card) {
        card.remove();
        console.log('Reservation card removed from UI');
      }
      // 페이지 새로고침으로 UI 갱신 (선택적)
      // window.location.reload();
    })
    .catch((error) => {
      console.error('Error during deletion:', error);
      if (error.response) {
        console.error('Error response:', error.response.status, error.response.statusText, error.response.data);
        alert(`예약 삭제 중 오류가 발생했습니다: ${error.response.status} ${error.response.statusText}`);
      } else {
        alert('예약 삭제 중 오류가 발생했습니다: ' + error.message);
      }
    });
}

// 문서가 로드될 때 실행
document.addEventListener('DOMContentLoaded', () => {
  console.log('DOM fully loaded');

  /**
   * 탭 전환 기능을 처리하는 함수
   * @param {number} index - 표시할 탭의 인덱스
   */
  window.showTab = function(index) {
    // 모든 탭 콘텐츠와 탭 버튼에서 active 클래스 제거
    document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab').forEach(tab => tab.classList.remove('active'));

    // 선택된 탭 콘텐츠와 탭 버튼에 active 클래스 추가
    document.querySelectorAll('.tab-content')[index].classList.add('active');
    document.querySelectorAll('.tab')[index].classList.add('active');
  };

  // 전체 문서에 대한 클릭 이벤트 디버깅 (이벤트 전파 확인)
  document.addEventListener('click', (event) => {
    console.log('Document clicked on:', event.target.tagName, event.target.className);
    const deleteButton = event.target.closest('.delete-btn');
    if (!deleteButton) {
      console.log('Clicked element is not a delete button');
    }
  });

  // 각 탭 버튼에 클릭 이벤트 리스너 등록
  document.querySelectorAll('.tab').forEach((tab, index) => {
    tab.addEventListener('click', () => window.showTab(index));
  });

  // 이벤트 위임 방식으로 .delete-btn 클릭 처리
  const tabContents = document.querySelectorAll('.tab-content');
  let reservationList = null;

  tabContents.forEach(content => {
    const header = content.querySelector('h3');
    if (header && header.textContent.trim() === '나의 예약') {
      reservationList = content.querySelector('.reservation-list');
    }
  });
  
  if (reservationList) {
    reservationList.addEventListener('click', function(event) {
      const btn = event.target.closest('.delete-btn');
      if (btn) {
        console.log('Delete button clicked for reservationId:', btn.getAttribute('data-reservation-id'));
        deleteReservation(event);
      }
    });
  } else {
    console.warn('Reservation list element not found');
  }

  // 디버깅용 로그
  console.log('초기 로드 시 전체 삭제 버튼 개수:', document.querySelectorAll('.delete-btn').length);
});