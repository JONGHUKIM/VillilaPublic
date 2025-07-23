document.addEventListener("DOMContentLoaded", function () {
  console.log("DOM 로드 완료"); // 디버깅: DOM 로드 확인

  let userJelly = 0;
  let paymentSuccess = false; // 예약용

  // ✅ 현재 보유 젤리 개수 가져와서 화면에 표시
  fetch("/jjam/api/jjams/total")
    .then(response => response.json())
    .then(data => {
      userJelly = data.totalJjams ?? data;
      document.getElementById("user-jjams").textContent = `현재 보유 JJAM: ${userJelly}개`;
    })
    .catch(error => console.error("쩸 개수 불러오기 실패", error));

  // ✅ 예약 버튼 처리
  const reserveButton = document.getElementById("submit-btn");
  console.log("버튼 찾기:", reserveButton); // 디버깅: 버튼 확인

  if (!reserveButton) {
    console.error("submit-btn을 찾을 수 없습니다.");
    return;
  }

  reserveButton.addEventListener("click", function () {
    console.log("버튼 클릭됨");

    // 🔍 총 요금 가져오기
	const priceText = document.getElementById("total-price").value.replace(/[^\d]/g, "");
	const totalPrice = parseInt(priceText, 10);
	console.log("총 요금:", totalPrice);

    if (!totalPrice || isNaN(totalPrice)) {
      alert("날짜/시작/종료 시간을 확인하세요.");
      return;
    }
	
	if (userJelly < totalPrice) {
	    const goToCharge = confirm(`현재 보유 JJAM(${userJelly}개)이 부족합니다.\n총 요금은 ${totalPrice}JJAM입니다.\n충전 페이지로 이동할까요?`);
	    if (goToCharge) {
	        const chargeWindow = window.open("", "_blank");
	        if (chargeWindow) {
	            chargeWindow.location.href = "/jjam/shop";
	        } else {
	            window.location.href = "/jjam/shop";
	        }
	    }
	    return;
	}

    // URL에서 productId 추출
	const urlParams = new URLSearchParams(window.location.search);
	const productId = urlParams.get("id") || urlParams.get("productId");
	console.log("전송용 productId:", productId);

    // 예약용 정보 구성
    const rentalDate = document.getElementById("rental-date").value;
    const start = document.getElementById("start-time").value;
    const end = document.getElementById("end-time").value;
    const startTime = `${rentalDate}T${start}:00`;
    const endTime = `${rentalDate}T${end}:00`;
    const reservationData = { productId, startTime, endTime };

	// ✅ 예약 요청
	fetch(`./reservation/check?id=${productId}`, {
	    method: "POST",
	    headers: { "Content-Type": "application/json" },
	    credentials: "include",
	    body: JSON.stringify(reservationData)
	})
	.then(response => response.json())
	.then(checkResult => {
	    console.log('예약 요청 진행 중...');
	    if (checkResult === true) {
	        return fetch("/api/jjam/reservations", {
	            method: "POST",
	            headers: { "Content-Type": "application/json" },
	            credentials: "include",
	            body: JSON.stringify({ 
	                fee: totalPrice,
	                productId: productId 
	            })
	        });
	    } else {
	        throw new Error('conflict');
	    }
	})
	    .then(response => response.json())
	    .then(data => {
	        console.log("API 응답 데이터:", data);
	        if (data.success) {
	            alert(`💸 예약금 ${data.usedJjams} JJAM 차감 완료!\n남은 JJAM: ${data.remainingJjams}개\n\n📝 예약 신청이 완료되었습니다.\n상대방이 수락하면 나머지 ${totalPrice - reservationDeposit} JJAM이 결제됩니다.`);
	            paymentSuccess = true;
	        } else {
	            const goToCharge = confirm(`${data.message}\n충전 페이지로 이동할까요?`);
	            if (goToCharge) {
	                const chargeWindow = window.open("", "_blank");
	                if (chargeWindow) {
	                    chargeWindow.location.href = "/jjam/shop";
	                } else {
	                    window.location.href = "/jjam/shop";
	                }
	            }
	        }
	    })
	    .then(() => {
	        if (paymentSuccess) {
	            reservationHandler(reservationData, productId);
	        }
	    })
	    .catch(error => {
	        if (error.message === 'conflict') {
	            alert("⚠️ 해당 시간대에 이미 예약이 존재합니다. 다른 시간대를 선택해주세요.");
	        } else {
	            console.error("예약 처리 중 오류 발생:", error);
	            alert("❗ 예약 처리 중 문제가 발생했습니다. 다시 시도해주세요.");
	        }
	    });
	});
	
	// 10원 단위 반올림 함수 추가
	function roundToNearest10Won(value) {
	    const remainder = value % 10;
	    return remainder < 5 ? value - remainder : value + (10 - remainder);
	}

  /* 예약 등록 함수 */
  function reservationHandler(reservationData, productId) {
    const uri = `./reservation/create?id=${productId}`;
    fetch(uri, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify(reservationData)
    })
      .then(response => response.json())
      .then(createResult => {
        console.log('서버 응답: ', createResult);
        if (createResult === true) {
          alert('예약이 신청되었습니다.');
          window.close();
        } else {
          alert('예약 신청이 실패하였습니다.');
        }
      })
      .catch(error => console.log(error));
  }
});
