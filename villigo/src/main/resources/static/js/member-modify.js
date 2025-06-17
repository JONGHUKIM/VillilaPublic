// 이미지 미리보기 함수
window.previewAvatar = function (input) {
  const previewWrapper = document.querySelector(".profile-image-wrapper");
  const oldPreview = document.getElementById("avatarPreview");

  if (!input.files || !input.files[0] || !previewWrapper) return;

  const reader = new FileReader();
  reader.onload = function (e) {
    const newImg = document.createElement("img");
    newImg.id = "avatarPreview";
    newImg.src = e.target.result;
    newImg.alt = "프로필 이미지";

    if (oldPreview) {
      previewWrapper.replaceChild(newImg, oldPreview);
    } else {
      previewWrapper.appendChild(newImg);
    }
  };
  reader.readAsDataURL(input.files[0]);
};

<<<<<<< HEAD
<<<<<<< HEAD

=======
// 탈퇴 확인 함수
function confirmWithdraw() {
  if (confirm("탈퇴하시겠습니까? 게시물, 리뷰, 채팅은 유지되며, 개인 정보는 삭제됩니다.")) {
	axios.post("/member/withdraw")
	  .then(response => {
	    alert("회원 탈퇴가 완료되었습니다.");
	    window.location.href = "/"; // JS에서 직접 이동
	  })
	  .catch(error => {
	    console.error("회원 탈퇴 실패:", error.response || error);
	    alert("회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.");
	  });
  }
}
>>>>>>> 49abed9 (서버에서 JSON응답처리, JS에서 리다이렉트 처리)
=======
// 탈퇴 확인 함수
function confirmWithdraw() {
  if (confirm("탈퇴하시겠습니까? 게시물, 리뷰, 채팅은 유지되며, 개인 정보는 삭제됩니다.")) {
    axios.post("/member/withdraw")
      .then(response => {
        alert("회원 탈퇴가 완료되었습니다.");
        window.location.href = "/";
      })
      .catch(error => {
        console.error("회원 탈퇴 실패:", error.response || error);
        alert("회원 탈퇴 중 오류가 발생했습니다. 다시 시도해주세요.");
      });
  }
}
>>>>>>> e02cb2f (JS에 탈퇴 함수 추가)

document.addEventListener("DOMContentLoaded", function () {
  // DOM 요소 선택
  const regionSelect = document.getElementById("region-select");
  const regionDropdown = document.getElementById("region-dropdown");
  const regionText = document.getElementById("region-text");
  const regionHiddenInput = document.getElementById("region-hidden");

  const interestSelect = document.getElementById("interest-select");
  const interestDropdown = document.getElementById("interest-dropdown");
  const interestText = document.getElementById("interest-text");
  const themeIdHiddenInput = document.getElementById("theme-id-hidden");

  const passwordInput = document.getElementById("password");
  const togglePassword = document.querySelector(".toggle-password");

  const nicknameInput = document.getElementById("nickname");
  const nicknameLabel = nicknameInput ? nicknameInput.parentElement : null;
  const form = document.querySelector("form");

  // 지역 데이터
  const regions = [
    "서울", "부산", "대구", "인천", "광주", "대전", "울산", "세종",
    "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남", "제주"
  ];

  // 비밀번호 표시/숨김 토글
  if (togglePassword && passwordInput) {
    togglePassword.addEventListener("click", function () {
      const isPassword = passwordInput.type === "password";
      passwordInput.type = isPassword ? "text" : "password";
      togglePassword.textContent = isPassword ? "🙈" : "👁️";
    });
  }

  // 닉네임 중복 체크
  if (nicknameInput && nicknameLabel) {
    const originalNickname = nicknameInput.value;
    
    nicknameInput.addEventListener("input", async () => {
      const nickname = nicknameInput.value.trim();
      const errorMessage = nicknameLabel.querySelector(".error-message");
      
      // 기존 에러 메시지 제거
      if (errorMessage) errorMessage.remove();
      
      // 빈 값이거나 원래 닉네임과 같으면 검사 스킵
      if (nickname === "" || nickname === originalNickname) return;

      try {
        const response = await axios.get("/api/user/check-nickname", { 
          params: { nickname } 
        });
        
        if (!response.data) {
          const errorSpan = document.createElement("span");
          errorSpan.className = "error-message";
          errorSpan.style.color = "red";
          errorSpan.style.marginLeft = "10px";
          errorSpan.textContent = "중복된 닉네임입니다";
          nicknameLabel.appendChild(errorSpan);
          nicknameInput.setCustomValidity("중복된 닉네임입니다");
        } else {
          nicknameInput.setCustomValidity("");
        }
      } catch (error) {
        console.error("닉네임 중복 체크 실패:", error);
      }
    });
  }

  // 지역 드롭다운 초기화
  regions.forEach(region => {
    const div = document.createElement("div");
    div.classList.add("dropdown-item");
    div.textContent = region;
    div.dataset.value = region;
    div.addEventListener("click", function () {
      regionText.textContent = region;
      regionHiddenInput.value = region;
      regionDropdown.style.display = "none";
    });
    regionDropdown.appendChild(div);
  });

  // 관심상품 드롭다운 초기화
  const initializeInterestDropdown = (interests) => {
    interestDropdown.innerHTML = ""; // 기존 내용 초기화
    interests.forEach(item => {
      const div = document.createElement("div");
      div.classList.add("dropdown-item");
      div.textContent = item.theme;
      div.dataset.value = item.theme;
      div.setAttribute("theme-id", item.id);
      div.addEventListener("click", function () {
        interestText.textContent = item.theme;
        themeIdHiddenInput.value = item.id;
        interestDropdown.style.display = "none";
      });
      interestDropdown.appendChild(div);
    });
  };

  // 테마 데이터 가져오기
  axios.get("/api/themes")
    .then(response => {
      const interests = response.data || [];
      console.log("Fetched themes:", interests);
      initializeInterestDropdown(interests);
    })
    .catch(error => {
      console.error("테마 목록 가져오기 실패:", error.response || error);
      // 기본 데이터로 폴백
      const fallbackInterests = [
        { id: 1, theme: "자동차" },
        { id: 2, theme: "가방/백" }
      ];
      initializeInterestDropdown(fallbackInterests);
    });

  // 드롭다운 토글 이벤트
  regionSelect.addEventListener("click", function (e) {
    e.stopPropagation();
    interestDropdown.style.display = "none";
    regionDropdown.style.display = regionDropdown.style.display === "block" ? "none" : "block";
  });

  interestSelect.addEventListener("click", function (e) {
    e.stopPropagation();
    regionDropdown.style.display = "none";
    interestDropdown.style.display = interestDropdown.style.display === "block" ? "none" : "block";
  });

  // 외부 클릭 시 드롭다운 닫기
  document.addEventListener("click", function (e) {
    if (!regionSelect.contains(e.target) && !regionDropdown.contains(e.target)) {
      regionDropdown.style.display = "none";
    }
    if (!interestSelect.contains(e.target) && !interestDropdown.contains(e.target)) {
      interestDropdown.style.display = "none";
    }
  });

  // 폼 제출 시 유효성 검사
  if (form && nicknameLabel) {
    form.addEventListener("submit", (event) => {
      const nicknameError = nicknameLabel.querySelector(".error-message");
      if (nicknameError) {
        event.preventDefault();
        alert("닉네임 중복을 확인해주세요.");
      }
    });
  }
});