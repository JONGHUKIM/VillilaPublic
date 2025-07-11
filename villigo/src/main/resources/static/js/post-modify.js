/**
 * 문서 로드 완료 시 초기화 함수 실행
 */
window.onload = function() {
    initializeMap();
    initializeAddressSearch();
    initializeColorPicker();
    initializeDriveToggle();
    initializeDeleteButtons();
    initializeFormValidation();
};


/**
 * 이미지 미리보기 기능
 */
let selectedFiles = [];

function previewImages() {
    const input = document.getElementById('uploadImage');
    const previewContainer = document.getElementById('imagePreview');
    const maxFiles = 10;
    const maxSizeMB = 50;

    const newFiles = Array.from(input.files);
    const totalFiles = selectedFiles.length + newFiles.length;

    if (totalFiles > maxFiles) {
        alert(`이미지는 최대 ${maxFiles}개까지 첨부 가능합니다.`);
        input.value = "";
        return;
    }

    for (let file of newFiles) {
        const fileSizeMB = file.size / (1024 * 1024);
        if (fileSizeMB > maxSizeMB) {
            alert(`이미지 ${file.name}의 크기가 너무 큽니다. (최대 ${maxSizeMB}MB)`);
            input.value = "";
            return;
        }

        const reader = new FileReader();
        reader.onload = function (e) {
            const wrapper = document.createElement("div");
            wrapper.classList.add("position-relative", "d-inline-block", "me-2");

            const img = document.createElement("img");
            img.src = e.target.result;
            img.style.width = "120px";
            img.style.height = "120px";
            img.style.objectFit = "contain";
            img.classList.add("preview-image");

            const badge = document.createElement("span");
            badge.classList.add("position-absolute", "translate-middle", "badge", "rounded-pill", "bg-danger");
            badge.style.top = "10%";
            badge.style.left = "90%";
            badge.textContent = "X";

            badge.addEventListener("click", function () {
                const index = selectedFiles.indexOf(file);
                if (index > -1) {
                    selectedFiles.splice(index, 1);
                }
                previewContainer.removeChild(wrapper);
            });

            wrapper.appendChild(img);
            wrapper.appendChild(badge);
            previewContainer.appendChild(wrapper);
        };
        reader.readAsDataURL(file);

        selectedFiles.push(file);
    }

    input.value = "";
}


/**
 * 기존 이미지 삭제 버튼 초기화
 */
function initializeDeleteButtons() {
    const badges = document.querySelectorAll(".delete-badge");
    badges.forEach(function (badge) {
        badge.addEventListener("click", function () {
            const wrapper = badge.closest(".position-relative");
            if (wrapper) wrapper.remove();
        });
    });
}

/**
 * 컬러칩 초기화
 */
function initializeColorPicker() {
    const colorCircles = document.querySelectorAll(".color-circle");
    let selectedColor = "blue"; // 백엔드에서 기존 선택된 색상 불러오기

    // 기존 선택된 색상 적용
    colorCircles.forEach(circle => {
        if (circle.getAttribute("data-color") === selectedColor) {
            circle.classList.add("selected");
        }

        // 색상 선택 기능 추가
        circle.addEventListener("click", function () {
            // 기존 선택된 색상 제거
            document.querySelectorAll(".color-circle.selected").forEach(selected => {
                selected.classList.remove("selected");
            });

            // 새로 선택된 색상 적용
            this.classList.add("selected");
            selectedColor = this.getAttribute("data-color");

            console.log("선택된 색상:", selectedColor);
        });
    });
}

/**
 * 주행 가능 여부 토글 초기화
 */
function initializeDriveToggle() {
    const driveToggle = document.getElementById("driveStatus");
    if (!driveToggle) return;
    
    const statusText = document.getElementById("status-text");
    const driveStatusInput = document.getElementById("driveStatusInput");
    
    // 초기값 설정
    statusText.innerHTML = (JSON.parse(driveStatusInput.value)) ? "가능" : "불가능";
    
    // 토글 이벤트 설정
    driveToggle.addEventListener("change", () => {
        const driveStatus = driveToggle.checked;
        statusText.textContent = driveStatus ? "가능" : "불가능";
        driveStatusInput.value = driveStatus;
    });
}

/**
 * 카카오맵 초기화
 */
