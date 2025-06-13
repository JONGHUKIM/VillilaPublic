document.addEventListener('DOMContentLoaded', function() {
    const termsAgree = document.getElementById('termsAgree');
    const privacyAgree = document.getElementById('privacyAgree');
    const marketingAgree = document.getElementById('marketingAgree'); // 마케팅 동의 체크박스
    const btnProceedSignUp = document.getElementById('btnProceedSignUp');
    const btnProceedGoogleSignUp = document.getElementById('btnProceedGoogleSignUp');

    function checkAgreements() {
        // 필수 동의 (서비스 이용약관 및 개인정보 수집 동의)만 확인하여 버튼 활성화
        // 마케팅 동의는 선택이므로 버튼 활성화 여부에 영향을 주지 않음
        const allRequiredAgreed = termsAgree.checked && privacyAgree.checked;
        btnProceedSignUp.disabled = !allRequiredAgreed;
        btnProceedGoogleSignUp.disabled = !allRequiredAgreed;
    }

    // 체크박스 상태 변경 시마다 버튼 활성화 여부 확인
    termsAgree.addEventListener('change', checkAgreements);
    privacyAgree.addEventListener('change', checkAgreements);
    marketingAgree.addEventListener('change', checkAgreements); // 마케팅 동의는 동의 내역 전달을 위해 change 이벤트를 계속 감지

    // URL 파라미터를 통해 회원가입 타입 (일반/구글) 확인
    const urlParams = new URLSearchParams(window.location.search);
    const signupType = urlParams.get('type');

    if (signupType === 'google') {
        btnProceedSignUp.style.display = 'none'; // 일반 회원가입 버튼 숨김
        btnProceedGoogleSignUp.style.display = 'block'; // 구글 회원가입 버튼 표시
        btnProceedGoogleSignUp.addEventListener('click', function() {
            // 구글 회원가입 진행 전, 마케팅 동의 여부를 sessionStorage에 저장
            sessionStorage.setItem('marketingConsent', marketingAgree.checked);
            // 구글 OAuth2 인증 흐름 시작 (Spring Security 설정에 따라 자동으로 리다이렉트)
            window.location.href = '/oauth2/authorization/google';
        });
    } else {
        btnProceedSignUp.style.display = 'block'; // 일반 회원가입 버튼 표시
        btnProceedGoogleSignUp.style.display = 'none'; // 구글 회원가입 버튼 숨김
        btnProceedSignUp.addEventListener('click', function() {
             // 일반 회원가입 진행 전, 마케팅 동의 여부를 sessionStorage에 저장
            sessionStorage.setItem('marketingConsent', marketingAgree.checked);
            // 일반 회원가입 페이지로 이동
            window.location.href = '/member/signup';
        });
    }

    checkAgreements(); // 페이지 로드 시 초기 상태 확인
});