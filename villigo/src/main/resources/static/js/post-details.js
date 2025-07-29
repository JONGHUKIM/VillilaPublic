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
	
	// 찜 상태 확인
	initializeHeartState();
	
	initializePriceRounding(); // 요금 반올림 함수 초기화
	
	// 지도는 Kakao Maps가 완전히 로드된 후에만 초기화
	kakao.maps.load(function() {
	    initializeMap(); // 여기가 진짜 실행 시점
	});
	
	// Pre-signed URL 갱신 (4분 50초 후)
	setTimeout(refreshImageUrls, 4.8 * 60 * 1000);
});

// 요금을 10원 단위로 반올림하는 함수
function roundPriceTo10Won(price) {
    const numPrice = parseFloat(price);
    if (isNaN(numPrice)) return price;
    
    // 10으로 나눈 나머지가 5 이상이면 올림, 미만이면 내림
    const remainder = numPrice % 10;
    if (remainder >= 5) {
        return Math.ceil(numPrice / 10) * 10;
    } else {
        return Math.floor(numPrice / 10) * 10;
    }
}

// 동적으로 추가되는 요금에도 적용할 수 있는 함수
function formatPrice(price) {
    const rounded = roundPriceTo10Won(price);
    return rounded.toLocaleString('ko-KR') + '원';
}

// 요금 표시 요소들에 반올림 적용
function initializePriceRounding() {
    // 요금 섹션의 temp-box 안에 있는 strong 태그를 찾아서 처리
    const priceSection = document.querySelector('.section .section-title');
    if (priceSection && priceSection.textContent.includes('요금')) {
        const tempBox = priceSection.nextElementSibling;
        if (tempBox && tempBox.classList.contains('temp-box')) {
            const strongElement = tempBox.querySelector('strong');
            if (strongElement) {
                const originalText = strongElement.textContent;
                const priceMatch = originalText.match(/(\d+(?:,\d{3})*)\s*원/);
                
                if (priceMatch) {
                    const originalPrice = parseInt(priceMatch[1].replace(/,/g, ''));
                    const roundedPrice = roundPriceTo10Won(originalPrice);
                    const formattedPrice = roundedPrice.toLocaleString('ko-KR');
                    strongElement.textContent = formattedPrice + '원';
                }
            }
        }
    }
}

// Pre-signed URL 갱신 함수
function refreshImageUrls() {
    const slides = document.querySelectorAll(".slide-image");
    slides.forEach(slide => {
        const filePath = slide.dataset.filePath;
        if (filePath) {
            fetch(`/api/files/download-url?fileKey=${encodeURIComponent(filePath)}`, {
                headers: { 'Content-Type': 'application/json' }
            })
            .then(response => response.json())
            .then(data => {
                if (data.url) {
                    slide.src = data.url;
                } else {
                    slide.src = '/images/default-image.png';
                }
            })
            .catch(error => {
                console.error('Pre-signed URL 갱신 실패:', error);
                slide.src = '/images/default-image.png';
            });
        }
    });
}

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
            modalImg.src = slides[currentIndex].src || '/images/default-image.png';
        } else {
            modalImg.src = '/images/default-image.png';
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

window.openChatRoomWithOwner = async function(ownerId) {
    const currentUserId = document.body.dataset.userId; // body 태그에 data-user-id가 있다고 가정
    if (!currentUserId) {
        alert('채팅을 시작하려면 로그인해야 합니다.');
        window.location.href = '/login'; // 로그인 페이지로 리다이렉트
        return;
    }

    // String 타입으로 넘어올 수 있으므로, 비교 시에도 타입 일치 또는 형변환을 고려합니다.
    if (String(currentUserId) === String(ownerId)) {
        alert('자신에게 채팅을 보낼 수 없습니다.');
        return;
    }

    try {
        // 새로운 API 엔드포인트 호출: /api/chat/rooms/inquiry
        const response = await fetch(`/api/chat/rooms/inquiry`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                userId1: parseInt(currentUserId), // 숫자로 변환하여 전송
                userId2: parseInt(ownerId)       // 숫자로 변환하여 전송
            })
        });

        if (!response.ok) {
            throw new Error('채팅방 생성/조회에 실패했습니다.');
        }

        const chatRoom = await response.json();
        const chatRoomId = chatRoom.id;

        // 채팅방 페이지로 이동
        window.location.href = `/chat?chatRoomId=${chatRoomId}`;

    } catch (error) {
        console.error("채팅방 연결 오류:", error);
        alert("채팅방 연결 중 오류가 발생했습니다.");
    }
};
