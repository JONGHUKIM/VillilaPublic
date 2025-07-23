document.addEventListener("DOMContentLoaded", function () {
    console.log("DOM 로드 완료");

    let reservationData = [];
    let pricePerMin = 0;

    // URL에서 productId 추출
    const params = new URLSearchParams(window.location.search);
    const productId = params.get("productId");

    // 예약 정보 받아오고 나서 Flatpickr 초기화
    fetch(`/reservation/api/reservations?productId=${productId}`)
        .then(res => res.json())
        .then(data => {
            reservationData = data;
            console.log("예약 정보:", reservationData);
            initializeFlatpickr(); // 예약 정보 준비된 뒤에 초기화
        });

    // Flatpickr 초기화 함수
    function initializeFlatpickr() {
        if (typeof flatpickr === "undefined") {
            console.warn("Flatpickr가 아직 로드되지 않았습니다. 100ms 후 재시도...");
            setTimeout(initializeFlatpickr, 100);
            return;
        }

        // Flatpickr 인스턴스를 변수에 저장하여 더 안정적으로 참조
        const rentalDatePicker = flatpickr("#rental-date", {
            enableTime: false,
            dateFormat: "Y-m-d",
            minDate: "today",
            locale: "ko",
            onOpen: function () {
                document.querySelector(".flatpickr-calendar").style.zIndex = "9999";
            },
            onChange: function (selectedDates, dateStr) {
                console.log("📅 선택된 날짜:", dateStr);
                // 날짜 변경 시에는 noticeBox를 완전히 비우고 시작
                document.getElementById("reservation-notice").innerHTML = ""; 
                showReservationsForDate(dateStr); // 해당 날짜의 기존 예약 정보 표시

                const today = new Date(); // 현재 날짜 및 시간 객체
                const todayStr = today.toISOString().slice(0, 10); // 'YYYY-MM-DD' 형식의 오늘 날짜
                const startPicker = document.querySelector("#start-time")._flatpickr;
                const endPicker = document.querySelector("#end-time")._flatpickr;
                
                let minAllowedStartDateTime; // Flatpickr의 minTime으로 설정될 최종 Date 객체
                
                // 선택된 날짜의 모든 예약 중 가장 늦게 끝나는 시간 찾기
                const reservationsOnSelectedDate = reservationData.filter(r => r.start.startsWith(dateStr));
                let latestExistingEndTime = null; // 해당 날짜의 가장 늦은 기존 예약 종료 시간

                if (reservationsOnSelectedDate.length > 0) {
                    reservationsOnSelectedDate.forEach(r => {
                        const rEnd = new Date(r.end); // 예약 종료 시간 Date 객체
                        if (!latestExistingEndTime || rEnd.getTime() > latestExistingEndTime.getTime()) {
                            latestExistingEndTime = rEnd;
                        }
                    });
                }

                if (dateStr === todayStr) {
                    // 오늘 날짜를 선택했을 때
                    const nowPlus10Minutes = new Date(today.getTime() + 10 * 60 * 1000); // 현재 시간 + 10분

                    if (latestExistingEndTime) {
                        // 오늘 날짜에 기존 예약이 있는 경우
                        const afterLastReservation = new Date(latestExistingEndTime.getTime() + 10 * 60 * 1000); // 마지막 예약 종료 + 10분
                        
                        // 마지막 예약 종료 + 10분과 현재 시간 + 10분 중 더 늦은 시간을 minAllowedStartDateTime으로 설정
                        minAllowedStartDateTime = afterLastReservation > nowPlus10Minutes ? afterLastReservation : nowPlus10Minutes;
                    } else {
                        // 오늘 날짜에 기존 예약이 없는 경우, 현재 시간 + 10분으로 설정
                        minAllowedStartDateTime = nowPlus10Minutes;
                    }
                } else {
                    // 오늘이 아닌 다른 날짜를 선택했을 때
                    if (latestExistingEndTime) {
                        // 다른 날짜에 기존 예약이 있는 경우, 마지막 예약 종료 + 10분을 minAllowedStartDateTime으로 설정
                        minAllowedStartDateTime = new Date(latestExistingEndTime.getTime() + 10 * 60 * 1000);
                        // 해당 날짜의 00:00보다 이르면 00:00으로 조정
                        const startOfDay = new Date(`${dateStr}T00:00:00`);
                        if (minAllowedStartDateTime < startOfDay) {
                             minAllowedStartDateTime = startOfDay;
                        }
                    } else {
                        // 다른 날짜에 기존 예약이 없는 경우, 해당 날짜의 00:00부터 시작 가능
                        minAllowedStartDateTime = new Date(`${dateStr}T00:00:00`);
                    }
                }

                // Flatpickr의 minTime을 설정할 문자열 형식으로 변환
                const formattedMinTime = minAllowedStartDateTime.getHours().toString().padStart(2, '0') + ":" + minAllowedStartDateTime.getMinutes().toString().padStart(2, '0');
                startPicker.set("minTime", formattedMinTime);
                
                // 시작 시간 필드를 계산된 최소 시간으로 자동 설정
                // 이 setDate 호출이 start-time의 onChange를 트리거하여 end-time까지 설정하게 됩니다.
                startPicker.setDate(minAllowedStartDateTime, true); 
                
                calculatePrice(); // 날짜 변경 시 요금 재계산
                checkTimeConflict(); // 날짜 변경 시 겹침 검사 재실행
            }
        });

        // 시간 선택기 (시작 시간)
        const startTimePicker = flatpickr("#start-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            locale: "ko",
            minTime: "00:00", // 초기 기본값
            onChange: function (selectedDates, dateStr, instance) {
                const start = selectedDates[0]; // 선택된 시작 시간 Date 객체
                const endPicker = document.querySelector("#end-time")._flatpickr; // 종료 시간 Flatpickr 인스턴스

                if (start && endPicker) {
                    const end = new Date(start.getTime() + 5 * 60000); // 시작 시간으로부터 5분 뒤
                    const formatted = end.getHours().toString().padStart(2, '0') + ":" + end.getMinutes().toString().padStart(2, '0');
                    endPicker.set("minTime", formatted); // 종료 시간의 최소값을 시작 시간 + 5분으로 설정
                    endPicker.setDate(end, true); // 종료 시간 필드를 시작 시간 + 5분으로 자동 설정
                }

                calculatePrice(); // 시작 시간 변경 시 요금 재계산
                checkTimeConflict(); // 시작 시간 변경 시 겹침 검사 재실행
            }
        });

        // 시간 선택기 (종료 시간)
        const endTimePicker = flatpickr("#end-time", {
            enableTime: true,
            noCalendar: true,
            dateFormat: "H:i",
            time_24hr: true,
            locale: "ko",
            onOpen: function () {
                this.calendarContainer.style.zIndex = "9999";
            },
            onChange: () => {
                calculatePrice(); // 종료 시간 변경 시 요금 재계산
                checkTimeConflict(); // 종료 시간 변경 시 겹침 검사 재실행
            }
        });
		
        // 시간 선택 시 겹침 검사, 버튼 비활성화 여부 제어
        ["start-time", "end-time"].forEach(id => {
            document.getElementById(id).addEventListener("change", checkTimeConflict);
        });

        function checkTimeConflict() {
            const date = document.getElementById("rental-date").value;
            const start = document.getElementById("start-time").value;
            const end = document.getElementById("end-time").value;
            const button = document.getElementById("submit-btn");
            const noticeBox = document.getElementById("reservation-notice");

            // 모든 경고 메시지 제거
            const existingWarnMsgs = noticeBox.querySelectorAll(".conflict-message");
            existingWarnMsgs.forEach(msg => msg.remove());
            
            // 모든 필수 필드가 채워지지 않았다면 버튼 비활성화
            if (!(date && start && end)) {
                button.disabled = true;
                // 기존 예약 정보 메시지가 없다면 noticeBox 숨김
                if (noticeBox.children.length === 0) { 
                    noticeBox.style.display = "none";
                }
                return;
            }

            // 선택된 날짜와 시간으로 Date 객체 타임스탬프 생성
            const startTimeStamp = new Date(`${date}T${start}:00`).getTime();
            const endTimeStamp = new Date(`${date}T${end}:00`).getTime();
			
			// 현재 시간을 초와 밀리초를 0으로 설정하여 가져옴
			const today = new Date(); 
			today.setSeconds(0);
			today.setMilliseconds(0);
			
            const todayStr = today.toISOString().slice(0, 10); // 'YYYY-MM-DD' 형식의 오늘 날짜

            // 1. 선택된 시작 시간이 예약 가능한 최소 시간보다 빠른지 검사
            let minAllowedTimeForSelectionStamp; // 현재 선택된 날짜에 대해 실제로 예약 가능한 최소 시간

            const reservationsOnSelectedDate = reservationData.filter(r => r.start.startsWith(date));
            let latestExistingEndTime = null;

            if (reservationsOnSelectedDate.length > 0) {
                reservationsOnSelectedDate.forEach(r => {
                    const rEnd = new Date(r.end);
                    if (!latestExistingEndTime || rEnd.getTime() > latestExistingEndTime.getTime()) {
                        latestExistingEndTime = rEnd;
                    }
                });
            }

			if (date === todayStr) {
			    // 오늘 날짜인 경우
			    // 현재 시간의 초와 밀리초를 0으로 맞춘 Date 객체에 10분 더함
			    const nowCleanedPlus10Minutes = new Date(today.getTime() + 10 * 60 * 1000); 

			    if (latestExistingEndTime) {
			        // 오늘 날짜에 기존 예약이 있는 경우
			        const afterLastReservation = new Date(latestExistingEndTime.getTime() + 10 * 60 * 1000); // 마지막 예약 종료 + 10분
			        
			        // 마지막 예약 종료 + 10분과 현재 시간 + 10분 중 더 늦은 시간으로 설정
			        minAllowedTimeForSelectionStamp = afterLastReservation.getTime() > nowCleanedPlus10Minutes.getTime() ? afterLastReservation.getTime() : nowCleanedPlus10Minutes.getTime();
			    } else {
			        // 오늘 날짜에 기존 예약이 없는 경우, 현재 시간 + 10분으로 설정
			        minAllowedTimeForSelectionStamp = nowCleanedPlus10Minutes.getTime();
			    }
			} else {
			    // 오늘이 아닌 다른 날짜인 경우
			    if (latestExistingEndTime) {
			        const afterLastReservation = new Date(latestExistingEndTime.getTime() + 10 * 60 * 1000);
			        minAllowedTimeForSelectionStamp = afterLastReservation.getTime();
			        const startOfDayStamp = new Date(`${date}T00:00:00`).getTime();
			        if (minAllowedTimeForSelectionStamp < startOfDayStamp) {
			             minAllowedTimeForSelectionStamp = startOfDayStamp;
			        }
			    } else {
			        minAllowedTimeForSelectionStamp = new Date(`${date}T00:00:00`).getTime();
			    }
           } 
            
            // 실제 선택된 시작 시간이 계산된 최소 허용 시간보다 이른 경우
            // 59초의 여유를 주어 1분 미만의 오차를 허용
			if (startTimeStamp < minAllowedTimeForSelectionStamp - 59000) { 
			    button.disabled = true;
			    const warn = document.createElement("p");
			    warn.textContent = "선택한 시간이 너무 이른 것 같아요! 조금 뒤로 설정해 주세요 😊";
			    warn.style.color = "#e53e3e";
			    warn.style.fontWeight = "bold";
			    warn.style.marginTop = "5px";
			    warn.classList.add("conflict-message");
			    noticeBox.appendChild(warn);
			    noticeBox.style.display = "block";
			    return; // 다른 검사로 넘어가지 않고 즉시 종료
			}

            // 2. 시작 시간이 종료 시간보다 같거나 클 경우
            if (startTimeStamp >= endTimeStamp) {
                button.disabled = true;
                const warn = document.createElement("p");
                warn.textContent = "종료 시간이 시작 시간보다 빠르거나 같습니다 😢";
                warn.style.color = "#e53e3e";
                warn.style.fontWeight = "bold";
                warn.style.marginTop = "5px";
                warn.classList.add("conflict-message");
                noticeBox.appendChild(warn);
                noticeBox.style.display = "block";
                return; // 다른 검사로 넘어가지 않고 즉시 종료
            }

            // 3. 기존 예약과 겹칠 경우
            const isConflict = reservationData.some(r => {
                const rStart = new Date(r.start).getTime();
                const rEnd = new Date(r.end).getTime();
                // 겹침 조건 (선택 시작 < 기존 종료) && (선택 종료 > 기존 시작)
                return startTimeStamp < rEnd && endTimeStamp > rStart;
            });

            if (isConflict) {
                button.disabled = true;
                const warn = document.createElement("p");
                warn.textContent = "선택한 예약시간이 기존 예약과 겹쳐요 😢";
                warn.style.color = "#e53e3e";
                warn.style.fontWeight = "bold";
                warn.style.marginTop = "5px";
                warn.classList.add("conflict-message");
                noticeBox.appendChild(warn);
                noticeBox.style.display = "block";
            } else {
                // 모든 유효성 검사를 통과하고 겹치는 예약이 없을 경우
                button.disabled = false;
                // 기존 예약 정보가 표시되지 않았다면 noticeBox를 숨김
                // (showReservationsForDate가 내용을 비운 후 기존 예약만 다시 추가)
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
				const displayPricePerMin = roundToNearest10Won(pricePerMin * 1.05);
				document.getElementById("price-per-min").value = `${displayPricePerMin.toLocaleString()} 원 / 분`;
                calculatePrice(); 
            });

        // 요금 계산
		function calculatePrice() {
		    const startTime = document.getElementById("start-time").value;
		    const endTime = document.getElementById("end-time").value;
		    const inputTotalPrice = document.getElementById("total-price");

		    if (startTime && endTime) {
		        // 날짜는 계산에 필요 없으므로 임의로 고정
		        const start = new Date(`2000-01-01T${startTime}:00`); 
		        const end = new Date(`2000-01-01T${endTime}:00`);     
		        const diffMinutes = (end - start) / (1000 * 60);
		        
		        if (diffMinutes > 0) {
		            // 1. 분당 요금에 5% 수수료를 먼저 적용하고 10원 단위 반올림
		            const pricePerMinWithFee = roundToNearest10Won(pricePerMin * 1.05);
		            
		            // 2. 반올림된 분당 요금에 사용 시간을 곱함
		            const basePrice = pricePerMinWithFee * diffMinutes;
		            
		            // 3. 최종 총액도 10원 단위 반올림
		            const finalTotalPrice = roundToNearest10Won(basePrice);
		            
		            inputTotalPrice.value = `${finalTotalPrice.toLocaleString()} 원`;
		        } else {
		            inputTotalPrice.value = "0 원";
		        }
		    } else {
		        inputTotalPrice.value = "0 원"; // 시간이 선택되지 않았다면 0원 표시
		    }
		}

        // 10원 단위 반올림 함수
        function roundToNearest10Won(value) {
            const remainder = value % 10;
            return remainder < 5 ? value - remainder : value + (10 - remainder);
        }

        // 해당 날짜의 예약 정보 출력 함수
        function showReservationsForDate(dateStr) {
            const container = document.getElementById("reservation-notice");
            // 기존 경고 메시지를 제외한 다른 P 태그들을 먼저 제거
            const existingPs = container.querySelectorAll("p:not(.conflict-message)");
            existingPs.forEach(p => p.remove());


            const sameDateReservations = reservationData.filter(r => r.start.startsWith(dateStr));

            if (sameDateReservations.length > 0) {
                container.style.display = "block";
                // 기존 예약 정보는 항상 위에 표시되도록 prepend 사용
                sameDateReservations.forEach(r => {
                    const startTime = r.start.slice(11, 16); // 'HH:mm' 부분만 추출
                    const endTime = r.end.slice(11, 16);     // 'HH:mm' 부분만 추출
                    const p = document.createElement("p");
                    p.textContent = `${startTime} ~ ${endTime} 예약됨`;
                    p.style.color = "red";
                    p.style.fontWeight = "bold";
                    p.classList.add("existing-reservation-info"); // 새로운 클래스 추가
                    container.prepend(p); // 기존 예약 정보를 가장 위에 추가
                });
            } 

            checkTimeConflict();
        }
        const initialDateValue = rentalDatePicker.element.value; 
        rentalDatePicker.config.onChange[0](rentalDatePicker.selectedDates, initialDateValue, rentalDatePicker);
    }
});