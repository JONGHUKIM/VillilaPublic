// 슬라이드 & 하트 기능
let slideIndex = 0;

document.addEventListener("DOMContentLoaded", function() {
    // 슬라이드쇼 초기화
    if (document.querySelectorAll(".slide-image").length > 0) {
        showSlides(slideIndex);
    }

    // 모달 기능 초기화
    initializeModal();

    // 버튼 이벤트 리스너 설정
    initializeButtons();

    // 지도 초기화
    initializeMap();
	
	// 찜 상태 확인
	initializeHeartState();
});

// 지도 초기화
function initializeMap() {
	
    const container = document.getElementById('map');
	let retryCount = 0;
	const maxRetries = 10;
	
    if (!container) return;

    // Kakao Maps API가 로드되었는지 확인
    function tryInitializeMap() {
		if (retryCount++ > maxRetries) {
		    console.error('Kakao Maps API 로드 실패: 최대 재시도 횟수 초과');
		    return;
		}
		if (typeof kakao === 'undefined' || !kakao.maps) {
		    console.log('Kakao Maps API가 아직 로드되지 않았습니다. 0.5초 후 재시도합니다.');
		    setTimeout(tryInitializeMap, 500);
		    return;
		}

        const latitude = parseFloat(container.getAttribute('data-lat'));
        const longitude = parseFloat(container.getAttribute('data-lng'));

        if (isNaN(latitude) || isNaN(longitude)) {
            console.error('유효한 위도/경도 값이 없습니다.');
            return;
        }

        const options = {
            center: new kakao.maps.LatLng(latitude, longitude),
            level: 3
        };

        const map = new kakao.maps.Map(container, options);

        const control = new kakao.maps.ZoomControl();
        map.addControl(control, kakao.maps.ControlPosition.TOPRIGHT);

        const marker = new kakao.maps.Marker({
            map: map,
            position: new kakao.maps.LatLng(latitude, longitude)
        });
    }

    tryInitializeMap();
}

function changeSlide(n) {
    showSlides(slideIndex += n);
}

function currentSlide(n) {
    showSlides(slideIndex = n - 1);
}

function showSlides(n) {
    const slides = document.querySelectorAll(".slide-image");
    const dots = document.querySelectorAll(".dot");

    if (slides.length === 0) return;

    if (n >= slides.length) slideIndex = 0;
    if (n < 0) slideIndex = slides.length - 1;
    
    slides.forEach(slide => slide.style.display = "none");
    dots.forEach(dot => dot.classList.remove("active"));

    slides[slideIndex].style.display = "block";
    if (dots.length > 0 && slideIndex < dots.length) {
        dots[slideIndex].classList.add("active");
    }
}

// 모달 기능 초기화
function initializeModal() {
    const slides = document.querySelectorAll(".slide-image");
    const modal = document.getElementById("imageModal");
    
    if (!modal) return;
    
    const modalImg = document.getElementById("modalImg");
    const closeBtn = document.querySelector(".close");
    const prevBtn = document.querySelector(".modal-prev");
    const nextBtn = document.querySelector(".modal-next");

    let currentIndex = 0;

    // 이미지 클릭 시 모달 열기
    slides.forEach((slide, index) => {
        slide.addEventListener("click", function() {
            currentIndex = index;
            openModal();
        });
    });

    function openModal() {
        modal.style.display = "flex";
        updateModalImage();
    }

    function closeModal() {
        modal.style.display = "none";
    }

    function updateModalImage() {
        if (modalImg && slides[currentIndex]) {
            modalImg.src = slides[currentIndex].src;
        }
    }

    function showNextImage() {
        currentIndex = (currentIndex + 1) % slides.length;
        updateModalImage();
    }

    function showPrevImage() {
        currentIndex = (currentIndex - 1 + slides.length) % slides.length;
        updateModalImage();
    }

    if (closeBtn) closeBtn.addEventListener("click", closeModal);
    if (prevBtn) prevBtn.addEventListener("click", showPrevImage);
    if (nextBtn) nextBtn.addEventListener("click", showNextImage);

    modal.addEventListener("click", function(event) {
        if (event.target === modal) {
            closeModal();
        }
    });
}

