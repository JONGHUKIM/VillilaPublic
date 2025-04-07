/**
 * /jjam 관련 JavaScript
 */

document.addEventListener("DOMContentLoaded", function () {
    // ✅ 현재 보유 젤리 개수 가져와서 화면에 표시
    fetch("/jjam/api/jjams/total")
        .then(response => response.json())
		.then(data => {
			document.getElementById("user-jjams").innerHTML = `
			                <div class="jjam-badge">
			                    <span class="text"></span>
			                    <span class="jjamemoji">💎</span>
			                    <span class="amount">${data}</span>
			                </div>
			            `;
		        })
		        .catch(error => console.error("젤리 개수 불러오기 실패", error));

    // ✅ 사용자 ID 가져오기 (팝업에서 사용할 값)
    let userId = null;
    fetch("/jjam/api/user/info")
        .then(response => response.json())
        .then(data => {
            console.log('data: ' + data);
            userId = data;
            console.log('userId: ' + userId);
        })
        .catch(error => {
            console.error("사용자 정보 불러오기 실패:", error);
        });

    // ✅ 잼 충전하기 버튼 이벤트 추가
    const chargeButtons = document.querySelectorAll(".btn-charge");

    chargeButtons.forEach(button => {
        button.addEventListener("click", function () {
            const amount = this.getAttribute("data-amount");

            // 🔹 알람창 표시 (사용자 확인)
            const isConfirmed = confirm(`${amount} 쨈을 충전합니다.\n결제창으로 이동합니다.`);

            if (isConfirmed) {
                // 팝업 크기 설정 (작게 조정)
                const popupWidth = 400;
                const popupHeight = 500;

                // 사용 가능한 화면 크기와 위치 계산 (다중 모니터 환경 고려)
                const screenWidth = window.screen.availWidth;
                const screenHeight = window.screen.availHeight;
                const screenLeft = window.screen.availLeft || 0;
                const screenTop = window.screen.availTop || 0;

                // 화면의 중앙 위치 계산
                const left = screenLeft + (screenWidth - popupWidth) / 2;
                const top = screenTop + (screenHeight - popupHeight) / 2;

                // ✅ 결제 팝업 창 열기 (중앙 정렬)
                const popup = window.open(
                    `/jjam/payment?amount=${amount}&userId=${userId}`,
                    "paymentPopup",
                    `width=${popupWidth},height=${popupHeight},left=${left},top=${top},resizable=no,scrollbars=no`
                );

                // 🔹 팝업 닫힌 후 부모 창 데이터 갱신
                const checkPopupClosed = setInterval(() => {
                    if (popup && popup.closed) {
                        clearInterval(checkPopupClosed);
                        location.reload(); // 충전 완료 후 젤리 개수 업데이트
                    }
                }, 1000);
            }
        });
    });
});