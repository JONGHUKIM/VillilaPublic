document.addEventListener("DOMContentLoaded", function () {
    const chatbotWindow = document.getElementById("chatbotWindow");
    const chatbotMessages = document.getElementById("chatbotMessages");
    const chatbotInput = document.getElementById("chatbotInput");
    const faqContainer = document.getElementById("faqButtons");

    // 챗봇 창 초기화 (처음에는 숨김)
    chatbotWindow.style.display = "none";

    // 챗봇 토글 기능
    window.toggleChatbot = function () {
        chatbotWindow.style.display = chatbotWindow.style.display === "flex" ? "none" : "flex";
        if (chatbotWindow.style.display === "flex") {
            loadFAQ();
        }
    };

    // FAQ 질문 불러오기
    function loadFAQ() {
        fetch("/api/faqs") // 백엔드에서 제공하는 FAQ API 호출
            .then(response => response.json())
            .then(data => {
                if (!Array.isArray(data)) throw new Error("FAQ 데이터가 배열이 아닙니다.");
                
                faqContainer.innerHTML = ""; // 기존 버튼 초기화
                data.forEach(faq => {
                    const button = document.createElement("button");
                    button.innerText = faq.question;
                    button.onclick = () => sendChatbotMessage(faq.question);
                    button.className = "faq-button"; 
                    faqContainer.appendChild(button);
                });
            })
            .catch(error => {
                console.error("FAQ 로드 오류:", error);
                faqContainer.innerHTML = "<p>FAQ 데이터를 불러올 수 없습니다.</p>";
            });
    }

    // 메시지 추가 함수 (말풍선 형태)
    function appendMessage(sender, text) {
        const messageElement = document.createElement("div");
        messageElement.className = sender === "User" ? "bubble user-message alert" : "bubble bot-message alert ";
        messageElement.innerHTML = text.replace(/\n/g, "<br>"); // 줄바꿈 처리
        chatbotMessages.appendChild(messageElement);
        chatbotMessages.scrollTop = chatbotMessages.scrollHeight;
    }

    // 메시지 전송 함수
	window.sendChatbotMessage = function (message = null) {
	    const messageText = message || chatbotInput.value.trim();
	    if (!messageText) return;

	    appendMessage("User", messageText);
	    chatbotInput.value = "";

	    //  FAQ 버튼 잠시 숨김
	    faqContainer.style.display = "none";

	    fetch("/bot/chat", {
	        method: "POST",
	        headers: { "Content-Type": "application/json" },
	        body: JSON.stringify({ message: messageText })
	    })
	        .then(response => response.json())
	        .then(data => {
	            appendMessage("Bot", data.response);

	            //  1.5초 후 FAQ 다시 표시
	            setTimeout(() => {
	                faqContainer.style.display = "flex";
	            }, 1500);
	        })
	        .catch(error => console.error("챗봇 오류:", error));
	};

    // 엔터 키 입력 시 메시지 전송
    chatbotInput.addEventListener("keypress", function (event) {
        if (event.key === "Enter") {
            sendChatbotMessage();
        }
    });
});
document.addEventListener("DOMContentLoaded", function () {
  const plusBtn = document.getElementById("plusToggleButton");
  const popup = document.getElementById("plusPopup");
  const plusIcon = document.getElementById("plusIcon");
  const closeIcon = document.getElementById("closeIcon");

  plusBtn.addEventListener("click", function () {
    const isVisible = popup.style.display === "flex";
    popup.style.display = isVisible ? "none" : "flex";
    plusIcon.style.display = isVisible ? "inline" : "none";
    closeIcon.style.display = isVisible ? "none" : "inline";
  });

  document.addEventListener("click", function (e) {
    if (!plusBtn.contains(e.target) && !popup.contains(e.target)) {
      popup.style.display = "none";
      plusIcon.style.display = "inline";
      closeIcon.style.display = "none";
    }
  });
});
