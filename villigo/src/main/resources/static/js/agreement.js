document.addEventListener('DOMContentLoaded', function() {
    const termsAgree = document.getElementById('termsAgree'); 
    const privacyAgree = document.getElementById('privacyAgree'); 
    const marketingAgree = document.getElementById('marketingAgree'); 
    const btnProceedSignUp = document.getElementById('btnProceedSignUp');

    function checkAgreements() {
        const allRequiredAgreed = termsAgree.checked && privacyAgree.checked; 
        btnProceedSignUp.disabled = !allRequiredAgreed;
    }

    termsAgree.addEventListener('change', checkAgreements); //
    privacyAgree.addEventListener('change', checkAgreements); //
    marketingAgree.addEventListener('change', checkAgreements); //

    // "가입하기" 버튼 클릭 시
    btnProceedSignUp.addEventListener('click', function() {
        sessionStorage.setItem('marketingConsent', marketingAgree.checked); // 마케팅 동의 여부 저장
        window.location.href = '/member/signup'; // 회원가입 페이지로 이동
    });

    checkAgreements(); // 페이지 로드 시 초기 상태 확인
});