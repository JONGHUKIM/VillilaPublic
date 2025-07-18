document.addEventListener('DOMContentLoaded', function () {
    // 전역 변수 및 상수 정의
    let selectedFiles = [];
    let selectedColor = null;
    let currentYear = 2025;
    let interval = null;
    
    const CONFIG = {
        COMMISSION_RATE: 0.05,
        MIN_YEAR: 1940,
        MAX_YEAR: 2025,
        MAX_FILES: 10,
        MAX_SIZE_MB: 50,
        YEAR_CHANGE_INTERVAL: 150
    };

    // 수수료 계산 기능
    function initializeFeeCalculation() {
        const feeInput = document.getElementById('fee');
        const feeWithCommissionSpan = document.getElementById('feeWithCommission');

        if (!feeInput || !feeWithCommissionSpan) return;

        feeInput.addEventListener('input', function () {
            const fee = parseFloat(feeInput.value) || 0;
            const commissionFee = Math.round(fee * (1 + CONFIG.COMMISSION_RATE));
            feeWithCommissionSpan.textContent = `책정은 분 당 가격: ${commissionFee.toLocaleString()}원`;
        });
    }

    // 브랜드 선택 기능
    function initializeBrandSelection() {
        const brandSelect = document.getElementById("brand");
        const customBrandInput = document.getElementById("customBrand");
        
        if (!brandSelect || !customBrandInput) return;

        function toggleCustomBrand() {
            customBrandInput.style.display = brandSelect.value === "0" ? "block" : "none";
        }

        brandSelect.addEventListener("change", toggleCustomBrand);
        toggleCustomBrand(); // 초기 상태 설정
    }

    // 색상 선택 기능
    function initializeColorSelection() {
        const colorCircles = document.querySelectorAll('.color-circle');
        const selectedColorInput = document.getElementById("selectedColor");
        
        if (!colorCircles.length || !selectedColorInput) return;

        colorCircles.forEach(circle => {
            circle.addEventListener('click', function () {
                // 기존 선택 해제
                document.querySelectorAll('.color-circle.selected').forEach(selected => {
                    selected.classList.remove('selected');
                });
                
                // 새 선택 적용
                this.classList.add('selected');
                selectedColor = this.getAttribute('data-color-id');
                selectedColorInput.value = selectedColor;
                
                console.log("선택한 색상:", selectedColor);
            });
        });
    }

    // 연식 선택 기능
    function initializeYearSelection() {
        const yearDisplay = document.getElementById('car-year');
        const btnDecrement = document.getElementById('year-decrement');
        const btnIncrement = document.getElementById('year-increment');
        const yearInput = document.getElementById("yearInput");

        if (!yearDisplay || !btnDecrement || !btnIncrement || !yearInput) return;

        function changeYear(step) {
            const newYear = currentYear + step;
            if (newYear >= CONFIG.MIN_YEAR && newYear <= CONFIG.MAX_YEAR) {
                currentYear = newYear;
                yearDisplay.textContent = currentYear;
                yearInput.value = currentYear;
            }
        }

        function startHold(step) {
            interval = setInterval(() => changeYear(step), CONFIG.YEAR_CHANGE_INTERVAL);
        }

        function stopHold() {
            if (interval) {
                clearInterval(interval);
                interval = null;
            }
        }

        // 이벤트 리스너 등록
        btnDecrement.addEventListener('click', () => changeYear(-1));
        btnDecrement.addEventListener('mousedown', () => startHold(-1));
        btnDecrement.addEventListener('mouseup', stopHold);
        btnDecrement.addEventListener('mouseleave', stopHold);

        btnIncrement.addEventListener('click', () => changeYear(1));
        btnIncrement.addEventListener('mousedown', () => startHold(1));
        btnIncrement.addEventListener('mouseup', stopHold);
        btnIncrement.addEventListener('mouseleave', stopHold);
    }

    // 주행 상태 토글 기능
    function initializeDriveStatusToggle() {
        const driveToggle = document.getElementById("driveStatus");
        const statusText = document.getElementById("status-text");
        const driveStatusInput = document.getElementById("driveStatusInput");

        if (!driveToggle || !statusText || !driveStatusInput) return;

        driveToggle.addEventListener("change", () => {
            const isEnabled = driveToggle.checked;
            statusText.textContent = isEnabled ? "가능" : "불가능";
            driveStatusInput.value = isEnabled ? "true" : "false";
        });
    }

    // 이미지 업로드 및 미리보기 기능
    function initializeImageUpload() {
        const uploadImageInput = document.getElementById('uploadImage');
        const previewContainer = document.getElementById('imagePreview');
        
        if (!uploadImageInput || !previewContainer) return;

        uploadImageInput.addEventListener('change', handleImageUpload);
    }

    function handleImageUpload() {
        const input = document.getElementById('uploadImage');
        const previewContainer = document.getElementById('imagePreview');
        const newFiles = Array.from(input.files);
        
        // 파일 개수 검증
        if (selectedFiles.length + newFiles.length > CONFIG.MAX_FILES) {
            alert(`이미지는 최대 ${CONFIG.MAX_FILES}개까지 첨부 가능합니다.`);
            input.value = "";
            return;
        }

        // 파일 크기 및 중복 검증
        for (let file of newFiles) {
            if (!validateFile(file)) {
                input.value = "";
                return;
            }
        }

        // 이미지 미리보기 생성
        newFiles.forEach(file => {
            createImagePreview(file, previewContainer);
            selectedFiles.push(file);
        });

        input.value = "";
    }

    function validateFile(file) {
        const fileSizeMB = file.size / (1024 * 1024);
        
        if (fileSizeMB > CONFIG.MAX_SIZE_MB) {
            alert(`이미지 ${file.name}의 크기가 너무 큽니다. (최대 ${CONFIG.MAX_SIZE_MB}MB)`);
            return false;
        }

        if (selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
            console.warn(`파일 ${file.name}은(는) 이미 선택되었습니다.`);
			alert(`파일 ${file.name}은(는) 이미 선택되었습니다.`);
            return false;
        }

        return true;
    }

    function createImagePreview(file, container) {
        const reader = new FileReader();
        
        reader.onload = function (e) {
            const wrapper = createImageWrapper(e.target.result, file, container);
            container.appendChild(wrapper);
        };
        
        reader.readAsDataURL(file);
    }

    function createImageWrapper(imageSrc, file, container) {
        const wrapper = document.createElement("div");
        wrapper.classList.add("position-relative", "d-inline-block", "me-2", "mb-2", "image-wrapper");

        const img = document.createElement("img");
        img.src = imageSrc;
        img.style.cssText = "width: 120px; height: 120px; object-fit: contain;";
        img.classList.add("preview-image");

        const deleteButton = document.createElement("span");
        deleteButton.classList.add("position-absolute", "translate-middle", "badge", "rounded-pill", "bg-danger", "delete-badge");
        deleteButton.style.cssText = "top: 10%; left: 90%; cursor: pointer;";
        deleteButton.textContent = "X";
        
        deleteButton.addEventListener("click", function () {
            wrapper.remove();
            selectedFiles = selectedFiles.filter(f => f !== file);
            console.log("파일 삭제 후 selectedFiles:", selectedFiles);
        });

        wrapper.appendChild(img);
        wrapper.appendChild(deleteButton);
        
        return wrapper;
    }

    // 주소 검색 기능
    function initializeAddressSearch() {
        const addressBtn = document.getElementById("addressBtn");
        
        if (!addressBtn) return;

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

    // 폼 제출 기능
    function initializeFormSubmission() {
        const submitBtn = document.getElementById("submitBtn");
        
        if (!submitBtn) return;

        submitBtn.addEventListener('click', handleFormSubmission);
    }

    async function handleFormSubmission(event) {
        event.preventDefault();

        // 폼 검증
        if (!validateForm()) return;

        try {
            // 이미지 업로드
            const s3Keys = await uploadImagesToS3();
            
            // 폼 데이터 전송
            await submitFormData(s3Keys);
            
        } catch (error) {
            console.error("폼 제출 실패:", error);
            alert(`오류가 발생했습니다: ${error.message}`);
        }
    }

    function validateForm() {
        const validationRules = [
            { condition: selectedFiles.length === 0, message: '사진을 하나 이상 첨부해주세요!' },
            { condition: !getElementValue("postName"), message: '제목을 입력해주세요!' },
            { condition: isCategory(1) && !getElementValue("productName"), message: '상품명을 입력해주세요!' },
            { condition: isCategory(2) && !getElementValue("productName"), message: '차종을 입력해주세요!' },
            { condition: !getElementValue("brand"), message: '브랜드를 선택해주세요!' },
            { condition: getElementValue("brand") === "0" && !getElementValue("customBrand").trim(), message: '브랜드명을 입력해주세요!' },
            { condition: !getElementValue("selectedColor"), message: '색상을 선택해주세요!' },
            { condition: !getElementValue("fee"), message: '요금을 입력해주세요!' },
            { condition: isCategory(2) && !getElementValue("minRentalTime"), message: '최소시간을 입력해주세요!' },
            { condition: !getElementValue("fullAddress"), message: '주소를 입력해주세요!' }
        ];

        for (let rule of validationRules) {
            if (rule.condition) {
                alert(rule.message);
                return false;
            }
        }

        return true;
    }

    function getElementValue(id) {
        const element = document.getElementById(id);
        return element ? element.value : "";
    }

    function isCategory(categoryNum) {
        return getElementValue("categoryNum") == categoryNum;
    }

	async function uploadImagesToS3() {
	    const s3Keys = [];
	    const uploadPromises = selectedFiles.map(async file => {
	        try {
	            const uniqueFileName = generateUniqueFileName(file.name);
	            const targetS3Key = `product_images/${uniqueFileName}`;
	            const presignedUrl = await getPresignedUrl(targetS3Key, file.type);
	            await uploadFileToS3(presignedUrl.presignedUrl, file);
	            return presignedUrl.s3Key;
	        } catch (error) {
	            console.error(`파일 ${file.name} 업로드 실패:`, error);
	            throw new Error(file.name);
	        }
	    });

	    const results = await Promise.allSettled(uploadPromises);
	    results.forEach((result, index) => {
	        if (result.status === 'fulfilled') {
	            s3Keys.push(result.value);
	        } else {
	            alert(`파일 ${selectedFiles[index].name} 업로드 실패: ${result.reason}`);
	        }
	    });

	    return s3Keys;
	}

    async function getPresignedUrl(filename, contentType) {
        const response = await fetch(`/api/files/upload-url?filename=${encodeURIComponent(filename)}&contentType=${encodeURIComponent(contentType)}`, {
            method: "POST",
            headers: { 'Content-Type': 'application/json' }
        });

        if (!response.ok) {
            const errorData = await response.json();
            throw new Error(`Pre-signed URL 발급 실패: ${errorData.message || response.statusText}`);
        }

        return await response.json();
    }

    async function uploadFileToS3(presignedUrl, file) {
        const response = await fetch(presignedUrl, {
            method: "PUT",
            body: file,
            headers: { "Content-Type": file.type }
        });

        if (!response.ok) {
            throw new Error(`S3 업로드 실패: ${response.statusText}`);
        }
    }

    async function submitFormData(s3Keys) {
        const form = document.getElementById("uploadForm");
        const formData = new FormData(form);
        
        // 기존 이미지 파일 제거
        formData.delete("images");
        
        // S3 키들 추가
        s3Keys.forEach(key => {
            formData.append("imageS3Keys", key);
        });

        const response = await fetch(form.action, {
            method: "POST",
            body: formData,
            redirect: 'follow'
        });

        if (response.redirected) {
            window.location.href = response.url;
            return;
        }

        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || "상품 등록 실패");
        }

        if (data.redirectUrl) {
            window.location.href = data.redirectUrl;
        } else {
            alert("상품 등록에 성공했습니다!");
        }
    }

	function generateUniqueFileName(originalFileName) {
	    const dotIndex = originalFileName.lastIndexOf('.');
	    const extension = dotIndex > -1 ? originalFileName.substring(dotIndex) : '';
	    return `${crypto.randomUUID()}${extension}`;
	}

    // 초기화 함수 실행
    function initialize() {
        initializeFeeCalculation();
        initializeBrandSelection();
        initializeColorSelection();
        initializeYearSelection();
        initializeDriveStatusToggle();
        initializeImageUpload();
        initializeAddressSearch();
        initializeFormSubmission();
    }

    // 모든 기능 초기화
    initialize();
});