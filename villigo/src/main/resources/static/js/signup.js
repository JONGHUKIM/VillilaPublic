document.addEventListener("DOMContentLoaded", function () {
    let isUsernameChecked = false;
    let isPasswordChecked = false;
    let isNicknameChecked = false;
    let isEmailChecked = false;
    let isPhoneChecked = false;
    let isRegionAndInterestChecked = false;

    const usernameInput = document.querySelector('#username');
    const passwordInput = document.querySelector('#password');
    const passwordConfirmInput = document.querySelector('#passwordConfirm');
    const nicknameInput = document.querySelector('#nickname');
    const emailInput = document.querySelector('#email');
    const phoneInput = document.querySelector('#phone');
    const regionHiddenInput = document.querySelector('#region-hidden');
    const themeIdHiddenInput = document.querySelector('#theme-id-hidden');
	const marketingConsentHiddenInput = document.querySelector('#marketing-consent-hidden');
    const checkUsernameResult = document.querySelector('#checkUsernameResult');
    const checkPasswordResult = document.querySelector('#checkPasswordResult');
    const checkNicknameResult = document.querySelector('#checkNicknameResult');
    const checkEmailResult = document.querySelector('#checkEmailResult');
    const checkPhoneResult = document.querySelector('#checkPhoneResult');
    const checkRegionAndInterestResult = document.querySelector('#checkRegionAndInterestResult');
    const btnSubmit = document.querySelector('#btnSubmit');
	
	// 페이지 로드 시 marketingConsent 값 설정
	const marketingConsent = sessionStorage.getItem('marketingConsent');
	if (marketingConsent !== null) {
	    marketingConsentHiddenInput.value = marketingConsent === 'true'; // 'true' 문자열을 boolean으로 변환
	} else {
	    // agreement.js에서 값이 제대로 설정되지 않았을 경우 기본값 false
	    marketingConsentHiddenInput.value = 'false';
	}
	
	// 폼 제출 시 디버깅 (marketingConsent 값 확인용)
	document.querySelector('form').addEventListener('submit', function (e) {
	    // marketingConsent 값 확인
	    console.log('Form Data - Marketing Consent:', new FormData(this).get('marketingConsent'));
	});

	
	// 아이디 입력 시 영문 소문자와 숫자만 허용
	usernameInput.addEventListener('input', function (e) {
	    e.target.value = e.target.value.replace(/[^a-z0-9]/g, '');
	});

	// 비밀번호 및 비밀번호 확인 표시 토글
	document.querySelectorAll(".toggle-password").forEach(toggle => {
	    toggle.addEventListener("click", function () {
	        const targetInputId = this.getAttribute("data-target");
	        const targetInput = document.querySelector(`#${targetInputId}`);
	        if (targetInput.type === "password") {
	            targetInput.type = "text";
	        } else {
	            targetInput.type = "password";
	        }
	    });
	});
	
	// 전화번호 입력 시 하이픈 자동 추가 및 유효성 검사
	phoneInput.addEventListener('input', function (e) {
	    let value = e.target.value.replace(/\D/g, ''); // 숫자만 추출
	    let formatted = '';
	    if (value.length > 0) {
	        formatted = value.substring(0, 3); // 010
	        if (value.length >= 4) {
	            formatted += '-' + value.substring(3, Math.min(7, value.length)); // 1234
	        }
	        if (value.length >= 8) {
	            formatted += '-' + value.substring(7, 11); // 5678
	        }
	    }
	    e.target.value = formatted;

        // input 이벤트가 끝나고 값이 완전히 업데이트된 후 checkPhone을 호출하도록 setTimeout 사용
        // 또는 change 이벤트를 활용하여 최종 검사를 담당하게 할 수 있음.
        // 현재는 실시간 피드백을 위해 input에서 setTimeout을 사용.
	    setTimeout(checkPhone, 0); // 0ms 지연으로 현재 실행 스택 비운 후 checkPhone 실행
	});

    // 거래 희망 지역 선택
    const regionSelect = document.getElementById("region-select");
    const regionDropdown = document.getElementById("region-dropdown");
    const regionText = document.getElementById("region-text");

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
        // 다른 드롭다운이 열려있을 경우 닫기 (선택 사항)
        interestDropdown.style.display = "none";
    });

    // 관심 상품 선택
    const interestSelect = document.getElementById("interest-select");
    const interestDropdown = document.getElementById("interest-dropdown");
    const interestText = document.getElementById("interest-text");

    document.querySelectorAll("#interest-dropdown .option").forEach(option => {
        option.addEventListener("click", function () {
            interestText.textContent = this.dataset.value;
            interestDropdown.style.display = "none";
            themeIdHiddenInput.value = option.getAttribute('theme-id');
            checkRegionAndInterest();
        });
    });

    interestSelect.addEventListener("click", function () {
        interestDropdown.style.display = interestDropdown.style.display === "block" ? "none" : "block";
        // 다른 드롭다운이 열려있을 경우 닫기 (선택 사항)
        regionDropdown.style.display = "none";
    });

    // 드롭다운이 열려있을 때 다른 곳 클릭하면 닫히도록 설정
    document.addEventListener("click", function (e) {
        if (!regionSelect.contains(e.target) && !regionDropdown.contains(e.target)) {
            regionDropdown.style.display = "none";
        }
        if (!interestSelect.contains(e.target) && !interestDropdown.contains(e.target)) {
            interestDropdown.style.display = "none";
        }
    });

    /* --------------------------- 회원가입 검사 --------------------------- */
    btnSubmit.disabled = true;
    usernameInput.addEventListener('change', checkUsername);
    passwordInput.addEventListener('change', checkPassword);
    passwordConfirmInput.addEventListener('change', checkPassword);
    nicknameInput.addEventListener('change', checkNickname);
    emailInput.addEventListener('change', checkEmail);
    // phoneInput.addEventListener('change', checkPhone); // input 이벤트에서 이미 checkPhone 호출하므로 제거
                                                      // 또는 이곳에 final check 로직 추가
    
    function changeBtnStatus() {
        if (isUsernameChecked && isPasswordChecked && isNicknameChecked && 
            isEmailChecked && isPhoneChecked && isRegionAndInterestChecked) {
            btnSubmit.disabled = false;
        } else {
            btnSubmit.disabled = true;
            testCheckVariables();
        }
    }

	function checkUsername(event) {
	        const username = usernameInput.value.trim(); // trim() 추가
	        const usernameRegex = /^[a-z0-9]{3,}$/;
	        if (username === '') {
	            checkUsernameResult.innerHTML = '아이디는 필수 입력 항목입니다.';
	            isUsernameChecked = false;
	            changeBtnStatus();
	            return;
	        }
	        if (!usernameRegex.test(username)) {
	            checkUsernameResult.innerHTML = '아이디는 영문 소문자 또는 영문 소문자,숫자 조합으로 3글자 이상이어야 합니다.';
	            isUsernameChecked = false;
	            changeBtnStatus();
	            return;
	        }

	        const uri = `./checkusername?username=${username}`;
	        axios
	            .get(uri)
	            .then(handleCheckUsernameResult)
	            .catch((error) => console.log(error));
	    }

    function handleCheckUsernameResult({data}) {
        if (data === true) {
            checkUsernameResult.innerHTML = '이미 사용중인 아이디입니다.';
            isUsernameChecked = false;
        } else {
            checkUsernameResult.innerHTML = '';
            isUsernameChecked = true;
        }
        changeBtnStatus();
    }

    function checkPassword(event) {
        const password = passwordInput.value.trim(); // trim() 추가
        const passwordConfirm = passwordConfirmInput.value.trim(); // trim() 추가
        const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{4,}$/;
        if (password === '') {
            checkPasswordResult.innerHTML = '비밀번호는 필수 입력 항목입니다.';
            isPasswordChecked = false;
            changeBtnStatus();
            return;
        }
        if (!passwordRegex.test(password)) {
            checkPasswordResult.innerHTML = '비밀번호는 최소 4글자이며, 영문 대소문자, 숫자, 특수기호를 포함해야 합니다.';
            isPasswordChecked = false;
            changeBtnStatus();
            return;
            // 비밀번호 확인이 비어있지 않다면, 비밀번호 변경 후 비밀번호 확인도 다시 검사하도록 유도
            // if (passwordConfirmInput.value.trim() !== '') {
            //     checkPasswordConfirm();
            // }
        }
        // 비밀번호가 올바른 형식이어도 일치 여부는 별도로 판단
        if (password !== passwordConfirm) {
            checkPasswordResult.innerHTML = '비밀번호가 일치하지 않습니다.';
            isPasswordChecked = false;
            changeBtnStatus();
            return;
        }
        
        checkPasswordResult.innerHTML = '';
        isPasswordChecked = true;
        changeBtnStatus();
    }

    function checkNickname(event) {
        const nickname = nicknameInput.value.trim(); // trim() 추가
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
            .catch((error) => console.log(error));
    }

    function handleCheckNicknameResult({data}) {
        if (data === true) {
            checkNicknameResult.innerHTML = '이미 사용중인 닉네임입니다.';
            isNicknameChecked = false;
        } else {
            checkNicknameResult.innerHTML = '';
            isNicknameChecked = true;
        }
        changeBtnStatus();
    }

	function checkEmail(event) {
	        const email = emailInput.value.trim(); // trim() 추가
	        const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
	        if (email === '') {
	            checkEmailResult.innerHTML = '이메일은 필수 입력 항목입니다.';
	            isEmailChecked = false;
	            changeBtnStatus();
	            return;
	        }
	        if (!emailRegex.test(email)) {
	            checkEmailResult.innerHTML = '이메일 형식이 올바르지 않습니다. (예: user@domain.com)';
	            isEmailChecked = false;
	            changeBtnStatus();
	            return;
	        }

	        const uri = `./checkemail?email=${encodeURIComponent(email)}`;
	        axios
	            .get(uri)
	            .then(handleCheckEmailResult)
	            .catch((error) => console.log(error));
	    }

		function handleCheckEmailResult({ data }) {
		    const password = document.getElementById('password');
		    const passwordConfirm = document.getElementById('passwordConfirm');

		    if (data === 'SNS_USER') {
		        checkEmailResult.innerHTML = 'SNS로 가입된 계정입니다.';
		        password.disabled = true;
		        passwordConfirm.disabled = true;
		        isEmailChecked = false;
		    } else if (data === 'DUPLICATE') {
		        checkEmailResult.innerHTML = '이미 사용중인 이메일입니다.';
		        password.disabled = false;
		        passwordConfirm.disabled = false;
		        isEmailChecked = false;
		    } else {
		        checkEmailResult.innerHTML = '';
		        password.disabled = false;
		        passwordConfirm.disabled = false;
		        isEmailChecked = true;
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
        console.log('유저네임: ' + isUsernameChecked);
        console.log('비번: ' + isPasswordChecked);
        console.log('닉네임: ' + isNicknameChecked);
        console.log('이메일: ' + isEmailChecked);
        console.log('전화번호: ' + isPhoneChecked);
        console.log('지역/관심: ' + isRegionAndInterestChecked);
    }

    function checkRegionAndInterest() {
        if (regionHiddenInput.value !== '' && themeIdHiddenInput.value !== '') {
            isRegionAndInterestChecked = true;
            checkRegionAndInterestResult.innerHTML = '';
            changeBtnStatus();
        } else {
            isRegionAndInterestChecked = false;
            checkRegionAndInterestResult.innerHTML = '지역과 관심 상품을 선택해주세요.';
            changeBtnStatus();
        }
    }

    // 페이지 로드 시 초기 유효성 검사 (입력 필드에 값이 있을 경우)
    if (usernameInput.value.trim() !== '') checkUsername();
    if (passwordInput.value.trim() !== '') checkPassword();
    if (nicknameInput.value.trim() !== '') checkNickname();
    if (emailInput.value.trim() !== '') checkEmail();
    if (phoneInput.value.trim() !== '') checkPhone();
    checkRegionAndInterest(); // 지역/관심은 항상 초기 검사
});