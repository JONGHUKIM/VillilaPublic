//  미리보기 기능 추가 (바꾸셔도됩니당)
function previewImages() {
    const input = document.getElementById('uploadImage');
    const previewContainer = document.getElementById('imagePreview');

    const maxFiles = 10;
    const maxSizeMB = 10;

    const files = Array.from(input.files);
    const attachedImgNum = document.querySelectorAll(".preview-container input[name='existingImageIds']").length;

    if(files.length + attachedImgNum > 10) {
        alert(`이미지는 최대 ${maxFiles}개 첨부 가능합니다.`);
        input.value = "";
        return;
    }

    for(let file of files) {
        const fileSizeMB = file.size / (1024 * 1024);
        if(fileSizeMB > maxSizeMB) {
            alert(`이미지 ${file.name}의 크기가 너무 큽니다.\n (최대 1MB)`)
            input.value = "";
            return;
        }
    }
        if (input.files.length > 0) {
        for (let file of input.files) {
            if (!file.type.startsWith("image/")) continue;

            const reader = new FileReader();
            reader.onload = function (e) {
                const wrapper = document.createElement("div");
                wrapper.classList.add("position-relative", "d-inline-block", "me-2");

                // 이미지 생성
                const img = document.createElement("img");
                img.src = e.target.result;
                img.style.width = "120px";
                img.style.height = "120px";
                img.style.objectFit = "contain";
                img.classList.add("preview-image");

                // 배지 생성
                const badge = document.createElement("span");
                badge.classList.add("position-absolute", "translate-middle", "badge", "rounded-pill", "bg-danger");
                badge.style.top = "10%";
                badge.style.left = "90%";
                badge.textContent = "X";
                
                badge.addEventListener("click", function () {
                    previewContainer.removeChild(wrapper);
                });
                
                wrapper.appendChild(img);
                wrapper.appendChild(badge);
                previewContainer.appendChild(wrapper);
            };
            reader.readAsDataURL(file);
        }
    }
}

// 기존 이미지 제거 버튼
document.addEventListener("DOMContentLoaded", function () {
    const badges = document.querySelectorAll(".delete-badge");

    badges.forEach(function (badge) {
        badge.addEventListener("click", function () {
            const wrapper = badge.closest(".position-relative");
            if (wrapper) wrapper.remove();
        });
    });
});
    
// 컬러칩 (바꾸셔도됩니당)
document.addEventListener("DOMContentLoaded", function () {
    const colorCircles = document.querySelectorAll(".color-circle");
    let selectedColor = "blue"; //  백엔드에서 기존 선택된 색상 불러오기

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
});


//  주행 가능 여부 토글 기능
const driveToggle = document.getElementById("driveStatus");
let statusText = document.getElementById("status-text");
let driveStatusInput = document.getElementById("driveStatusInput");

if(driveToggle) {
    statusText.innerHTML = (JSON.parse(driveStatusInput.value)) ? "가능" : "불가능";
    driveToggle.addEventListener("change", () => {
        driveStatus = driveToggle.checked;
        statusText.textContent = driveStatus ? "가능" : "불가능";
        driveStatusInput.value = driveStatus;
    });
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

// 주소 검색 기능
document.getElementById("addressBtn").addEventListener('click', function() {
    new daum.Postcode({
        oncomplete: function(data) {
            document.getElementById("zonecode").value=data.zonecode;
            document.getElementById("sido").value=data.sido;
            document.getElementById("fullAddress").setAttribute("value", data.address);
            document.getElementById("fullAddress").value=data.address;
            document.getElementById("sigungu").value=data.sigungu;
            document.getElementById("bname").value=data.bname;

            const changeEvent = new Event("change", { bubbles: true });
            document.getElementById("fullAddress").dispatchEvent(changeEvent);
        }
    }).open();
});

// 주소 검색하면 전체주소 넣어주고 좌표 가져오기
document.getElementById("fullAddress").addEventListener('change', function() {
    const address = this.value;
    if (address) {
        fetch(`/api/address/latlng?addr=${address}`)
            .then(response => response.json())
            .then(data => {
                document.getElementById("latitude").value=data.latitude;
                document.getElementById("longitude").value=data.longitude;
                const newLatLng = new kakao.maps.LatLng(data.latitude, data.longitude);
                map.setCenter(newLatLng);
                marker.setPosition(newLatLng);
            })
            .catch(error => {
                console.error("좌표 변환 실패:", error);
                alert("주소의 위치 정보를 가져올 수 없습니다.");
            });
    }
});

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

// 내용 미입력 알림창
const submitBtn = document.getElementById("submitBtn");
submitBtn.addEventListener('click', function(event) {
    event.preventDefault();

    const categoryNumInput = document.getElementById("categoryNum");
    const imagePreviewDiv = document.getElementById("imagePreview");
    const postNameInput = document.getElementById("postName");
    const feeInput = document.getElementById("fee");
    const minRentalTimeInput = document.getElementById("minRentalTime");
    const fullAddressInput = document.getElementById("fullAddress");
    if(imagePreviewDiv.childElementCount == 0 || imagePreviewDiv.innerHTML === "") {
        alert('사진을 하나 이상 첨부해주세요!');
        return;
    } else if(postNameInput.value === "") {
        alert('제목을 입력해주세요!');
        return;
    } else if(feeInput.value === "") {
        alert('요금을 입력해주세요!');
        return;
    } else if(categoryNumInput.value == 2 && minRentalTimeInput.value === "") {
        alert('최소시간을 입력해주세요!');
        return;
    } else if(fullAddressInput.value === "") {
        alert('주소를 입력해주세요!');
        return;
    }
    document.getElementById("uploadForm").submit();

});
  