/**
 * fragments.html 파일에 포함
 */

document.addEventListener('DOMContentLoaded', () => {
    const stompClient = new StompJs.Client({
        webSocketFactory: () => new SockJS('/ws'),
        reconnectDelay: 5000, // 재접속 간격 (5초)
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        debug: (str) => {
            console.log(str);
        }
    });
    const sectionToaster = document.getElementById('toaster');
    
    // 새로운 채팅 개수 알림 생성 여부를 저장
    let isChatNotifyCreated = false;
    
    // 수정: WebSocket 연결과 에러 핸들링을 명확히 분리
    stompClient.onConnect = (frame) => {
        console.log('Connected: ' + frame);
        // 알림 구독
        stompClient.subscribe(`/user/queue/alert`, (msg) => {
            const now = new Date(); // 현재 날짜 및 시간 가져오기
            const formattedTime = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')} 
                ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
            console.log("알림 도착:", msg.body);
            sectionToaster.innerHTML = `
              <div class="toast-container position-fixed bottom-0 start-0 p-3">
                <div id="liveToast" class="toast" role="alert" aria-live="assertive" aria-atomic="true">
                  <div class="toast-header">
                    <!-- 수정: 빈 이미지 소스 대신 기본 이미지 경로 추가 -->
                    <img src="/images/default-toast-icon.png" class="rounded me-2" alt="알림 아이콘">
                    <strong class="me-auto">📢 villigo</strong>
                    <small>${formattedTime}</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                  </div>
                  <div class="toast-body">
                      ${msg.body}
                  </div>
                </div>
              </div>
            `;

            const toastLive = document.getElementById('liveToast');
            const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLive);
            console.log('토스트!: ' + toastBootstrap);
            toastBootstrap.show();
            isChatNotifyCreated = false;
            getAllAlarms();
        });
    };

    // 수정: WebSocket 에러 핸들링 분리
    stompClient.onStompError = (error) => {
        console.error('Connection failed: ', error);
        sectionToaster.innerHTML = '<p>서버 연결 실패</p>';
    };

    // 수정: WebSocket 연결 활성화
    stompClient.activate();
    
    // ******* 알람 팝업 설정 *******
    let currentPageNo = 0;    // 현재 페이지 번호 -> 페이징용
    const linkAlarmBtn = document.querySelectorAll('.alarm-btn'); // 알림 점
    let isAlarmDotCreated = false; // 알림 점 생성 여부를 저장
    let showAllAlarms = false; // 모든 알림 표시 여부
    let isLoading = false; // 로딩 중 여부
    // 수정: ID 기반 선택 대신 클래스 기반 선택으로 변경
    const btnShowNewAlarms = document.querySelectorAll('.btn-show-new-alarms'); // 안 읽은 알림 표시 버튼
    const btnShowAllAlarms = document.querySelectorAll('.btn-show-all-alarms'); // 모든 알림 표시 버튼
    
    // 알림 로딩 중 표시 div - 검은색 네비게이션바
    const divLoadingBlack = document.getElementById('alarm-loading-black');
    // 알림 로딩 중 표시 div - 하양색 네비게이션바  
    const divLoadingWhite = document.getElementById('alarm-loading-white');
    
    // 무한 스크롤을 위한 옵저버 설정.
    // 옵저버1: 검은색 네비게이션바
    const observer1 = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && !isLoading) {
            if (showAllAlarms) {
                getAllAlarms(currentPageNo + 1);
            } else {
                getUnreadAlarms(currentPageNo + 1);
            }
            console.log('무한 스크롤(black)');
            // console.log(divLoadingBlack); // divLoadingBlack가 존재하는지 확인
            // console.log(divLoadingBlack.getBoundingClientRect()); // 위치 확인
        }
    }, {
        // divAlarmList가 아닌 다른 부모 요소가 스크롤을 담당하므로 root: null(기본값)으로 설정.
        root: null, 
        threshold: 0.7
    });
    observer1.observe(divLoadingBlack);
    
    // 옵저버2: 하양색 네비게이션바
    const observer2 = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && !isLoading) {
            if (showAllAlarms) {
                getAllAlarms(currentPageNo + 1);
            } else {
                getUnreadAlarms(currentPageNo + 1);
            }
            console.log('무한 스크롤(white)');
            // console.log(divLoadingWhite); // divLoadingWhite가 존재하는지 확인
            // console.log(divLoadingWhite.getBoundingClientRect()); // 위치 확인
        }
    }, {
        root: null,
        threshold: 0.7
    });
    observer2.observe(divLoadingWhite);
    
    // 알림 표시 버튼들에 이벤트 리스너 설정
    btnShowNewAlarms.forEach((btn) => btn.addEventListener('click', showNewAlarmHandler));
    btnShowAllAlarms.forEach((btn) => btn.addEventListener('click', showAllAlarmHandler));

    // 안 읽은 알람 데이터를 가져옴.
    getUnreadAlarms();
    
    /* ---------------------- 함수 선언 ---------------------- */
    function getUnreadAlarms(pageNo = 0) {
        if (isLoading) return; // 중복 호출 방지
        isLoading = true;
        console.log('로딩중...');
        const url = `/alarm/list/preforward?p=${pageNo}`;
        const loadings = document.querySelectorAll('.alarm-loading');
        loadings.forEach((div) => div.classList.add('active')); // 로딩 표시
        
        fetch(url)
        .then((response) => response.json())
        .then((data) => {
            console.log('data: ', data);
            // 수정: 구조 분해 할당으로 데이터 처리 간소화
            const [pagedModel, unreadChatMessages] = data;
            currentPageNo = pagedModel.page.number;
            makeAlarmElements(unreadChatMessages, pagedModel);
        })
        .catch((error) => {
            console.log(error);
            isLoading = false;
            loadings.forEach((div) => div.classList.remove('active'));
        });
    }
    
    function getAllAlarms(pageNo = 0) {
        if (isLoading) return; // 중복 호출 방지
        isLoading = true;
        console.log('로딩중...');
        const url = `/alarm/list?p=${pageNo}`;
        const loadings = document.querySelectorAll('.alarm-loading');
        loadings.forEach((div) => div.classList.add('active')); // 로딩 표시
		
        fetch(url)
        .then((response) => response.json())
        .then((data) => {
            console.log('data: ', data);
            // 수정: 구조 분해 할당으로 데이터 처리 간소화
            const [pagedModel, unreadChatMessages] = data;
            currentPageNo = pagedModel.page.number;
            makeAlarmElements(unreadChatMessages, pagedModel);
        })
        .catch((error) => {
            console.log(error);
            isLoading = false;
            loadings.forEach((div) => div.classList.remove('active'));
        });
    }
    
    function makeAlarmElements(unreadChatMessages, {content, page}) {
        let htmlStr = '';
        const divNotifyNewChatMessages = document.querySelectorAll('div.notifyNewChatMessage');
        // 읽지 않은 채팅 메세지 개수를 확인하고 알림창에 표시
        if (!isChatNotifyCreated) {
            const countNewChat = unreadChatMessages;
            // 수정: 삼항 연산자로 간소화
            const newChatNotify = countNewChat === 0 ? '💬 새로운 채팅 메시지가 없습니다' : `💬 새로운 채팅 메시지가 ${countNewChat}개 있습니다.`;
            if (countNewChat > 0) makeAlarmDot();
            htmlStr += `<a class="text-decoration-none text-dark" href="/chat" role="link">${newChatNotify}</a>`;
            divNotifyNewChatMessages.forEach((div) => div.innerHTML = htmlStr);
            isChatNotifyCreated = true;
            htmlStr = '';
        }
        
        // 알람 카드들이 표시될 div 요소
        const divAlarmContents = document.querySelectorAll('.alarmContents');
        // 알람 없음 메시지가 표시된 div 요소
        const divNoAlarm = document.querySelectorAll('.divNoAlarm');

        // 수정: content.length로 간소화
        if (content.length === 0) {
            console.log('새로운 알람 없음');
            divAlarmContents.forEach((div) => div.innerHTML = '<div class="alarm-item" style="text-align: center;">👀 새로운 알람이 없습니다.</div>');
        } else {
            divNoAlarm.forEach((div) => div.classList.add('d-none'));
            if (!showAllAlarms) makeAlarmDot();
            let destination = '/mypage?dest=';
            // 수정: forEach와 삼항 연산자로 간소화
            content.forEach(dto => {
                destination = '/mypage?dest=' + (
                    dto.alarmCategoryId === 1 ? 'showtab1' :
                    dto.alarmCategoryId === 2 ? 'showtab2' : 'showtab4'
                );
                const createdTime = new Date(dto.createdTime);
                const currentTime = new Date();
                const minutesDiff = Math.floor((currentTime - createdTime) / (1000 * 60));
                const timeNotify = minutesDiff < 60 ? `${minutesDiff}분 전` 
                    : minutesDiff < 1440 ? `${Math.floor(minutesDiff / 60)}시간 전` 
                    : `${createdTime.getFullYear()}.${String(createdTime.getMonth() + 1).padStart(2, '0')}.${String(createdTime.getDate()).padStart(2, '0')}`;
                
                htmlStr += `
                    <div class="alarm-item alarm-card">
                        <a class="alarm-link text-decoration-none text-dark" href="${destination}" data-id="${dto.id}" role="link">${dto.content}</a>
                        <br><p class="small text-muted text-end timeNotify">${timeNotify}</p>
                        ${dto.status ? '<div class="alarm-item-overlay"></div>' : ''}
                        <button class="delete-alarm-btn" data-id="${dto.id}">×</button>
                    </div>`;
            });

            // 수정: 삼항 연산자로 간소화
            divAlarmContents.forEach((div) => div.innerHTML = currentPageNo === 0 ? htmlStr : div.innerHTML + htmlStr);

            // 수정: 이벤트 리스너 등록 간소화
            document.querySelectorAll('a.alarm-link').forEach(link => link.addEventListener('click', checkAlarm));
            document.querySelectorAll('button.delete-alarm-btn').forEach(btn => btn.addEventListener('click', deleteAlarm));
        }
    
        isLoading = false;
        console.log('로딩 완료! 로딩 중 인가요?', isLoading);
        const loadings = document.querySelectorAll('.alarm-loading');
        loadings.forEach((div) => div.classList.remove('active'));
        
        // 마지막 페이지일 경우 무한 스크롤 중단
        if (page.number === page.totalPages - 1) {
            console.log('🚀 모든 알람을 불러왔습니다. 무한 스크롤을 중단합니다.');
            observer1.disconnect();
            observer2.disconnect();
            loadings.forEach((div) => {
                div.innerHTML = '최근 14일 동안 받은 알림을 모두 확인했습니다.';
                div.classList.add('active');            
            });     
        }
    }
    
    // 알림 점이 생성되지 않았으면 알림점을 생성하는 함수
    function makeAlarmDot() {
        if (!isAlarmDotCreated) {
            // 수정: ID 속성 제거
            linkAlarmBtn.forEach((link) => link.innerHTML += '<span class="alarm-dot"></span>');
            isAlarmDotCreated = true;
        }
    }
    
    // 알람 확인 처리 함수
    function checkAlarm(event) {
        event.preventDefault();
        // 수정: link 변수 간소화
        const link = event.target;
        link.style.pointerEvents = 'none';
        link.style.opacity = '0.5';
        fetch(`/alarm/check/${link.dataset.id}`)
        .then(response => response.json())
        .then(data => {
            console.log('확인 처리된 알람 ID: ', data);
            window.location.href = link.getAttribute('href');
        })
        .catch(error => {
            console.log(error);
            link.style.pointerEvents = 'auto';
            link.style.opacity = '1.0';
        });
    }
    
    // 알람 삭제 처리 함수
    function deleteAlarm(event) {
        const check = confirm('선택한 알림을 삭제하시겠습니까?');
        if (!check) return;
    
        fetch(`/alarm/delete/${event.target.dataset.id}`)
        .then(() => {
            console.log('알람 삭제 완료');
            observer1.observe(divLoadingBlack);
            observer2.observe(divLoadingWhite);
            const loadings = document.querySelectorAll('.alarm-loading');
            loadings.forEach((div) => {
                div.innerHTML = '로딩중...';
                div.classList.remove('active');            
            });  
            isChatNotifyCreated = false;
            currentPageNo = 0;
            getAllAlarms();
        })
        .catch(error => console.log(error));
    }
    
    // 새로운 알림 표시 버튼 이벤트 리스너 콜백 함수
    function showNewAlarmHandler() {
        btnShowNewAlarms.forEach((btn) => {
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-secondary');
        });
        btnShowAllAlarms.forEach((btn) => {
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-outline-secondary');
        });
        observer1.observe(divLoadingBlack);
        observer2.observe(divLoadingWhite);
        currentPageNo = 0;
        showAllAlarms = false;
        isLoading = false;
        getUnreadAlarms();
    }
    
    // 모든 알림 표시 버튼 이벤트 리스너 콜백 함수
    function showAllAlarmHandler() {
        btnShowNewAlarms.forEach((btn) => {
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-outline-secondary');
        });
        btnShowAllAlarms.forEach((btn) => {
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-secondary');
        });
        observer1.observe(divLoadingBlack);
        observer2.observe(divLoadingWhite);
        currentPageNo = 0;
        showAllAlarms = true;
        isLoading = false;
        getAllAlarms();
    }
});