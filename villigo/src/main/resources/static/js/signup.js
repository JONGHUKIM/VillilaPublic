document.addEventListener("DOMContentLoaded", function () {
    // 회원가입 검사용
    let isUsernameChecked = false;
    let isPasswordChecked = false;
    let isNicknameChecked = false;
    let isEmailChecked = false;
    let isRegionAndInterestChecked = false;

    const usernameInput = document.querySelector('#username');
    // passwordInput은 아래에서 선언한 변수를 사용.
    const nicknameInput = document.querySelector('#nickname');
    const emailInput = document.querySelector('#email');
    const regionHiddenInput = document.querySelector('#region-hidden');
    const themeIdHiddenInput = document.querySelector('#theme-id-hidden');    
    const checkUsernameResult = document.querySelector('#checkUsernameResult');
    const checkPasswordResult = document.querySelector('#checkPasswordResult');
    const checkNicknameResult = document.querySelector('#checkNicknameResult');
    const checkEmailResult = document.querySelector('#checkEmailResult');
    const checkRegionAndInterestResult = document.querySelector('#checkRegionAndInterestResult');
    const btnSubmit = document.querySelector('#btnSubmit');
    
    // 비밀번호 표시 기능
    const passwordInput = document.getElementById("password");
    const togglePassword = document.querySelector(".toggle-password");

    togglePassword.addEventListener("click", function () {
        if (passwordInput.type === "password") {
            passwordInput.type = "text";
        } else {
            passwordInput.type = "password";
        }
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
            // input태그에 값을 추가
            const regionHidden = document.getElementById('region-hidden');
            regionHidden.value = regionText.innerText;
            checkRegionAndInterest();
        });
        regionDropdown.appendChild(div);
    });

    regionSelect.addEventListener("click", function () {
        regionDropdown.style.display = regionDropdown.style.display === "block" ? "none" : "block";
    });

    // 관심 상품 선택
    const interestSelect = document.getElementById("interest-select");
    const interestDropdown = document.getElementById("interest-dropdown");
    const interestText = document.getElementById("interest-text");

    document.querySelectorAll("#interest-dropdown .option").forEach(option => {
        option.addEventListener("click", function () {
            interestText.textContent = this.dataset.value;
            interestDropdown.style.display = "none";
            // input태그에 값을 추가
            const themeIdHidden = document.getElementById('theme-id-hidden');
            themeIdHidden.value = option.getAttribute('theme-id');
            checkRegionAndInterest();
        });
    });

    interestSelect.addEventListener("click", function () {
        interestDropdown.style.display = interestDropdown.style.display === "block" ? "none" : "block";
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
    nicknameInput.addEventListener('change', checkNickname);
    emailInput.addEventListener('change', checkEmail);
    
    /* 콜백 */
    function changeBtnStatus() {
        if (isUsernameChecked && isPasswordChecked && isNicknameChecked && isEmailChecked
            && isRegionAndInterestChecked) {
            btnSubmit.disabled = false;
            return;
        }
        testCheckVariables();
        btnSubmit.disabled = true;
    }
    
    function checkUsername(event) {
        const username = usernameInput.value;
        if (username === '') {
            checkUsernameResult.innerHTML = '아이디는 필수 입력 항목입니다.';
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
            checkUsernameResult.innerHTML = '';
            checkUsernameResult.innerHTML = '이미 사용중인 아이디입니다.';
            isUsernameChecked = false;
        } else {
            checkUsernameResult.innerHTML = '';
            isUsernameChecked = true;
        }
        changeBtnStatus();
    }
    
    function checkPassword(event) {
        const password = passwordInput.value;
        if (password === '') {
            checkPasswordResult.innerHTML = '비밀번호는 필수 입력 항목입니다.';
            isPasswordChecked = false;
            changeBtnStatus();
            return;
        } else {
            checkPasswordResult.innerHTML = '';
            isPasswordChecked = true;
            changeBtnStatus();
        }
    }
    
    function checkNickname(event) {
        const nickname = nicknameInput.value;
        if (nickname === '') {
            checkNicknameResult.innerHTML = '닉네임은 필수 입력 항목입니다.';
            isNicknameChecked = false;
            changeBtnStatus();
            return;
        }

        const uri = `./checknickname?nickname=${nickname}`;
        axios
        .get(uri)
        .then(handleCheckNicknameResult)
        .catch((error) => console.log(error));
    }
    
    function handleCheckNicknameResult({data}) {
        if (data === true) {
            checkNicknameResult.innerHTML = '';
            checkNicknameResult.innerHTML = '이미 사용중인 닉네임입니다.';
            isNicknameChecked = false;
        } else {
            checkNicknameResult.innerHTML = '';
            isNicknameChecked = true;
        }
        changeBtnStatus();
    }
    
    function checkEmail(event) {
        const email = emailInput.value;
        if (email === '') {
            checkEmailResult.innerHTML = '이메일은 필수 입력 항목입니다.';
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

    function handleCheckEmailResult({data}) {
        console.log('이메일 체크 서버 응답: ', data);
        if (data === true) {
            checkEmailResult.innerHTML = '';
            checkEmailResult.innerHTML = '이미 사용중인 이메일입니다.';
            console.log('이메일 검사 탈락');
            isEmailChecked = false;
        } else {
            checkEmailResult.innerHTML = '';
            isEmailChecked = true;
            console.log('이메일 검사 통과');
        }
        changeBtnStatus();
        console.log('isEmailChecked 값:', isEmailChecked); // 비동기 호출 딜레이 확인용
    }
    
    function testCheckVariables() {
        console.log('유저네임: ' + isUsernameChecked);
        console.log('비번: ' + isPasswordChecked);
        console.log('닉네임: ' + isNicknameChecked);
        console.log('이메일: ' + isEmailChecked);
    }
    
    function checkRegionAndInterest() {
        console.log('region.value: ' + regionHiddenInput.value + ', theme.value: ' + themeIdHiddenInput.value);
        if (regionHiddenInput.value !== '' && themeIdHiddenInput.value !== '') {
            isRegionAndInterestChecked = true;
            checkRegionAndInterestResult.innerHTML = '';
            changeBtnStatus();
        } else {
            isRegionAndInterestChecked = false;
            changeBtnStatus();
            checkRegionAndInterestResult.innerHTML = '지역과 관심 상품을 선택해주세요.';
        }
    }
});
