document.addEventListener("DOMContentLoaded", function () {
    const passwordInput = document.getElementById("password");
    const togglePassword = document.querySelector(".toggle-password");
    
    const divUsername = document.getElementById('divUsername');
    const divPassword = document.getElementById('divPassword');
    const divnotifyMessage = document.getElementById('notifyMessage');

    togglePassword.addEventListener("click", function () {
        if (passwordInput.type === "password") {
            passwordInput.type = "text";
        } else {
            passwordInput.type = "password";
        }
    });
    
    showLoginErrorNotify();
    
    /* ------------------------- 아이디, 비밀번호 검사 --------------------------- */
    function showLoginErrorNotify() {
        const queryString = window.location.search;
        if (queryString.includes('error')) {
            divUsername.classList.add('dangerBorder');
            divPassword.classList.add('dangerBorder');
            divnotifyMessage.innerHTML = '아이디와 비밀번호를 확인하세요';
        }
    }
});
