function toggleCustomBrand() {
       const brandSelect = document.getElementById("brand");
       const customBrandInput = document.getElementById("customBrand");
       customBrandInput.style.display = brandSelect.value === "0" ? "block" : "none";
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
	 //  차 연식
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

	    // 버튼을 누르면 숫자 변경 (한 번)
	    // 버튼 누를 때 반복 실행 시작
	    // 버튼에서 손을 떼거나 벗어나면 반복 중지
		if(btnDecrement) {
			btnDecrement.addEventListener('click', () => changeYear(-1));
			btnDecrement.addEventListener('mousedown', () => startHold(-1));
			btnDecrement.addEventListener('mouseup', stopHold);
			btnDecrement.addEventListener('mouseleave', stopHold);
		}
		if(btnIncrement) {
			btnIncrement.addEventListener('click', () => changeYear(1));
			btnIncrement.addEventListener('mousedown', () => startHold(1));
			btnIncrement.addEventListener('mouseup', stopHold);
			btnIncrement.addEventListener('mouseleave', stopHold);
		}

	    // 버튼을 꾹 누르면 숫자가 계속 변경됨
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


	   // 토글 상태 변경 시 텍스트 업데이트 + input value 값 변환 -> bag에는 없어서 if문 안에 넣었습니다.
	   if(driveToggle) {
			driveToggle.addEventListener("change", () => {
				statusText.textContent = driveToggle.checked ? "가능" : "불가능";
				driveStatusInput.value = driveToggle.checked ? "true" : "false";
		  	});
	   }

       function previewImages() {
           const input = document.getElementById('uploadImage');
           const previewContainer = document.getElementById('imagePreview');
    
           const maxFiles = 100;
           const maxSizeMB = 10;
           
           const files = Array.from(input.files);
           
           if(files.length > 10) {
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
                       img.style.width = "120px"
                       img.style.height = "120px"
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

	// 주소 검색 기능
	document.getElementById("addressBtn").addEventListener('click', function() {
		new daum.Postcode({
			oncomplete: function(data) {
				document.getElementById("zonecode").value=data.zonecode;
				document.getElementById("sido").value=data.sido;
				document.getElementById("fullAddress").value=data.address;
				document.getElementById("sigungu").value=data.sigungu;
				document.getElementById("bname").value=data.bname;
			}
		}).open();
	});

	// 전체 주소 넣어주기
	document.getElementById("fullAddress").addEventListener('change', function() {
		const address = this.value;
		if(address) {

		}
	});

	// 내용 미입력 알림창
	const submitBtn = document.getElementById("submitBtn");
	submitBtn.addEventListener('click', function(event) {
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
		if(imagePreviewDiv.innerHTML === "") {
			alert('사진을 하나 이상 첨부해주세요!');
			return;
		} else if(postNameInput.value === "") {
			alert('제목을 입력해주세요!');
			return;
		} else if(categoryNumInput.value == 1 && productName.value === "") {
			alert('상품명을 입력해주세요!');
			return;
		} else if(categoryNumInput.value == 2 && productName.value === "") {
			alert('차종을 입력해주세요!');
			return;
		} else if (brandSelect.value === "") {
			alert('브랜드를 선택해주세요!');
			return;
		} else if (brandSelect.value === "0" && customBrandInput.value.trim() === "") {
			alert('브랜드명을 입력해주세요!');
			return;
		} else if(selectedColorInput.value === "") {
			alert('색상을 선택해주세요!');
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
  
