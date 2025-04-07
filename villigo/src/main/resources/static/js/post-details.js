//슬라이드 & 하트 기능
   
let slideIndex = 0;
showSlides(slideIndex);

function changeSlide(n) {
    showSlides(slideIndex += n);
}

function currentSlide(n) {
    showSlides(slideIndex = n - 1);
}

function showSlides(n) {
    let slides = document.querySelectorAll(".slide-image");
    let dots = document.querySelectorAll(".dot");
    
    if (n >= slides.length) { slideIndex = 0 }
    if (n < 0) { slideIndex = slides.length - 1 }

    slides.forEach(slide => slide.style.display = "none");
    dots.forEach(dot => dot.classList.remove("active"));

    slides[slideIndex].style.display = "block";
    dots[slideIndex].classList.add("active");
}

// 찜하기 기능
function toggleHeart(productId) {
    console.log(productId);
    let heartBtn = document.querySelector(".heart-btn");
    if (heartBtn.classList.contains("active")) {
        fetch(`/api/like/no?id=${productId}`)
        .then(() => {
            heartBtn.classList.remove("active");
            heartBtn.textContent = "🤍"; // 찜 해제 화이트
        })
        .catch(error => {
            console.error("좋아요 해제 실패:", error);
        });

    } else {
        fetch(`/api/like/yes?id=${productId}`)
        .then((response) => response)
        .then((data) => {
            console.log(data);
            heartBtn.classList.add("active");
            heartBtn.textContent = "❤️"; // 찜 등록
        })
        .catch(error => {
            console.error("좋아요 등록 실패:", error);
        });
    }
}
// ✅ 예약신청 - carId를 URL 파라미터로 팝업으로 전달
function openReservationPopup() {
    // carId를 HTML에서 받아옴
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
    const popupOptions = `width=${popupWidth},height=${popupHeight},top=${top},left=${left},scrollbars=no,resizable=no`;

	  // 🔥 carId 포함한 URL로 팝업 열기
	    window.open(`/reservation?productId=${productId}`, "예약 신청", popupOptions);
	}

// 지도 표시 기능
var container = document.getElementById('map');
var latitude = parseFloat(container.getAttribute('data-lat'));
var longitude = parseFloat(container.getAttribute('data-lng'));
console.log(latitude, longitude);
var options = {
    center: new kakao.maps.LatLng(latitude, longitude),
    level: 3
};

var map = new kakao.maps.Map(container, options);

var control = new kakao.maps.ZoomControl();
map.addControl(control, kakao.maps.ControlPosition.TOPRIGHT);

var marker = new kakao.maps.Marker({
    map: map,
    position: new kakao.maps.LatLng(latitude, longitude)
});

// 가방 삭제하기 버튼
const deleteBagBtn = document.getElementById("deleteBagBtn")
if(deleteBagBtn) {
    deleteBagBtn.addEventListener('click', () => {
        console.log(deleteBagBtn.getAttribute("data-id"));
        if (!confirm('정말 삭제할까요?')) return;
    
        fetch(`/post/delete/bag?id=${deleteBagBtn.getAttribute("data-id")}`, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                
                alert('삭제되었습니다.');
                location.href = '/mypage'; // 이동 경로 조정
            } else {
                alert('신청된 예약을 처리 후 삭제해주세요.');
            }
        });
    });
}

// 차 삭제하기 버튼
const deleteCarBtn = document.getElementById("deleteCarBtn")
if(deleteCarBtn) {
    deleteCarBtn.addEventListener('click', () => {
        console.log(deleteCarBtn.getAttribute("data-id"));
        if (!confirm('정말 삭제할까요?')) return;
    
        fetch(`/post/delete/bag?id=${deleteCarBtn.getAttribute("data-id")}`, {
            method: 'DELETE'
        }).then(res => {
            if (res.ok) {
                console.log(res.ok.valueOf);
                alert('삭제되었습니다.');
                location.href = '/mypage'; // 이동 경로 조정
            } else {
                alert('신청된 예약을 처리 후 삭제해주세요.');
            }
        });
    });
}
