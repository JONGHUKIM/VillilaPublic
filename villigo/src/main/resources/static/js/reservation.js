document.addEventListener("DOMContentLoaded", function () {
    console.log("DOM 로드 완료, Flatpickr 초기화 시작");

    function initializeFlatpickr() {
        if (typeof flatpickr === "undefined") {
            console.warn("Flatpickr가 아직 로드되지 않았습니다. 100ms 후 재시도...");
            setTimeout(initializeFlatpickr, 100);
            return;
        }

        // 날짜 선택
        flatpickr("#rental-date", {
            enableTime: false,
            dateFormat: "Y-m-d",
            minDate: "today",
            locale: "ko", /* 한글 로케일 설정 */
            onOpen: function () {
                console.log("📅 캘린더 열림");
                document.querySelector(".flatpickr-calendar").style.zIndex = "9999";
            },
            onReady: function () {
                console.log("📅 Flatpickr 준비 완료");
            },
            onChange: function (selectedDates, dateStr) {
                console.log("📅 선택된 날짜:", dateStr);
            }
        });

        // 시간 선택
        flatpickr("#start-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            position: "auto",
            locale: "ko", /* 한글 로케일 설정 */
            onChange: function (selectedDates, dateStr) {
                console.log("⏰ 시작 시간 선택:", dateStr);
                calculatePrice();
            }
        });

        flatpickr("#end-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            position: "auto",
            locale: "ko", /* 한글 로케일 설정 */
			onOpen: function () { // onOpen 이벤트 추가
			    this.calendarContainer.style.zIndex = "9999"; // 시간 선택기의 z-index
			},
            onChange: function (selectedDates, dateStr) {
                console.log("⏰ 종료 시간 선택:", dateStr);
                calculatePrice();
            }
        });
        
        // URL에서 productId 추출 (추가 정보 전송용)
        const params = new URLSearchParams(window.location.search);
        const productId = params.get("productId");
        
        // REST API로 상품의 요금을 불러옴
        let pricePerMin = 0;
        fetch(`./api/productfee?id=${productId}`)
        .then((response) => response.json())
        .then((data) => {
            console.log('상품 대여 요금 조회 결과 data: ', data);
            pricePerMin = data;
            // 이용 요금 표시 input 요소
            const inputPricePerMin = document.getElementById("price-per-min");
            inputPricePerMin.value = `${pricePerMin.toLocaleString()} 원 / 분`;
            console.log('요금 반영 완료 => pricePerMin=', pricePerMin);
        })
        .catch((error) => console.log(error));

        // 총 요금 계산
        function calculatePrice() {
            const startTime = document.getElementById("start-time").value;
            const endTime = document.getElementById("end-time").value;

            if (startTime && endTime) {
                const start = new Date(`2000-01-01T${startTime}:00`);
                const end = new Date(`2000-01-01T${endTime}:00`);
                const diffMinutes = (end - start) / (1000 * 60);
                const inputTotalPrice = document.getElementById("total-price");
                if (diffMinutes > 0) {
                    const totalPrice = diffMinutes * pricePerMin;
                    inputTotalPrice.value = `${totalPrice.toLocaleString()} 원`;
                } else {
                    document.getElementById("total-price").value = "0 원";
                }
            }
        }
    }

    initializeFlatpickr();
});