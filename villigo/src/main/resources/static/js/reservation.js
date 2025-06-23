document.addEventListener("DOMContentLoaded", function () {
    console.log("DOM 로드 완료");

    let reservationData = [];
    let pricePerMin = 0;

    // URL에서 productId 추출 (공통 사용)
    const params = new URLSearchParams(window.location.search);
    const productId = params.get("productId");

    // 1. 예약 정보 받아오고 나서 Flatpickr 초기화
    fetch(`/reservation/api/reservations?productId=${productId}`)
        .then(res => res.json())
        .then(data => {
            reservationData = data;
            console.log("✅ 예약 정보:", reservationData);
            initializeFlatpickr(); // 예약 정보 준비된 뒤에 초기화
        });

    // 2. Flatpickr 초기화 함수
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
            locale: "ko",
            onOpen: function () {
                document.querySelector(".flatpickr-calendar").style.zIndex = "9999";
            },
            onChange: function (selectedDates, dateStr) {
                console.log("📅 선택된 날짜:", dateStr);
                showReservationsForDate(dateStr);
            }
        });

        // 시간 선택기
        flatpickr("#start-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            locale: "ko",
            onChange: () => calculatePrice()
        });

        flatpickr("#end-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            locale: "ko",
            onOpen: function () {
                this.calendarContainer.style.zIndex = "9999";
            },
            onChange: () => calculatePrice()
        });
		
		// 시간 선택 시 겹침 검사, 버튼 비활성화 여부제어
		["start-time", "end-time"].forEach(id => {
		    document.getElementById(id).addEventListener("change", checkTimeConflict);
		});

		function checkTimeConflict() {
		    const date = document.getElementById("rental-date").value;
		    const start = document.getElementById("start-time").value;
		    const end = document.getElementById("end-time").value;
		    const button = document.getElementById("submit-btn");
		    const noticeBox = document.getElementById("reservation-notice");

		    if (!(date && start && end)) {
		        button.disabled = true;
		        return;
		    }

		    const startTime = new Date(`${date}T${start}:00`).getTime();
		    const endTime = new Date(`${date}T${end}:00`).getTime();

		    if (startTime >= endTime) {
		        button.disabled = true;
		        return;
		    }

		    const isConflict = reservationData.some(r => {
		        const rStart = new Date(r.start).getTime();
		        const rEnd = new Date(r.end).getTime();
		        return startTime < rEnd && endTime > rStart;
		    });

		    if (isConflict) {
		        button.disabled = true;

		        const warn = document.createElement("p");
		        warn.textContent = "예약시간이 겹쳐요 😢";
		        warn.style.color = "#e53e3e";
		        warn.style.fontWeight = "bold";
		        warn.style.marginTop = "5px";
		        warn.classList.add("conflict-message");

		        // 중복 방지: 이미 있으면 안 추가
		        const already = noticeBox.querySelector(".conflict-message");
		        if (!already) noticeBox.appendChild(warn);

		        noticeBox.style.display = "block";
		    } else {
		        button.disabled = false;

		        // 기존 경고 메시지 제거
		        const warn = noticeBox.querySelector(".conflict-message");
		        if (warn) warn.remove();

		        // 다른 예약이 없을 경우 다시 숨김
		        if (noticeBox.children.length === 0) {
		            noticeBox.style.display = "none";
		        }
		    }
		}

        // 요금 불러오기
        fetch(`./api/productfee?id=${productId}`)
            .then(res => res.json())
            .then(data => {
                pricePerMin = data;
                document.getElementById("price-per-min").value = `${pricePerMin.toLocaleString()} 원 / 분`;
            });

        // 요금 계산
        function calculatePrice() {
            const startTime = document.getElementById("start-time").value;
            const endTime = document.getElementById("end-time").value;
            const inputTotalPrice = document.getElementById("total-price");

            if (startTime && endTime) {
                const start = new Date(`2000-01-01T${startTime}:00`);
                const end = new Date(`2000-01-01T${endTime}:00`);
                const diffMinutes = (end - start) / (1000 * 60);
                if (diffMinutes > 0) {
                    const basePrice = diffMinutes * pricePerMin;
                    const totalPriceWithFee = basePrice * 1.05;
                    inputTotalPrice.value = `${roundToNearest10Won(totalPriceWithFee).toLocaleString()} 원`;
                } else {
                    inputTotalPrice.value = "0 원";
                }
            }
        }

		// 반올림 함수
        function roundToNearest10Won(value) {
            const remainder = value % 10;
            return remainder < 5 ? value - remainder : value + (10 - remainder);
        }

        // 예약 정보 출력 함수
		function showReservationsForDate(dateStr) {
		    const container = document.getElementById("reservation-notice");
		    container.innerHTML = "";

		    const sameDateReservations = reservationData.filter(r => r.start.startsWith(dateStr));

		    if (sameDateReservations.length > 0) {
		        container.style.display = "block";
		        sameDateReservations.forEach(r => {
		            const startTime = r.start.slice(11, 16);
		            const endTime = r.end.slice(11, 16);
		            const p = document.createElement("p");
		            p.textContent = `${startTime} ~ ${endTime} 예약됨`;
		            p.style.color = "red";
		            p.style.fontWeight = "bold";
		            container.appendChild(p);
		        });
		    } else {
		        container.style.display = "none";
		    }

		    checkTimeConflict(); // 날짜 바뀌면 겹침 검사 다시
		}
    }
});
