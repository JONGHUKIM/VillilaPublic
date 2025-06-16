document.addEventListener("DOMContentLoaded", function () {
  const checkboxes = document.querySelectorAll(".feedback-options input[type='checkbox']");
  const submitBtn = document.getElementById("submit-review");

  submitBtn.addEventListener("click", () => {
    const selectedOptions = Array.from(checkboxes)
      .filter(cb => cb.checked)
      .map(cb => cb.value);

    if (selectedOptions.length === 0) {
      alert("최소 한 개 이상의 항목을 선택해주세요.");
      return;
    }

    const keywordId = selectedOptions[0]; // ✅ 첫 번째 선택값만 사용

    // 부모창에서 targetId와 reservationId 가져오기
    const params = new URLSearchParams(window.location.search);
    const targetId = params.get('targetId')
    console.log('targetId: ', targetId);
    const reservationId = params.get('reservationId');
    console.log('reservationId: ', reservationId);
    const isOwner = params.get('owner');
    console.log('isOwner: ', isOwner);

    if (!targetId || !reservationId) {
      alert("리뷰 대상 정보가 없습니다.");
      return;
    }

    // 후기 내용은 체크박스라 간단 텍스트 고정 처리 (옵션)
    const keywordText = checkboxes[parseInt(keywordId) - 1]?.nextElementSibling?.innerText || "기타";

    const reviewData = {
      targetId: targetId,
      reservationId: reservationId,
      keywordId: keywordId,
      content: keywordText, // ✅ 체크박스 설명 문구를 그대로 content로 보냄
      isOwner: isOwner // 상품 주인이 작성한 후기인지 여부를 저장
    };

    axios.post('/api/reviews', reviewData)
      .then(() => {
        alert("후기 전송이 완료되었습니다!");
        try {
          if (window.opener && typeof window.opener.completeReview === 'function') {
            window.opener.completeReview(); // 오류 방지용 try-catch
          }
        } catch (e) {
          console.warn("부모창 처리 중 오류 발생:", e);
        }
        window.close();
      })
      .catch(err => {
        console.error("후기 전송 실패:", err);
        alert("후기 전송에 실패했습니다.");
      });
  });
});