// 버튼 이벤트 리스너 설정
function initializeButtons() {
    // 삭제 버튼 초기화
    initializeDeleteButton("deleteBagBtn", "/post/delete/bag");
    initializeDeleteButton("deleteCarBtn", "/post/delete/car");
}

// 찜 상태 초기화 함수
function initializeHeartState() {
    const heartBtn = document.querySelector(".heart-btn");
    if (!heartBtn) return;

    const productId = heartBtn.getAttribute("data-product-id");
    if (!productId) {
        console.warn("productId 속성이 하트 버튼에 없습니다.");
        return;
    }

    fetch(`/api/like/check?id=${productId}`)
        .then(res => res.json()) // true 또는 false
        .then(isLiked => {
            if (isLiked) {
                heartBtn.classList.add("active");
                heartBtn.textContent = "❤️";
            } else {
                heartBtn.classList.remove("active");
                heartBtn.textContent = "🤍";
            }
        })
        .catch(err => {
            console.error("찜 상태 확인 실패:", err);
        });
}


// 삭제 버튼 초기화 함수
function initializeDeleteButton(buttonId, endpoint) {
    const deleteBtn = document.getElementById(buttonId);
    if (!deleteBtn) return;
    
    deleteBtn.addEventListener('click', () => {
        const itemId = deleteBtn.getAttribute("data-id");
        if (!itemId) {
            alert('삭제할 항목 ID를 찾을 수 없습니다.');
            return;
        }
        
        if (!confirm('정말 삭제할까요?')) return;
    
        fetch(`${endpoint}?id=${itemId}`, {
            method: 'DELETE'
        })
        .then(res => {
            if (res.ok) {
                alert('삭제되었습니다.');
                location.href = '/mypage';
            } else {
                return res.text().then(text => {
                    throw new Error(text || '신청된 예약을 처리 후 삭제해주세요.');
                });
            }
        })
        .catch(error => {
            alert(error.message || '삭제 중 오류가 발생했습니다.');
            console.error('삭제 오류:', error);
        });
    });
}

// 찜하기 기능
function toggleHeart(productId) {
    if (!productId) {
        console.error("유효하지 않은 상품 ID입니다.");
        return;
    }
    
    const heartBtn = document.querySelector(".heart-btn");
    if (!heartBtn) {
        console.error("하트 버튼을 찾을 수 없습니다.");
        return;
    }
    
    const endpoint = heartBtn.classList.contains("active") 
        ? `/api/like/no?id=${productId}`
        : `/api/like/yes?id=${productId}`;
        
    fetch(endpoint)
        .then(response => {
            if (!response.ok) {
                throw new Error("서버 응답 오류");
            }
            return response;
        })
        .then(() => {
            if (heartBtn.classList.contains("active")) {
                heartBtn.classList.remove("active");
                heartBtn.textContent = "🤍"; // 찜 해제 화이트
            } else {
                heartBtn.classList.add("active");
                heartBtn.textContent = "❤️"; // 찜 등록
            }
        })
        .catch(error => {
            console.error("좋아요 처리 실패:", error);
        });
}

// 예약신청 팝업
function openReservationPopup() {
    const urlParams = new URLSearchParams(window.location.search);
    const productId = urlParams.get("id");

    if (!productId) {
        alert("차량 정보를 찾을 수 없습니다.");
        return;
    }
    
    // 팝업 창의 크기
    const popupWidth = 500;
    const popupHeight = 600;

    // 사용 가능한 화면 크기와 위치 계산 (다중 모니터 환경 고려)
    const screenWidth = window.screen.availWidth;
    const screenHeight = window.screen.availHeight;
    const screenLeft = window.screen.availLeft || 0;
    const screenTop = window.screen.availTop || 0;

    // 화면의 중앙 위치 계산
    const left = screenLeft + (screenWidth - popupWidth) / 2;
    const top = screenTop + (screenHeight - popupHeight) / 2;

    // 팝업 창 옵션 문자열 생성
    const popupOptions = `width=${popupWidth},height=${popupHeight},top=${top},left=${left},scrollbars=yes,resizable=yes`;

    // productId 포함한 URL로 팝업 열기
    window.open(`/reservation?productId=${productId}`, "예약 신청", popupOptions);
}