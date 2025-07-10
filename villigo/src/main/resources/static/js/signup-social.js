document.addEventListener('DOMContentLoaded', function () {
    // 상태 변수
    let isNicknameChecked = false;
    let isPhoneChecked = false;
    let isRegionAndInterestChecked = false;

    // DOM 요소
    const nicknameInput = document.getElementById('nickname');
    const phoneInput = document.getElementById('phone');
    const regionSelect = document.getElementById('region-select');
    const regionDropdown = document.getElementById('region-dropdown');
    const regionText = document.getElementById('region-text');
    const interestSelect = document.getElementById('interest-select');
    const interestDropdown = document.getElementById('interest-dropdown');
    const interestText = document.getElementById('interest-text');
    const regionHiddenInput = document.getElementById('region-hidden');
    const themeIdHiddenInput = document.getElementById('theme-id-hidden');
    const marketingConsentHiddenInput = document.getElementById('marketing-consent-hidden');
    const checkNicknameResult = document.getElementById('checkNicknameResult');
    const checkPhoneResult = document.getElementById('checkPhoneResult');
    const btnSubmit = document.getElementById('btnSubmit');

    // 요소 누락 체크
    if (!nicknameInput || !phoneInput || !regionSelect || !regionDropdown || !regionText ||
        !interestSelect || !interestDropdown || !interestText || !regionHiddenInput ||
        !themeIdHiddenInput || !marketingConsentHiddenInput || !checkNicknameResult ||
        !checkPhoneResult || !btnSubmit) {
        console.error("폼 요소가 DOM에 없습니다.");
        return;
    }

    // 마케팅 동의 값 설정
    const marketingConsent = sessionStorage.getItem('marketingConsent');
    marketingConsentHiddenInput.value = marketingConsent === 'true';
    console.log("마케팅 동의 여부:", marketingConsent);

    // 전화번호 포맷팅
    phoneInput.addEventListener('input', function (e) {
        let value = e.target.value.replace(/\D/g, '');
        let formatted = '';
        if (value.length > 0) {
            formatted = value.substring(0, 3);
            if (value.length >= 4) {
                formatted += '-' + value.substring(3, Math.min(7, value.length));
            }
            if (value.length >= 8) {
                formatted += '-' + value.substring(7, 11);
            }
        }
        e.target.value = formatted;
        checkPhone(); // 즉시 유효성 검사
    });

    // 지역 선택
    const regions = [
        "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
        "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
    ];
    regions.forEach(region => {
        const div = document.createElement("div");
        div.classList.add("option");
        div.textContent = region;
        div.dataset.value = region;
        div.addEventListener("click", function () {
            regionText.textContent = this.dataset.value;
            regionDropdown.style.display = "none";
            regionHiddenInput.value = regionText.innerText;
            checkRegionAndInterest();
        });
        regionDropdown.appendChild(div);
    });

    regionSelect.addEventListener("click", function () {
        regionDropdown.style.display = regionDropdown.style.display === "block" ? "none" : "block";
        interestDropdown.style.display = "none"; // 관심 상품 드롭다운 닫기
    });

    // 관심 상품 선택
    document.querySelectorAll("#interest-dropdown .option").forEach(option => {
        option.addEventListener("click", function () {
            interestText.textContent = this.dataset.value;
            interestDropdown.style.display = "none";
            themeIdHiddenInput.value = this.getAttribute('theme-id');
            checkRegionAndInterest();
        });
    });

    interestSelect.addEventListener("click", function () {
        interestDropdown.style.display = interestDropdown.style.display === "block" ? "none" : "block";
        regionDropdown.style.display = "none"; // 지역 드롭다운 닫기
    });

    // 드롭다운 외부 클릭 시 닫기
    document.addEventListener("click", function (e) {
        if (!regionSelect.contains(e.target) && !regionDropdown.contains(e.target)) {
            regionDropdown.style.display = "none";
        }
        if (!interestSelect.contains(e.target) && !interestDropdown.contains(e.target)) {
            interestDropdown.style.display = "none";
        }
    });

    // 유효성 검사 및 버튼 상태 변경
    btnSubmit.disabled = true;
    nicknameInput.addEventListener('change', checkNickname);
    phoneInput.addEventListener('change', checkPhone);

    function changeBtnStatus() {
        if (isNicknameChecked && isPhoneChecked && isRegionAndInterestChecked) {
            btnSubmit.disabled = false;
        } else {
            btnSubmit.disabled = true;
            testCheckVariables();
        }
    }

    function checkNickname() {
        const nickname = nicknameInput.value;
        if (nickname === '') {
            checkNicknameResult.innerHTML = '닉네임은 필수 입력 항목입니다.';
            isNicknameChecked = false;
            changeBtnStatus();
            return;
        }

        const uri = `./checknickname?nickname=${encodeURIComponent(nickname)}`;
        axios
            .get(uri)
            .then(handleCheckNicknameResult)
            .catch((error) => console.error("닉네임 중복 확인 중 오류 발생:", error));
    }

    function handleCheckNicknameResult({ data }) {
        if (data === true) {
            checkNicknameResult.innerHTML = '이미 사용중인 닉네임입니다.';
            isNicknameChecked = false;
        } else {
            checkNicknameResult.innerHTML = '';
            isNicknameChecked = true;
        }
        changeBtnStatus();
    }

	function checkPhone() { // event 매개변수 제거 (input 이벤트에서 바로 호출하므로 제거)
	        const phone = phoneInput.value.trim(); // <-- trim() 추가
	        const phoneRegex = /^010-\d{4}-\d{4}$/;

	        // 디버깅을 위한 로그 추가
	        console.log("Debug checkPhone - Raw:", phoneInput.value, "Trimmed:", phone, "Length:", phone.length, "Regex test:", phoneRegex.test(phone));

	        if (phone === '') {
	            checkPhoneResult.innerHTML = '전화번호는 필수 입력 항목입니다.';
	            isPhoneChecked = false;
	            changeBtnStatus();
	            return;
	        }
	        if (!phoneRegex.test(phone)) {
	            checkPhoneResult.innerHTML = '전화번호는 010-1234-5678 형식으로 입력해야 합니다.';
	            isPhoneChecked = false;
	            changeBtnStatus();
	            return;
	        }

	        const uri = `./checkphone?phone=${phone}`;
	        axios
	            .get(uri)
	            .then(({data}) => {
	                if (data === true) {
	                    checkPhoneResult.innerHTML = '이미 사용중인 전화번호입니다.';
	                    isPhoneChecked = false;
	                } else {
	                    checkPhoneResult.innerHTML = '';
	                    isPhoneChecked = true;
	                }
	                changeBtnStatus();
	            })
	            .catch((error) => console.log(error));
	    }

    function testCheckVariables() {
        console.log('닉네임: ' + isNicknameChecked);
        console.log('전화번호: ' + isPhoneChecked);
        console.log('지역/관심: ' + isRegionAndInterestChecked);
    }

    function checkRegionAndInterest() {
        if (regionHiddenInput.value !== '' && themeIdHiddenInput.value !== '') {
            isRegionAndInterestChecked = true;
        } else {
            isRegionAndInterestChecked = false;
        }
        changeBtnStatus();
    }

    // 초기 유효성 검사
    checkRegionAndInterest();
});