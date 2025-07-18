document.addEventListener('DOMContentLoaded', function () {
    // 전역 변수 및 상수 정의
    let selectedFiles = [];
    let deletedImageIds = [];

    const CONFIG = {
        MAX_FILES: 10,
        MAX_SIZE_MB: 50
    };

    // 에러 메시지 표시 (Bootstrap Toast)
    function showError(message) {
        const toast = document.createElement("div");
        toast.className = "toast align-items-center text-white bg-danger border-0";
        toast.role = "alert";
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        document.body.appendChild(toast);
        new bootstrap.Toast(toast).show();
        setTimeout(() => toast.remove(), 5000);
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

        const totalFiles = previewContainer.querySelectorAll('.image-wrapper').length + newFiles.length;

        if (totalFiles > CONFIG.MAX_FILES) {
            showError(`이미지는 최대 ${CONFIG.MAX_FILES}개까지 첨부 가능합니다.`);
            input.value = "";
            return;
        }

        for (let file of newFiles) {
            if (!validateFile(file)) {
                input.value = "";
                return;
            }
        }

        newFiles.forEach(file => {
            createImagePreview(file, previewContainer);
            selectedFiles.push(file);
        });

        input.value = "";
    }

    function validateFile(file) {
        const fileSizeMB = file.size / (1024 * 1024);

        if (fileSizeMB > CONFIG.MAX_SIZE_MB) {
            showError(`이미지 ${file.name}의 크기가 너무 큽니다. (최대 ${CONFIG.MAX_SIZE_MB}MB)`);
            return false;
        }

        if (selectedFiles.some(f => f.name === file.name && f.size === file.size)) {
            showError(`파일 ${file.name}은(는) 이미 선택되었습니다.`);
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

        reader.onerror = function () {
            showError(`파일 ${file.name}을 읽는 중 오류가 발생했습니다.`);
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

    // 기존 이미지 삭제 버튼 초기화
    function initializeDeleteButtons() {
        const previewContainer = document.getElementById("imagePreview");
        if (!previewContainer) return;

        previewContainer.addEventListener("click", function (event) {
            if (event.target.classList.contains("delete-badge")) {
                const wrapper = event.target.closest(".image-wrapper");
                if (wrapper) {
                    const existingImageIdInput = wrapper.querySelector('input[name="existingImageIds"]');
                    if (existingImageIdInput) {
                        const imageId = existingImageIdInput.value;
                        console.log(`기존 이미지 ID ${imageId} 삭제됨`);
                        deletedImageIds.push(imageId);
                        existingImageIdInput.remove();
                    }
                    wrapper.remove();
                }
            }
        });
    }

    // 주소 검색 기능
    function initializeAddressSearch() {
        const addressBtn = document.getElementById("addressBtn");
        const fullAddressInput = document.getElementById("fullAddress");

        if (!addressBtn || !fullAddressInput) return;

        addressBtn.addEventListener('click', function () {
            new daum.Postcode({
                oncomplete: function (data) {
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

        fullAddressInput.addEventListener('change', function () {
            const address = this.value;
            if (address) {
                fetch(`/api/address/latlng?addr=${encodeURIComponent(address)}`)
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
                        showError("주소의 위치 정보를 가져올 수 없습니다.");
                    });
            }
        });
    }

    // 카카오맵 초기화
    function initializeMap() {
        const container = document.getElementById('map');
        if (!container) return;

        if (typeof kakao === 'undefined' || !kakao.maps) {
            console.error('Kakao Maps API가 로드되지 않았습니다.');
            return;
        }

        let latitude = parseFloat(container.getAttribute('data-lat'));
        let longitude = parseFloat(container.getAttribute('data-lng'));

        if (isNaN(latitude) || isNaN(longitude)) {
            console.warn('유효한 위도/경도 값이 없어 기본값으로 설정합니다.');
            latitude = 37.5665;
            longitude = 126.9780;
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

    // 주행 상태 토글 기능
    function initializeDriveToggle() {
        const driveToggle = document.getElementById("driveStatus");
        const statusText = document.getElementById("status-text");
        const driveStatusInput = document.getElementById("driveStatusInput");

        if (!driveToggle || !statusText || !driveStatusInput) return;

        const initialValue = driveStatusInput.value === "true" || driveStatusInput.value === "false" 
            ? JSON.parse(driveStatusInput.value) 
            : false;
        statusText.textContent = initialValue ? "가능" : "불가능";
        driveToggle.checked = initialValue;
        driveStatusInput.value = initialValue;

        driveToggle.addEventListener("change", () => {
            const driveStatus = driveToggle.checked;
            statusText.textContent = driveStatus ? "가능" : "불가능";
            driveStatusInput.value = driveStatus;
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
        const submitBtn = document.getElementById("submitBtn");
        submitBtn.disabled = true;
        submitBtn.textContent = "처리 중...";

        if (!validateForm()) {
            submitBtn.disabled = false;
            submitBtn.textContent = "수정하기";
            return;
        }

        try {
            console.log("제출 전 selectedFiles:", selectedFiles);
            console.log("제출 전 deletedImageIds:", deletedImageIds);
            const s3Keys = await uploadImagesToS3();
            await submitFormData(s3Keys);
        } catch (error) {
            console.error("폼 제출 실패:", error);
            showError(`오류가 발생했습니다: ${error.message}`);
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "수정하기";
        }
    }

    function validateForm() {
        const validationRules = [
            { condition: document.getElementById("imagePreview").querySelectorAll('.image-wrapper').length === 0, message: '사진을 하나 이상 첨부해주세요!' },
            { condition: !getElementValue("postName").trim(), message: '제목을 입력해주세요!' },
            { condition: !getElementValue("fee") || getElementValue("fee") < 0, message: '유효한 요금을 입력해주세요!' },
            { condition: isCategory(2) && (!getElementValue("minRentalTime") || getElementValue("minRentalTime") < 30), message: '최소 이용 시간을 30분 이상으로 입력해주세요!' },
            { condition: !getElementValue("fullAddress").trim(), message: '주소를 입력해주세요!' }
        ];

        for (let rule of validationRules) {
            if (rule.condition) {
                showError(rule.message);
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
                showError(`파일 ${selectedFiles[index].name} 업로드 실패: ${result.reason}`);
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

        formData.delete("images");
        s3Keys.forEach(key => {
            formData.append("imageS3Keys", key);
        });
        deletedImageIds.forEach(id => {
            formData.append("deletedImageIds", id);
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
            throw new Error(data.message || "상품 수정 실패");
        }

        if (data.redirectUrl) {
            window.location.href = data.redirectUrl;
        } else {
            alert("상품 수정에 성공했습니다!");
        }
    }

    function generateUniqueFileName(originalFileName) {
        const dotIndex = originalFileName.lastIndexOf('.');
        const extension = dotIndex > -1 ? originalFileName.substring(dotIndex) : '';
        return `${crypto.randomUUID()}${extension}`;
    }

    // 초기화 함수 실행
    function initialize() {
        initializeMap();
        initializeAddressSearch();
        initializeDriveToggle();
        initializeDeleteButtons();
        initializeImageUpload();
        initializeFormSubmission();
    }

    // 모든 기능 초기화
    initialize();
});