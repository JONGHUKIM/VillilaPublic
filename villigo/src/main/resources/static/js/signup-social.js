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