function initializeMap() {
    const container = document.getElementById('map');
    if (!container) return;

    if (typeof kakao === 'undefined' || !kakao.maps) {
        console.error('Kakao Maps API가 로드되지 않았습니다.');
        return;
    }

    let latitude = parseFloat(container.getAttribute('data-lat'));
    let longitude = parseFloat(container.getAttribute('data-lng'));

    // 위도/경도가 유효하지 않은 경우 기본값 설정 (예: 서울 시청)
    if (isNaN(latitude) || isNaN(longitude)) {
        console.warn('유효한 위도/경도 값이 없어 기본값으로 설정합니다.');
        latitude = 37.5665; // 서울 시청 위도
        longitude = 126.9780; // 서울 시청 경도
    }

    console.log("지도 초기화:", latitude, longitude);
    
    const options = {
        center: new kakao.maps.LatLng(latitude, longitude),
        level: 3
    };

    window.map = new kakao.maps.Map(container, options);
    window.marker = new kakao.maps.Marker({
        map: window.map,
        position: new kakao.maps.LatLng(latitude, longitude)
    });

    const control = new kakao.maps.ZoomControl();
    window.map.addControl(control, kakao.maps.ControlPosition.TOPRIGHT);
}

/**
 * 주소 검색 기능 초기화
 */
function initializeAddressSearch() {
    const addressBtn = document.getElementById("addressBtn");
    const fullAddressInput = document.getElementById("fullAddress");
    
    if (!addressBtn || !fullAddressInput) return;

    // 주소 검색 버튼 클릭 이벤트
    addressBtn.addEventListener('click', function() {
        new daum.Postcode({
            oncomplete: function(data) {
                document.getElementById("zonecode").value = data.zonecode;
                document.getElementById("sido").value = data.sido;
                document.getElementById("fullAddress").value = data.address;
                document.getElementById("sigungu").value = data.sigungu;
                document.getElementById("bname").value = data.bname;

                const changeEvent = new Event("change", { bubbles: true });
                fullAddressInput.dispatchEvent(changeEvent);
            }
        }).open();
    });

    // 주소 변경 시 좌표 업데이트 이벤트
    fullAddressInput.addEventListener('change', function() {
        const address = this.value;
        if (address) {
            fetch(`/api/address/latlng?addr=${address}`)
                .then(response => response.json())
                .then(data => {
                    document.getElementById("latitude").value = data.latitude;
                    document.getElementById("longitude").value = data.longitude;
                    
                    if (window.map && window.marker) {
                        const newLatLng = new kakao.maps.LatLng(data.latitude, data.longitude);
                        window.map.setCenter(newLatLng);
                        window.marker.setPosition(newLatLng);
                    }
                })
                .catch(error => {
                    console.error("좌표 변환 실패:", error);
                    alert("주소의 위치 정보를 가져올 수 없습니다.");
                });
        }
    });
}

/**
 * 폼 유효성 검사 초기화
 */
function initializeFormValidation() {
    const submitBtn = document.getElementById("submitBtn");
    if (!submitBtn) return;
    
    submitBtn.addEventListener('click', function(event) {
        event.preventDefault();

        const categoryNumInput = document.getElementById("categoryNum");
        const imagePreviewDiv = document.getElementById("imagePreview");
        const postNameInput = document.getElementById("postName");
        const feeInput = document.getElementById("fee");
        const minRentalTimeInput = document.getElementById("minRentalTime");
        const fullAddressInput = document.getElementById("fullAddress");
        
        if (imagePreviewDiv.childElementCount == 0 || imagePreviewDiv.innerHTML === "") {
            alert('사진을 하나 이상 첨부해주세요!');
            return;
        } else if (postNameInput.value === "") {
            alert('제목을 입력해주세요!');
            return;
        } else if (feeInput.value === "") {
            alert('요금을 입력해주세요!');
            return;
        } else if (categoryNumInput.value == 2 && minRentalTimeInput.value === "") {
            alert('최소시간을 입력해주세요!');
            return;
        } else if (fullAddressInput.value === "") {
            alert('주소를 입력해주세요!');
            return;
        }

        // 여기서 바로 FormData를 만들고 전송한다
        const form = document.getElementById("uploadForm");
        const formData = new FormData(form);

        formData.delete("images");
        selectedFiles.forEach(file => formData.append("images", file));

		fetch(form.action, {
		  method: "POST",
		  body: formData,
		  redirect: 'follow'
		})
		.then(response => {
		  if (response.redirected) {
		    window.location.href = response.url; // 리다이렉트된 URL로 이동
		    return;
		  }
		    return response.json();
		  })
		  .then(data => {
		    if (data && data.redirectUrl) {
		      window.location.href = data.redirectUrl;
		    }
		  })
		  .catch(error => {
		    alert("서버와 통신 중 문제가 발생했습니다: " + error.message);
		  });


    });
}
