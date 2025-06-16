/**
 * /jjam/payment.html 관련 JavaScript
 */
function showToast(message) {
    const toast = document.getElementById("toast");
    toast.innerText = message;
    toast.style.visibility = "visible";
    setTimeout(() => {
        toast.style.visibility = "hidden";
    }, 3000);
}
document.addEventListener("DOMContentLoaded", function () {
    // ✅ URL에서 amount, userId 가져오기
    const urlParams = new URLSearchParams(window.location.search);
    const amount = urlParams.get("amount") || "1000"; // 기본값 1000
    const userId = urlParams.get("userId");

    // ✅ HTML에 젤리 개수 표시
    document.getElementById("amountText").innerText = amount;

    // ✅ 확인 버튼 클릭 이벤트 추가
    document.getElementById("confirm-btn").addEventListener("click", function () {
        fetch("/jjam/api/jjams/purchase", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
			credentials: "include",  // ✅ 세션 쿠키 유지
            body: JSON.stringify({ userId: userId, quantity: amount })
        })
        .then(response => response.json())
        .then(data => {
			if (data) {
			           showToast("충전 완료!");
			           setTimeout(() => {
			               window.close(); // 2초 후 팝업 닫기
			           }, 3000);
			       } else {
			           throw new Error("충전 실패");
			       }
        })
        .catch(error => {
            console.error("Error:", error);
            alert("충전 요청 실패");
        });
    });
});
