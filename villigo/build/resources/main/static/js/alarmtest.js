/**
 * alarmtest.html 파일에 포함
 */

document.addEventListener('DOMContentLoaded', () => {
    const socket = new SockJS('/ws');
    const stompClient = Stomp.over(socket);
    const userId = document.getElementById('userId').value;    
    const sectionToaster = document.getElementById('toaster');
    // 현재 날짜 및 시간 가져오기
    const now = new Date();

    // 연도, 월, 일, 시간, 분, 초 가져오기
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0'); // 월은 0부터 시작하므로 +1 필요
    const day = String(now.getDate()).padStart(2, '0');
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    const seconds = String(now.getSeconds()).padStart(2, '0');

    // 포맷팅
    const formattedTime = `${year}.${month}.${day} ${hours}:${minutes}:${seconds}`;

    stompClient.connect({}, (frame) => {
        console.log('Connected: ' + frame);
        // 알림 구독
        stompClient.subscribe(`/user/queue/alert`, (msg) => {
            console.log("알림 도착:", msg.body);
            sectionToaster.innerHTML = `
              <div class="toast-container position-fixed bottom-0 end-0 p-3">
                <div id="liveToast" class="toast" role="alert" aria-live="assertive" aria-atomic="true">
                  <div class="toast-header">
                    <img src="" class="rounded me-2" alt="">
                    <strong class="me-auto">villigo</strong>
                    <small>${formattedTime}</small>
                    <button type="button" class="btn-close" data-bs-dismiss="toast" aria-label="Close"></button>
                  </div>
                  <div class="toast-body">
                      ${msg.body}
                  </div>
                </div>
              </div>
            `;
            setTimeout(() => {
                const toastLive = document.getElementById('liveToast');
                const toastBootstrap = bootstrap.Toast.getOrCreateInstance(toastLive);
                console.log('토스트!: ' + toastBootstrap);
                toastBootstrap.show();
            }, 0);
        });
    });
   
   // 디버그 설정
   stompClient.debug = (str) => {
       console.log(str);
   };
   
   // 알람 버튼 이벤트 리스너 설정
   const btnSendAlarm = document.getElementById('btnSendA');
   btnSendAlarm.addEventListener('click', () => {
       stompClient.send('/app/rent', {}, JSON.stringify({ productId: 29, ownerId: '7' }));
   });
});