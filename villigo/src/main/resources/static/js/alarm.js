/**
 * fragments.html 파일에 포함
 */

document.addEventListener('DOMContentLoaded', () => {
   const socket = new SockJS('/ws');
   const stompClient = StompJs.Stomp.over(socket);
   const sectionToaster = document.getElementById('toaster');
   
   // 새로운 채팅 개수 알림 생성 여부를 저장
   let isChatNotifyCreated = false;
   
   stompClient.connect({}, (frame) => {
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
                   <img src="" class="rounded me-2" alt="">
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
   }, (error) => {
       console.error('Connection failed: ', error);
       // 사용자에게 알람 표시
       sectionToaster.innerHTML = '<p>서버 연결 실패</p>';
   });
   
   // 디버그 설정
   stompClient.debug = (str) => {
       console.log(str);
   };
   
   // ******* 알람 팝업 설정 *******
   let currentPageNo = 0;    // 현재 페이지 번호 -> 페이징용
   const linkAlarmBtn = document.querySelectorAll('.alarm-btn'); // 알림 점
   let isAlarmDotCreated = false; // 알림 점 생성 여부를 저장
   let showAllAlarms = false; // 모든 알림 표시 여부
   let isLoading = false; // 로딩 중 여부
   const btnShowNewAlarms = document.querySelectorAll('#showNewAlarms'); // 안 읽은 알림 표시 버튼
   const btnShowAllAlarms = document.querySelectorAll('#showAllAlarms'); // 모든 알림 표시 버튼
   
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
           console.log(divLoadingBlack); // divLoadingBlack가 존재하는지 확인
           console.log(divLoadingBlack.getBoundingClientRect()); // 위치 확인
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
           console.log(divLoadingWhite); // divLoadingWhite가 존재하는지 확인
           console.log(divLoadingWhite.getBoundingClientRect()); // 위치 확인
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
       .then((response) => {
           return response.json();            
       })
       .then((data) => {
           console.log('data: ', data);
           const pagedModel = data[0]; // 알람들이 담긴 pagedModel
           const unreadChatMessages = data[1]; // 새로운 채팅 개수
           currentPageNo = pagedModel.page.number; // 현재 알람창의 페이지 번호
           makeAlarmElements(unreadChatMessages, pagedModel); 
       })
       .catch((error) => {
           console.log(error);
           isLoading = false; // 에러 시 로딩 해제
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
       .then((response) => {
           return response.json();            
       })
       .then((data) => {
           console.log('data: ', data);
           const pagedModel = data[0]; // 알람들이 담긴 pagedModel
           const unreadChatMessages = data[1]; // 새로운 채팅 개수
           currentPageNo = pagedModel.page.number; // 현재 알람창의 페이지 번호
           makeAlarmElements(unreadChatMessages, pagedModel); 
       })
       .catch((error) => {
           console.log(error);
           isLoading = false; // 에러 시 로딩 해제
           loadings.forEach((div) => div.classList.remove('active'));
       });
   }
   
   function makeAlarmElements(unreadChatMessages, {content, page}) {
       let htmlStr = ''; // div에 삽입할 문자열
       const divNotifyNewChatMessages = document.querySelectorAll('div.notifyNewChatMessage');
       // 읽지 않은 채팅 메세지 개수를 확인하고 알림창에 표시
       if (isChatNotifyCreated === false) {
           const countNewChat = unreadChatMessages;
           let newChatNotify = '';
           if (countNewChat === 0) {
               newChatNotify = '💬 새로운 채팅 메시지가 없습니다';
               isAlarmDotCreated = false;
           } else {
               newChatNotify = `💬 새로운 채팅 메시지가 ${countNewChat}개 있습니다.`;
               makeAlarmDot(); // 알림 점이 없으면 생성
           }
           htmlStr += `
               <a class="text-decoration-none text-dark" id="chatAlarm" 
                   href="/chat" role="link">
                   ${newChatNotify}
               </a>             
           `;
           divNotifyNewChatMessages.forEach((div) => div.innerHTML = htmlStr);
           isChatNotifyCreated = true;
           htmlStr = ''; // 문자열 초기화
       }                        
       
       // 알람 카드들이 표시될 div 요소
       const divalarmContents = document.querySelectorAll('.alarmContents');
       // 알람 없음 메시지가 표시된 div 요소
       const divNoAlarm = document.querySelectorAll('.divNoAlarm');

       const contentLength = Object.keys(content).length;
       console.log('표시되는 알람 갯수: ', contentLength);
       // 1. 표시해야 하는 알람이 없는 경우
       if (contentLength === 0) {
           console.log('새로운 알람 없음');
           divalarmContents.forEach((div) => {
               div.innerHTML = htmlStr;
               div.innerHTML += '<div class="alarm-item" style="text-align: center;">👀 새로운 알람이 없습니다.</div>';
           });
           //divNoAlarm.forEach((div) => div.innerHTML = '👀 새로운 알람이 없습니다.');
       } else {
       // 2. 표시해야 하는 알람이 있는 경우
           divNoAlarm.forEach((div) => div.classList.add('d-none'));
           if (!showAllAlarms) makeAlarmDot(); // 알림 점이 없으면 생성
           let destination = '/mypage?dest='; // 알람에 추가할 href 링크
           for (const dto of content) {
               // 알람 카테고리에 따라 href 링크를 수정
               switch (dto.alarmCategoryId) {
                   case 1:
                       destination += 'showtab1'; // 예약 현황
                       break;
                   case 2:
                       destination += 'showtab2'; // 나의 예약
                       break;
                   case 3:
                       destination += 'showtab4'; // 후기
                       break;
               }
               
               // 알람 데이터 생성 시간
               const createdTime = dto.createdTime;
               // 현재 시간
               const currentTime = new Date();
               // 서버 시간을 JS Date 객체로 변환
               const parsedTime = new Date(createdTime);
               // 시간 차 계산 (밀리초 단위)
               const timeDifference = currentTime - parsedTime;
               // 분 단위로 변환
               const minutesDifference = Math.floor(timeDifference / (1000 * 60));
               let timeDifferenceNotify = '';
               if (minutesDifference < 60) {
                   // 60분 미만일 경우 '분 전'
                   timeDifferenceNotify = minutesDifference + '분 전';
               } else if (minutesDifference >= 60 && minutesDifference < 1440) {
                   // 60분 이상 1440분 미만일 경우 '시간 전'
                   const hoursDifference = Math.floor(minutesDifference / 60);
                   timeDifferenceNotify = hoursDifference + '시간 전';
               } else {
                   // 1440분 이상일 경우 날짜로 표시
                   const formattedCreatedTime = `${parsedTime.getFullYear()}.${String(parsedTime.getMonth() + 1).padStart(2, '0')}.${String(parsedTime.getDate()).padStart(2, '0')}`;
                   timeDifferenceNotify = formattedCreatedTime;
               }
               
               // 알람별 status에 따라 오버레이 추가
               switch (dto.status) {
                   case true: // 읽음(오버레이 추가)
                        htmlStr += `
                        <div class="alarm-item alarm-card">
                            <a class="alarm-link text-decoration-none text-dark" id="destination" 
                                href="${destination}" data-id="${dto.id}" role="link">
                                ${dto.content}
                            </a>
                            <br>
                            <p class="small text-muted text-end timeNotify">${timeDifferenceNotify}</p>
                            <div class="alarm-item-overlay"></div>
                            <button class="delete-alarm-btn" data-id="${dto.id}">&times;</button>
                        </div>
                        `;
                       break;
                   case false: // 안 읽음
                       htmlStr += `
                       <div class="alarm-item alarm-card">
                           <a class="alarm-link text-decoration-none text-dark" id="destination" 
                               href="${destination}" data-id="${dto.id}" role="link">
                               ${dto.content}
                           </a>
                           <br>
                           <p class="small text-muted text-end timeNotify">${timeDifferenceNotify}</p>
                           <button class="delete-alarm-btn" data-id="${dto.id}">&times;</button>
                       </div>
                       `;
                       break;
               }
           }
           // 페이징 처리
           if (currentPageNo === 0) {
               // 첫번째 페이지면 기존 내용을 다 지우고 새로 작성.
               divalarmContents.forEach((div) => div.innerHTML = htmlStr);
           } else {
               // 첫번째 페이지가 아니면 기존 내용 밑에 예약 목록을 추가.
               divalarmContents.forEach((div) => div.innerHTML += htmlStr);
           }
           
           // 알람 카드에서 링크가 설정되는 요소에 click 이벤트 리스너를 설정
           const alarmLink = document.querySelectorAll('a.alarm-link');
           alarmLink.forEach((link) => link.addEventListener('click', checkAlarm));
           // 알람 카드에서 삭제 버튼에 click 이벤트 리스너를 설정
           const btnAlarmDeletes = document.querySelectorAll('button.delete-alarm-btn');
           btnAlarmDeletes.forEach((btn) => btn.addEventListener('click', deleteAlarm));
       } 
   
       isLoading = false; // 로딩 완료
       console.log('로딩 완료! 로딩 중 인가요?', isLoading);
       const loadings = document.querySelectorAll('.alarm-loading');
       loadings.forEach((div) => div.classList.remove('active'));
       
       // 마지막 페이지일 경우 무한 스크롤 중단
       if (page.number === page.totalPages - 1) {
           console.log('🚀 모든 알람을 불러왔습니다. 무한 스크롤을 중단합니다.');
           observer1.disconnect(); // 검은색 네비게이션바 옵저버 해제
           observer2.disconnect(); // 하얀색 네비게이션바 옵저버 해제
           // 로딩 div를 표시하고 div 안의 내용을 안내 메시지로 변경
           loadings.forEach((div) => {
               div.innerHTML = '최근 14일 동안 받은 알림을 모두 확인했습니다.';
               div.classList.add('active');            
           });     
       }
   }
   
   // 알림 점이 생성되지 않았으면 알림점을 생성하는 함수
   function makeAlarmDot() {
       if (!isAlarmDotCreated) {
           linkAlarmBtn.forEach((link) => link.innerHTML += '<span class="alarm-dot" id="alarmDot"></span>'); // 알림 점 표시
           isAlarmDotCreated = true;
       }
   }
   
   // 알람 확인 처리 함수
   function checkAlarm(event) {
       event.preventDefault(); // 기본 클릭 동작 취소
       const link = document.getElementById('destination');
       
       // 링크 비활성화 로직 추가
       link.style.pointerEvents = "none"; // 클릭 불가능하게 설정
       link.style.opacity = "0.5"; // 시각적으로 비활성화 표시
       
       const uri = `/alarm/check/${event.target.getAttribute('data-id')}`;
       
       fetch(uri)
       .then((response) => {
           return response.json();            
       })
       .then((data) => {
           console.log('확인 처리된 알람 ID: ', data);
           // 서버 응답이 오면 링크에 저장된 주소로 이동
           const destination = link.getAttribute('href');
           window.location.href = destination;
       })
       .catch((error) => {
           console.log(error);
           // 링크 활성화 복구
           link.style.pointerEvents = "auto";
           link.style.opacity = "1.0";
       });
   }
   
   // 알람 삭제 처리 함수
   function deleteAlarm(event) {
       const check = confirm('선택한 알림을 삭제하시겠습니까?');
       if (!check) {
           return;
       }
    
       const uri = `/alarm/delete/${event.target.getAttribute('data-id')}`;
       
       fetch(uri)
       .then((response) => {
           console.log('알람 삭제 완료');
           // 알림 로딩 중 표시 div에 옵저버 재연결
           const divLoadingBlack = document.getElementById('alarm-loading-black');  
           const divLoadingWhite = document.getElementById('alarm-loading-white');
           observer1.observe(divLoadingBlack);
           observer2.observe(divLoadingWhite);
           // 로딩 div를 숨기고 div 안의 내용을 로딩 메시지로 변경
           const loadings = document.querySelectorAll('.alarm-loading');
           loadings.forEach((div) => {
               div.innerHTML = '로딩중...';
               div.classList.remove('active');            
           });  
           // 알람 팝업 초기화
           isChatNotifyCreated = false;
           currentPageNo = 0;
           getAllAlarms();
       })
       .catch((error) => console.log(error));
   }
   
   // 새로운 알림 표시 버튼 이벤트 리스너 콜백 함수
   function showNewAlarmHandler(event) {
        btnShowNewAlarms.forEach((btn) => {
            btn.classList.remove('btn-outline-secondary');
            btn.classList.add('btn-secondary');
        });
        btnShowAllAlarms.forEach((btn) => {
            btn.classList.remove('btn-secondary');
            btn.classList.add('btn-outline-secondary');
        });
        
        // 알림 로딩 중 표시 div에 옵저버 재연결
        const divLoadingBlack = document.getElementById('alarm-loading-black');  
        const divLoadingWhite = document.getElementById('alarm-loading-white');
        observer1.observe(divLoadingBlack);
        observer2.observe(divLoadingWhite);
        
        currentPageNo = 0; // 페이징 초기화
        showAllAlarms = false; // 모든 알림 표시 여부
        isLoading = false; // 로딩 중 여부
        getUnreadAlarms();
   }
   
   // 모든 알림 표시 버튼 이벤트 리스너 콜백 함수
   function showAllAlarmHandler(event) {
       btnShowNewAlarms.forEach((btn) => {
           btn.classList.remove('btn-secondary');
           btn.classList.add('btn-outline-secondary');
       });
       btnShowAllAlarms.forEach((btn) => {
           btn.classList.remove('btn-outline-secondary');
           btn.classList.add('btn-secondary');
       });
       
       // 알림 로딩 중 표시 div에 옵저버 재연결
       const divLoadingBlack = document.getElementById('alarm-loading-black');  
       const divLoadingWhite = document.getElementById('alarm-loading-white');
       observer1.observe(divLoadingBlack);
       observer2.observe(divLoadingWhite);

       currentPageNo = 0; // 페이징 초기화
       showAllAlarms = true; // 모든 알림 표시 여부
       isLoading = false; // 로딩 중 여부
       getAllAlarms();
   }
   
});