document.addEventListener("DOMContentLoaded", () => {

    const tabs = document.querySelectorAll(".tab");
    const contents = document.querySelectorAll(".tab-content");

    const activeTabIndex = Array.from(tabs).findIndex(tab => tab.classList.contains("active"));
    if (activeTabIndex === 2) bindReservationCardEvents();
    if (activeTabIndex === 4) loadReviews();

    window.showTab = function(index) {
        tabs.forEach((t, i) => t.classList.toggle("active", i === index));
        contents.forEach((c, i) => c.classList.toggle("active", i === index));
        if (index === 2) bindReservationCardEvents();
        if (index === 4) loadReviews();
    };
	
	// WebSocket 알림 구독
	const userId = document.body.dataset.userId;
	if (userId) {
	  const socket = new SockJS('/ws');
	  const stompClient = Stomp.over(socket);

	  // userId 헤더 포함해서 연결
	  stompClient.connect({ userId: userId }, () => {
	    console.log('✅ WebSocket connected');

	    // 알림 구독
	    stompClient.subscribe(`/topic/notifications.${userId}`, (message) => {
	      const data = JSON.parse(message.body);
	      alert(data.message);  // 혹은 원하는 알림 표시 방식
	    });
	  }, (error) => {
	    console.error('❌ WebSocket 연결 실패:', error);
	  });
	}

    axios.get('/api/user/profile', { withCredentials: true })
        .then(response => {
            const data = response.data;
            document.getElementById('nickname').textContent = data.nickname || '닉네임 없음';
            document.getElementById('jjamPoints').textContent = data.jjamPoints || 0;
            document.getElementById('region').textContent = data.region || '지역 없음';
            document.getElementById('theme').textContent = '관심 상품: ' + (data.theme || '없음');
            

            const profileImage = document.getElementById('profileImage');
            if (data.avatar) {
                let cleanAvatar = data.avatar.startsWith('/') ? data.avatar.slice(1) : data.avatar;
                profileImage.outerHTML = `<img id="profileImage" src="/images/avatar/${cleanAvatar}" alt="프로필 사진">`;
            } else {
                profileImage.outerHTML = `<span id="profileImage" class="emoji-frog">🐸</span>`;
            }

            loadReviews();
        })
        .catch(error => {
            console.error('Error fetching user profile:', error);
            loadReviews();
        });

    let currentMannerScore = 36;

    function loadReviews() {
        const userId = document.body.getAttribute('data-user-id');
        if (!userId) {
            updateMannerScoreBar(currentMannerScore);
            createScoreInfoElements();
            return;
        }
        axios.get(`/api/reviews/${userId}`)
            .then(response => {
                const reviews = response.data;
                displayReviews(reviews);
                updateMannerScoreFromReviews(reviews);
                createScoreInfoElements();
            })
            .catch(error => {
                updateMannerScoreBar(currentMannerScore);
                createScoreInfoElements();
                const reviewsContainer = document.getElementById("reviewList");
                if (reviewsContainer) {
                    reviewsContainer.innerHTML = "<p>후기 데이터를 불러올 수 없습니다.</p>";
                }
            });
    }

    function createScoreInfoElements() {
        const mannerScoreContainer = document.getElementById("mannerScoreContainer");
        if (!mannerScoreContainer || mannerScoreContainer.querySelector(".manner-score-info")) return;
        const scoreInfo = document.createElement("div");
        scoreInfo.className = "manner-score-info";
        scoreInfo.innerHTML = "<span>0</span><span>50</span><span>100</span>";
        const scoreDescription = document.createElement("div");
        scoreDescription.className = "manner-score-description";
        scoreDescription.textContent = "기본 매너 온도는 36°C 입니다.";
        const scoreText = mannerScoreContainer.querySelector(".manner-score-text");
        if (scoreText) {
                    scoreText.after(scoreInfo); // scoreText 뒤에 삽입
                } else {
                   mannerScoreContainer.appendChild(scoreInfo);
                }
                mannerScoreContainer.appendChild(scoreDescription);
            }

    function updateMannerScoreFromReviews(reviews) {
        let mannerScore = 36;
        if (reviews && reviews.length > 0) {
            let totalReviewScore = 0;
            reviews.forEach(review => {
                const score = typeof review.score === 'number' ? review.score : 0;
                totalReviewScore += score;
            });
            mannerScore += totalReviewScore;
            mannerScore = Math.min(100, Math.max(0, mannerScore));
        }
        currentMannerScore = mannerScore;
        updateMannerScoreBar(mannerScore);
    }

    function updateMannerScoreBar(mannerScore) {
        const mannerScoreContainer = document.getElementById("mannerScoreContainer");
        const mannerScoreBar = document.querySelector(".manner-score-bar");
        const mannerScoreFill = mannerScoreBar?.querySelector(".manner-score-fill");
        const mannerScoreText = document.querySelector(".manner-score-text");
        const mannerScoreDescription = mannerScoreContainer?.querySelector(".manner-score-description");
		const headingScoreText = document.getElementById("mannerScoreHeadingText");
		if (headingScoreText) {
		    headingScoreText.textContent = `${mannerScore}점`;
		}

        if (!mannerScoreBar || !mannerScoreFill || !mannerScoreText) return;
        const scorePercent = Math.min(100, Math.max(0, mannerScore));
        mannerScoreFill.style.width = `${scorePercent}%`;
        
        mannerScoreText.style.left = `${scorePercent}%`;
        mannerScoreText.textContent = `${mannerScore}점`;
		// ✅ 여기에서 프로필 상단에도 점수 반영!
		const profileScore = document.getElementById("mannerScore");
		if (profileScore) {
		    profileScore.textContent = `매너 점수: ${mannerScore}점`;
		}

        mannerScoreFill.classList.remove("score-low", "score-medium", "score-high");
        if (mannerScore < 36) {
            mannerScoreFill.classList.add("score-low");
            if (mannerScoreDescription) mannerScoreDescription.textContent = "매너 온도가 낮습니다. 활동을 통해 온도를 높여보세요.";
        } else if (mannerScore <= 70) {
            mannerScoreFill.classList.add("score-medium");
            if (mannerScoreDescription) mannerScoreDescription.textContent = "기본 매너 온도를 유지하고 있습니다.";
        } else {
            mannerScoreFill.classList.add("score-high");
            if (mannerScoreDescription) mannerScoreDescription.textContent = "매너 온도가 매우 높습니다. 활발한 활동에 감사합니다!";
        }
    }

    function displayReviews(reviews) {
        const reviewsContainer = document.getElementById("reviewList");
        if (!reviewsContainer) return;
        reviewsContainer.innerHTML = "";
        if (!reviews || reviews.length === 0) {
            reviewsContainer.innerHTML = "<p>후기가 없습니다.</p>";
        } else {
            reviews.forEach(review => {
                const score = typeof review.score === 'number' ? review.score : 0;
                const starCount = Math.max(0, score);
                const userId = review.userId;
                const userName = review.userName || '익명';
                const userImage = review.userImage ? `/member/images/${review.userImage}` : `<img src="<span class='emoji-frog'>🐸</span>" />`;
                const userProfileUrl = `/member/details?userId=${userId}`;
                
                const reviewElement = document.createElement("div");
                reviewElement.classList.add("review-item");
                reviewElement.innerHTML = `
                    <div class="review-user-info">
                        <div class="review-user-img">
                            <a href="${userProfileUrl}">
                                <img src="${userImage}" alt="리뷰어 프로필">
                            </a>
                        </div>
                        <div class="review-user-meta">
                             <a href="${userProfileUrl}" class="review-user-name">${userName}</a>
                            <div class="review-score">
                                ${'⭐️'.repeat(starCount)} (${score}점)
                            </div>
                        </div>
                    </div>
                    <div class="review-content">${review.content || '리뷰 내용 없음'}</div>
                `;
                reviewsContainer.appendChild(reviewElement);
            });
        }
    }

    function bindReservationCardEvents() {
        document.querySelectorAll(".tab-content:nth-child(3) .reservation-card").forEach(card => {
            const statusInput = card.querySelector(".reservation-status");
            const steps = card.querySelectorAll(".status-steps .step");
            const dealButtons = card.querySelector(".deal-buttons");
            const btnChat = card.querySelector(".btn-chat");

            if (!statusInput || !steps || !dealButtons || !btnChat) return;

            const status = parseInt(statusInput.value);
            steps.forEach(step => step.classList.remove("completed"));

            if (status === 0 || status === 1) {
                steps[1].classList.add("completed");
            } else if (status === 2) {
                steps[2].classList.add("completed");
                dealButtons.style.display = "flex";
            } else if (status === 3) {
                steps[3].classList.add("completed");
            } else if (status === 4) {
                steps[4].classList.add("completed");
            }

            btnChat.addEventListener("click", (event) => {
                openChatRoomByReservation(event);
            });
        });
    }

    window.openChatRoomByReservation = async function(event) {
        const reservationId = event.target.getAttribute('data-id');
        if (!reservationId) {
            alert('예약 정보를 찾을 수 없습니다.');
            return;
        }
        const currentUserId = document.body.dataset.userId;
        if (!currentUserId) {
            alert('로그인이 필요합니다.');
            return;
        }
        try {
            const res = await fetch(`/api/chat/rooms/by-reservation`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    reservationId: parseInt(reservationId),
                    currentUserId: parseInt(currentUserId)
                })
            });
            if (!res.ok) {
                if (res.status === 403) {
                    alert("이 예약에 대한 채팅 권한이 없습니다.");
                    return;
                }
                throw new Error("채팅방 요청 실패");
            }
            const chatRoom = await res.json();
            const chatRoomId = chatRoom.id;
            window.location.href = `/chat?chatRoomId=${chatRoomId}`;
        } catch (error) {
            console.error("채팅방 요청 오류:", error);
            alert("채팅방 연결 중 오류가 발생했습니다.");
        }
    };

    // ✅ 거래완료 버튼 → 후기 작성 팝업 열기
	window.openCompletePopup = function (btn) {
	    const card = btn.closest(".reservation-card");
	    if (!card) return;

	    const reservationId = card.querySelector("input[name='reservationId']")?.value;
        console.log('reservationId: ', reservationId);
	    const targetId = card.querySelector("input[name='targetId']")?.value;
        console.log('targetId: ', targetId);

	    if (!reservationId || !targetId) {
	        alert("예약 정보가 누락되었습니다.");
	        return;
	    }

	    // ✅ 상태 값을 3(거래완료)로 변경
	    const statusInput = card.querySelector(".reservation-status");
	    const steps = card.querySelectorAll(".status-steps .step");
	    const dealButtons = card.querySelector(".deal-buttons");

	    if (statusInput) statusInput.value = "3";
		
		// 🔽 여기에 추가
		fetch(`/reservation/finish?reservationId=${reservationId}`)
		  .then(response => {
		    if (!response.ok) throw new Error("거래완료 상태 업데이트 실패");
		  })
		  .catch(error => {
		    console.error("상태 업데이트 오류:", error);
		    alert("거래완료 상태를 서버에 저장하는 데 실패했습니다.");
		  });

		// UI 상태바 업데이트 (이후 그대로 유지)
		steps.forEach(step => step.classList.remove("completed"));
		steps[3].classList.add("completed");

		if (dealButtons) {
		  dealButtons.style.display = "none";
		}

	    // 상태바 UI 업데이트
	    steps.forEach(step => step.classList.remove("completed"));
	    steps[3].classList.add("completed"); // '거래완료' 단계 표시

	    if (dealButtons) {
	        dealButtons.style.display = "none"; // 버튼 숨김
	    }

	    // 팝업 열기
	    const popupWidth = 500;
	    const popupHeight = 600;
	    const screenWidth = window.screen.availWidth;
	    const screenHeight = window.screen.availHeight;
	    const screenLeft = window.screen.availLeft || 0;
	    const screenTop = window.screen.availTop || 0;

	    const left = screenLeft + (screenWidth - popupWidth) / 2;
	    const top = screenTop + (screenHeight - popupHeight) / 2;
	    const popupOptions = `width=${popupWidth},height=${popupHeight},top=${top},left=${left},scrollbars=no,resizable=no`;

	    const url = `/review?reservationId=${reservationId}&targetId=${targetId}&owner=0`;
	    window.open(url, "거래 완료 및 후기", popupOptions);
	};
    

    // ✅ 거래완료 버튼 → 후기 작성 팝업 열기(예약현황)
    window.openCompletePopupForResReq = function (btnSendReview) {
        if (!btnSendReview) return;

        const reservationIdData = btnSendReview.getAttribute('data-id');
        const targetIdData = btnSendReview.getAttribute('data-renter-id');

        if (!reservationIdData || !targetIdData) {
            alert("예약 정보가 누락되었습니다.");
            return;
        }

        // 팝업 열기
        const popupWidth = 500;
        const popupHeight = 600;
        const screenWidth = window.screen.availWidth;
        const screenHeight = window.screen.availHeight;
        const screenLeft = window.screen.availLeft || 0;
        const screenTop = window.screen.availTop || 0;

        const left = screenLeft + (screenWidth - popupWidth) / 2;
        const top = screenTop + (screenHeight - popupHeight) / 2;
        const popupOptions = `width=${popupWidth},height=${popupHeight},top=${top},left=${left},scrollbars=no,resizable=no`;

        const url = `/review?reservationId=${reservationIdData}&targetId=${targetIdData}&owner=1`;
        window.open(url, "거래 완료 및 후기", popupOptions);
    };

	window.completeReview = function () {
	  const currentTab = document.querySelector(".tab.active");
	  const currentTabIndex = Array.from(document.querySelectorAll(".tab")).indexOf(currentTab);

	  // 예약현황 탭이면
	  if (currentTabIndex === 1) {
	    if (typeof getAllReservationRequests === 'function') {
	      getAllReservationRequests(0); // 예약 카드 다시 불러오기
	    }
	  }
	}


});
