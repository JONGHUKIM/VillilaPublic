document.addEventListener('DOMContentLoaded', function () {
    // 수수료 계산 로직 수정
    const feeInput = document.getElementById('fee');
    const feeWithCommissionSpan = document.getElementById('feeWithCommission');

    if (feeInput && feeWithCommissionSpan) {
        feeInput.addEventListener('input', function () {
            const fee = parseFloat(feeInput.value) || 0;
            const commissionFee = Math.round(fee * 1.05); // 5% 수수료 추가 및 반올림
            feeWithCommissionSpan.textContent = `책정은 분 당 가격: ${commissionFee.toLocaleString()}원`; // 입력 후 "책정가격: X원" 표시
        });
    }

    // 기존 코드
    function toggleCustomBrand() {
        const brandSelect = document.getElementById("brand");
        const customBrandInput = document.getElementById("customBrand");
        customBrandInput.style.display = brandSelect.value === "0" ? "block" : "none";
    }
    
    // 브랜드 변경 이벤트 리스너 추가
    const brandSelect = document.getElementById("brand");
    if (brandSelect) {
        brandSelect.addEventListener("change", toggleCustomBrand);
        // 페이지 로드시 초기 상태 설정
        toggleCustomBrand();
    }

    // 모든 컬러 버튼 가져오기
    const colorCircles = document.querySelectorAll('.color-circle');
    let selectedColor = null; // 현재 선택된 색상 저장

    colorCircles.forEach(circle => {
        circle.addEventListener('click', function () {
            // 기존 선택된 버튼의 클래스 제거
            document.querySelectorAll('.color-circle.selected').forEach(selected => {
                selected.classList.remove('selected');
            });
            // 클릭한 버튼에 selected 클래스 추가
            this.classList.add('selected');

            // 선택한 색상 저장 (UI에는 표시하지 않음)
            selectedColor = this.getAttribute('data-color-id');
            document.getElementById("selectedColor").value = selectedColor;

            // 백엔드에서 사용할 색상 값 (개발자 도구 콘솔에서 확인 가능)
            console.log("선택한 색상:", selectedColor);
        });
    });

    // 차 연식
    const yearDisplay = document.getElementById('car-year');
    const btnDecrement = document.getElementById('year-decrement');
    const btnIncrement = document.getElementById('year-increment');

    let currentYear = 2025;
    const minYear = 1940;
    const maxYear = 2025;
    let interval = null; // 자동 증가/감소를 위한 변수

    function changeYear(step) {
        if ((step === -1 && currentYear > minYear) || (step === 1 && currentYear < maxYear)) {
            currentYear += step;
            yearDisplay.textContent = currentYear;
            document.getElementById("yearInput").value = currentYear;
        }
    }

    if (btnDecrement) {
        btnDecrement.addEventListener('click', () => changeYear(-1));
        btnDecrement.addEventListener('mousedown', () => startHold(-1));
        btnDecrement.addEventListener('mouseup', stopHold);
        btnDecrement.addEventListener('mouseleave', stopHold);
    }
    if (btnIncrement) {
        btnIncrement.addEventListener('click', () => changeYear(1));
        btnIncrement.addEventListener('mousedown', () => startHold(1));
        btnIncrement.addEventListener('mouseup', stopHold);
        btnIncrement.addEventListener('mouseleave', stopHold);
    }

    function startHold(step) {
        interval = setInterval(() => changeYear(step), 150); // 0.15초마다 증가/감소
    }

    function stopHold() {
        clearInterval(interval);
    }

    // 주행여부 버튼
    const driveToggle = document.getElementById("driveStatus");
    const statusText = document.getElementById("status-text");
    const driveStatusInput = document.getElementById("driveStatusInput");

    if (driveToggle) {
        driveToggle.addEventListener("change", () => {
            statusText.textContent = driveToggle.checked ? "가능" : "불가능";
            driveStatusInput.value = driveToggle.checked ? "true" : "false";
        });
    }

    let selectedFiles = [];

    // 이미지 미리보기 함수
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
                wrapper.classList.add("position-relative", "d-inline-block", "me-2", "mb-2");

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
                badge.style.cursor = "pointer";

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

    // 이미지 업로드 이벤트 리스너 추가 - 핵심 수정 사항
    const uploadImageInput = document.getElementById('uploadImage');
    if (uploadImageInput) {
        uploadImageInput.addEventListener('change', previewImages);
    }

    // 주소 검색 기능
    const addressBtn = document.getElementById("addressBtn");
    if (addressBtn) {
        addressBtn.addEventListener('click', function () {
            new daum.Postcode({
                oncomplete: function (data) {
                    document.getElementById("zonecode").value = data.zonecode;
                    document.getElementById("sido").value = data.sido;
                    document.getElementById("fullAddress").value = data.address;
                    document.getElementById("sigungu").value = data.sigungu;
                    document.getElementById("bname").value = data.bname;
                }
            }).open();
        });
    }

    // 전체 주소 넣어주기
    const fullAddressInput = document.getElementById("fullAddress");
    if (fullAddressInput) {
        fullAddressInput.addEventListener('change', function () {
            const address = this.value;
            if (address) {
                // 주소 변경 시 필요한 추가 처리가 있다면 여기에 작성
            }
        });
    }

    // 내용 미입력 알림창
    const submitBtn = document.getElementById("submitBtn");
    if (submitBtn) {
        submitBtn.addEventListener('click', function (event) {
            event.preventDefault();

            const categoryNumInput = document.getElementById("categoryNum");
            const imagePreviewDiv = document.getElementById("imagePreview");
            const postNameInput = document.getElementById("postName");
            const productNameInput = document.getElementById("productName");
            const brandSelect = document.getElementById("brand");
            const customBrandInput = document.getElementById("customBrand");
            const selectedColorInput = document.getElementById("selectedColor");
            const feeInput = document.getElementById("fee");
            const minRentalTimeInput = document.getElementById("minRentalTime");
            const fullAddressInput = document.getElementById("fullAddress");

            if (imagePreviewDiv.innerHTML === "") {
                alert('사진을 하나 이상 첨부해주세요!');
                return;
            } else if (postNameInput.value === "") {
                alert('제목을 입력해주세요!');
                return;
            } else if (categoryNumInput.value == 1 && productNameInput.value === "") {
                alert('상품명을 입력해주세요!');
                return;
            } else if (categoryNumInput.value == 2 && productNameInput.value === "") {
                alert('차종을 입력해주세요!');
                return;
            } else if (brandSelect.value === "") {
                alert('브랜드를 선택해주세요!');
                return;
            } else if (brandSelect.value === "0" && customBrandInput.value.trim() === "") {
                alert('브랜드명을 입력해주세요!');
                return;
            } else if (selectedColorInput.value === "") {
                alert('색상을 선택해주세요!');
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

            // 폼 데이터 전송
            const form = document.getElementById("uploadForm");
            const formData = new FormData(form);

            // 기존 input[type=file]에 들어있는 거는 무시하고
            formData.delete("images");
			
            // selectedFiles에 있는 파일만 새로 추가
            selectedFiles.forEach(file => {
                formData.append("images", file);
            });

			fetch(form.action, {
			    method: "POST",
			    body: formData,
			    redirect: 'follow'
			})
			.then(response => {
			    if (response.redirected) {
			        return; // 리다이렉션이 처리되었으므로 추가 로직 없이 종료
			    }

			    // 그 외의 경우 (200 OK 등) 또는 리다이렉트가 아닌 최종 응답 처리
			    if (response.ok) {
			        const contentType = response.headers.get("content-type");
			        if (contentType && contentType.includes("application/json")) {
			            return response.json();
			        } else {
			            // JSON이 아닌 응답 (예: 본문 없는 200 OK)
			            return response.text().then(() => ({ success: true, message: "등록 완료" }));
			        }
			    } else {
			        // 서버에서 에러 응답 (4xx, 5xx)
			        const contentType = response.headers.get("content-type");
			        if (contentType && contentType.includes("application/json")) {
			            return response.json().then(errorData => {
			                throw new Error(errorData.message || `서버 오류: ${response.status}`);
			            });
			        } else {
			            return response.text().then(errorText => {
			                throw new Error(errorText || `서버 오류: ${response.status}`);
			            });
			        }
			    }
			})
			.then(data => {
			    // 리다이렉트되지 않고 이 블록으로 왔다면 (성공적인 JSON 또는 텍스트 응답)
			    alert(data.message || '상품이 성공적으로 등록되었습니다.');
			})
			.catch(error => {
			    console.error('에러 발생:', error);
			    // 이 alert는 실제 'Failed to fetch' 에러가 발생했을 때만 나타남
			    alert('서버와 통신 중 문제가 발생했습니다: ' + error.message);
			});
        });
    }
});