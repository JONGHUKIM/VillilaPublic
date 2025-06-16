document.addEventListener('DOMContentLoaded', function() {
    const termsAgree = document.getElementById('termsAgree'); 
    const privacyAgree = document.getElementById('privacyAgree'); 
    const marketingAgree = document.getElementById('marketingAgree'); 
    const btnProceedSignUp = document.getElementById('btnProceedSignUp');

    function checkAgreements() {
        const allRequiredAgreed = termsAgree.checked && privacyAgree.checked; 
        btnProceedSignUp.disabled = !allRequiredAgreed;
    }
	
	// 다음 아코디언 자동 열기 함수
	function openAccordion(targetId) {
	    const collapseElement = document.getElementById(targetId);
	    if (!collapseElement.classList.contains('show')) {
	        const bsCollapse = new bootstrap.Collapse(collapseElement, {
	            toggle: false
	        });
	        bsCollapse.show();
	    }
	}

	// 체크박스 변화에 따른 아코디언 오픈 처리
	termsAgree.addEventListener('change', function () {
	    checkAgreements();
	    if (termsAgree.checked) {
	        openAccordion('collapseTwo'); // 개인정보 항목 열기
	    }
	});
	
	privacyAgree.addEventListener('change', function () {
	    checkAgreements();
	    if (privacyAgree.checked) {
	        openAccordion('collapseThree'); // 마케팅 항목 열기
	    }
	});
	
    marketingAgree.addEventListener('change', checkAgreements);

    // "가입하기" 버튼 클릭 시
    btnProceedSignUp.addEventListener('click', function() {
        sessionStorage.setItem('marketingConsent', marketingAgree.checked); // 마케팅 동의 여부 저장
        window.location.href = '/member/signup'; // 회원가입 페이지로 이동
    });

    checkAgreements(); // 페이지 로드 시 초기 상태 확인
});