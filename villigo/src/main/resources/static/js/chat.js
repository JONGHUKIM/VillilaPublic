document.addEventListener("DOMContentLoaded", function () {
    console.log("페이지 로드 완료");

    // DOM 요소 가져오기
    let stompClient = null;
    const messageInput = document.getElementById("messageInput");
    const sendButton = document.getElementById("sendMessage");
    const chatMessages = document.querySelector(".chat-messages");
    const attachButton = document.querySelector(".attach-btn");
    const chatList = document.querySelector(".chat-list");
    const deleteButton = document.querySelector(".delete-btn");
    const typingIndicator = document.getElementById("typingIndicator");
    const noChatMessage = document.getElementById("noChatMessage");
    const chatMain = document.querySelector(".chat-main");
    const fileInput = document.getElementById("fileInput");
    const unreadFilter = document.getElementById("unreadFilter");
    const searchChat = document.getElementById("searchChat");
    
    // URL 파라미터에서 chatRoomId 가져오기
    const urlParams = new URLSearchParams(window.location.search);
    const urlChatRoomId = parseInt(urlParams.get('chatRoomId')) || null;

    // 초기 데이터 설정
    let chatRoomId = parseInt(chatMain.getAttribute("data-chatroom-id")) || urlChatRoomId;
    let senderId = parseInt(chatMain.getAttribute("data-user-id")) || parseInt(document.body.getAttribute("data-user-id"));
    if (!senderId) {
        console.error("senderId를 가져올 수 없습니다! 서버에서 사용자 ID를 제대로 전달했는지 확인하세요.");
        throw new Error("senderId가 설정되지 않았습니다. 사용자 인증 정보를 확인하세요.");
    }
    let activeChatRoomId = chatRoomId;
    let userId = senderId;
    let receiverId = null;
    let messagesCache = [];
    let isSending = false;
    let isConnected = false;
    let activeSubscriptions = [];
    const previewMessages = new Map();

    let modal = document.getElementById("imageModal");
    let modalImg = document.getElementById("modalImage");
    let closeBtn = modal.querySelector(".close");
    let navLeft = modal.querySelector(".nav.left");
    let navRight = modal.querySelector(".nav.right");
    let currentImageIndex = 0;
    let imageList = [];

    // chatUserName 초기값 가공
    const chatUserNameElement = document.getElementById("chatUserName");
    if (chatUserNameElement) {
        const initialChatUserName = chatUserNameElement.innerText;
        chatUserNameElement.innerText = initialChatUserName.split(",")[0].trim();
    }

    chatMessages.addEventListener("click", function (e) {
        if (e.target.tagName === "IMG" && e.target.closest(".message")) {
            const message = e.target.closest(".message");
            const groupedImages = message.querySelector(".grouped-images");

            if (groupedImages) {
                const urls = JSON.parse(groupedImages.getAttribute("data-urls") || "[]");
                imageList = urls.map(url => {
                    const img = new Image();
                    img.src = url;
                    return img;
                });
                currentImageIndex = Array.from(groupedImages.querySelectorAll(".group-image")).indexOf(e.target);
            } else {
                imageList = Array.from(chatMessages.querySelectorAll(".message .single-image")).map(img => {
                    const newImg = new Image();
                    newImg.src = img.src;
                    return newImg;
                });
                currentImageIndex = Array.from(chatMessages.querySelectorAll(".message .single-image")).indexOf(e.target);
            }

            if (currentImageIndex !== -1) {
                showModalImage(currentImageIndex);
            }
        }
    });
	
	function addChatRoomIfNotExists(chatRoom) {
	    if (!chatRoomsCache.some(r => r.id === chatRoom.id)) {
	        chatRoomsCache.push(chatRoom);
	    }
	}

    function showModalImage(index) {
        if (index < 0 || index >= imageList.length) return;
        modal.style.display = "block";
        modalImg.src = imageList[index].src;
    }

    closeBtn.onclick = () => {
        modal.style.display = "none";
        modalImg.src = "";
    };

    navLeft.onclick = () => {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            showModalImage(currentImageIndex);
        }
    };

    navRight.onclick = () => {
        if (currentImageIndex < imageList.length - 1) {
            currentImageIndex++;
            showModalImage(currentImageIndex);
        }
    };
	
	// 마우스 클릭으로 모달 닫기
	modal.addEventListener('click', function (e) {
	    // 모달 이미지 외부를 클릭한 경우에만 닫기
	    if (e.target === modal) {
	        modal.style.display = "none";
	        modalImg.src = "";
	    }
	});

    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape" && modal.style.display === "block") {
            modal.style.display = "none";
            modalImg.src = "";
        }
    });

    console.log("초기 chatRoomId:", chatRoomId);
    console.log("로그인된 userId:", senderId);

    if (!messageInput || !sendButton) {
        console.error("messageInput 또는 sendButton 요소를 찾을 수 없습니다! HTML을 확인하세요.");
        return;
    }
    if (!typingIndicator) {
        console.error("typingIndicator 요소가 없습니다! HTML을 확인하세요.");
    }
    if (!noChatMessage) {
        console.error("noChatMessage 요소를 찾을 수 없습니다! HTML을 확인하세요.");
    }
    if (!unreadFilter) {
        console.error("unreadFilter 체크박스를 찾을 수 없습니다! HTML을 확인하세요.");
        return;
    }
    if (!searchChat) {
        console.error("searchChat 요소를 찾을 수 없습니다! HTML을 확인하세요.");
        return;
    }

    // 안읽음 체크박스 이벤트 리스너 추가
    unreadFilter.addEventListener("change", function () {
        const showUnreadOnly = unreadFilter.checked;
        console.log("안읽음 필터 상태:", showUnreadOnly);
        filterAndUpdateChatList();
    });

    // 검색 입력창 이벤트 리스너 추가 (searchChat 사용)
    let debounceTimeout;
    searchChat.addEventListener("input", function () {
        clearTimeout(debounceTimeout);
        debounceTimeout = setTimeout(() => {
            const searchTerm = searchChat.value.trim();
            console.log("검색어:", searchTerm);
            filterAndUpdateChatList();
        }, 300);
    });

    // 채팅 목록 필터링 및 업데이트 함수
    function filterAndUpdateChatList() {
        const showUnreadOnly = unreadFilter.checked;
        const searchTerm = searchChat.value.trim().toLowerCase();

        // 안 읽음 필터링
        let filteredChatRooms = showUnreadOnly
            ? chatRoomsCache.filter(chat => chat.unreadCount > 0)
            : chatRoomsCache;

        // 검색어 필터링
        if (searchTerm) {
            filteredChatRooms = filteredChatRooms.filter(chat => {
                const otherUserName = chat.otherUserNickName ? chat.otherUserNickName.split(",")[0].trim().toLowerCase() : "";
                return otherUserName.includes(searchTerm);
            });
        }
		
		// 채팅방이 없을 경우 처리
		    if (filteredChatRooms.length === 0) {
		        noChatMessage.style.display = "block";
		        noChatMessage.innerHTML = "<p>채팅이 없어요!😢 <br> 새로운 예약을 통해 채팅을 시작하세요!🔥</p>";
		    } else {
		        noChatMessage.style.display = "none";
		    }

        updateChatList(filteredChatRooms);
    }

    // 시간 포맷 유틸 (하나로 통일)
    function formatTime(dateString, type = "time") {
        const date = new Date(dateString);
        if (isNaN(date.getTime())) return "";

        const today = new Date();
        const isToday = date.toDateString() === today.toDateString();

        if (type === "separator") {
            return isToday
                ? `오늘 ${date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false })}`
                : date.toLocaleDateString("ko-KR", { year: "2-digit", month: "numeric", day: "numeric", weekday: "long" });
        }

        if (type === "list") {
            return isToday
                ? date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: true })
                : date.toLocaleDateString("ko-KR", { month: "numeric", day: "numeric" });
        }

        return date.toLocaleTimeString("ko-KR", { hour: "2-digit", minute: "2-digit", hour12: false });
    }

	function sendImageGroup(files) {
	    const formData = new FormData();
	    files.forEach(file => formData.append("files", file));
	    formData.append("roomId", chatRoomId);
	    formData.append("senderId", senderId);

	    fetch("/api/chat/upload/multiple", {
	        method: "POST",
	        body: formData
	    })
	        .then(res => {
	            if (!res.ok) {
	                if (res.status === 413) {
	                    throw new Error("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.");
	                }
	                throw new Error(`이미지 업로드 실패: ${res.statusText}`);
	            }
	            return res.json();
	        })
	        .then(imageUrls => {
	            const chatMessage = {
	                chatRoomId,
	                senderId,
	                messageType: "IMAGE_GROUP",
	                content: JSON.stringify(imageUrls),
	                createdAt: new Date().toISOString()
	            };
	            // stompClient와 연결 상태 확인
	            if (!stompClient || !isConnected) {
	                console.error("WebSocket이 연결되지 않았습니다. 연결을 시도합니다...");
	                return connectWebSocket().then(() => {
	                    stompClient.publish({
	                        destination: "/app/chat.sendMessage",
	                        body: JSON.stringify(chatMessage)
	                    });
	                }).catch(err => {
	                    console.error("WebSocket 연결 실패:", err);
	                    alert("WebSocket 연결에 실패했습니다. 메시지를 전송할 수 없습니다.");
	                });
	            } else {
	                stompClient.publish({
	                    destination: "/app/chat.sendMessage",
	                    body: JSON.stringify(chatMessage)
	                });
	            }
	        })
	        .catch(err => {
	            console.error("이미지 그룹 전송 실패:", err);
	            alert(`이미지 전송에 실패했습니다: ${err.message}`);
	        });
	}

    function scrollToBottom(force = false) {
        const threshold = 100;
        const atBottom = chatMessages.scrollTop + chatMessages.clientHeight >= chatMessages.scrollHeight - threshold;
        if (atBottom || force) {
            requestAnimationFrame(() => {
                chatMessages.scrollTop = chatMessages.scrollHeight;
            });
        }
    }

    function sendAllMessages() {
        const messageContent = messageInput.value.trim();

        if (previewMessages.size > 0) {
            const files = Array.from(previewMessages.keys());
            sendImageGroup(files);
            previewMessages.forEach((previewElement, file) => {
                previewElement.remove();
                URL.revokeObjectURL(file);
            });
            previewMessages.clear();
        }

        if (messageContent !== "") {
            sendMessage();
        }

        scrollToBottom();
    }

	function connectWebSocket() {
	    // SockJS를 사용하여 WebSocket 연결 설정
	    const socket = new SockJS('/ws');
	    
	    // @stomp/stompjs 클라이언트 생성
	    stompClient = new StompJs.Client({
	        webSocketFactory: () => socket,
	        connectHeaders: {
	            "userId": senderId.toString()
	        },
	        debug: function (str) {
	            console.log("STOMP Debug:", str);
	        },
	        reconnectDelay: 5000, // 재연결 지연 시간 (5초)
	        heartbeatIncoming: 4000, // 서버에서 클라이언트로의 하트비트
	        heartbeatOutgoing: 4000, // 클라이언트에서 서버로의 하트비트
	    });

	    return new Promise((resolve, reject) => {
	        stompClient.onConnect = function (frame) {
	            console.log("✅ WebSocket 연결 성공:", frame);
	            isConnected = true;

	            // 사용자 메시지 큐 구독
	            stompClient.subscribe(`/user/queue/messages`, function (message) {
	                const msg = JSON.parse(message.body);
	                if (!messagesCache.find(m => m.id === msg.id)) {
	                    messagesCache.push(msg);
	                    displayMessage(msg);
	                }
	            });

	            resolve();
	        };

	        stompClient.onStompError = function (frame) {
	            console.error("❌ WebSocket 연결 실패:", frame);
	            isConnected = false;
	            stompClient = null;
	            setTimeout(() => {
	                connectWebSocket().then(resolve).catch(reject);
	            }, 5000);
	            reject(new Error("WebSocket 연결 실패"));
	        };

	        stompClient.onWebSocketClose = function (event) {
	            console.warn("WebSocket 연결이 닫혔습니다:", event);
	            isConnected = false;
	            stompClient = null;
	        };

	        // 클라이언트 활성화
	        stompClient.activate();
	    });
	}

	function subscribeToChatRoom(roomId) {
	    if (!isConnected || !stompClient) {
	        console.warn("WebSocket 연결 안됨. 연결 후 재시도...");
	        return connectWebSocket().then(() => subscribeToChatRoom(roomId));
	    }

	    unsubscribeAll();

	    const chatSubscription = stompClient.subscribe(`/topic/chat.${roomId}`, function (message) {
	        const msg = JSON.parse(message.body);
	        if (msg.type === "READ_UPDATE") {
	            if (parseInt(msg.chatRoomId) === chatRoomId) {
	                updateMessageReadStatus(msg);
	            }
	        } else if (msg.type === "ERROR") {
	            alert("에러: " + msg.message);
	        } else {
	            if (!messagesCache.find(m => m.id === msg.id)) {
	                messagesCache.push(msg);
	                displayMessage(msg);
					const chatRoom = chatRoomsCache.find(room => room.id === parseInt(msg.chatRoomId));
					                if (chatRoom) {
					                    chatRoom.lastMessage = msg.content;
					                    chatRoom.lastMessageTime = msg.createdAt;
					                    chatRoom.unreadCount = (msg.senderId !== senderId && roomId !== activeChatRoomId) ? (chatRoom.unreadCount || 0) + 1 : 0;
					                    filterAndUpdateChatList(); // UI 갱신
					                }
	            }
	        }
	    });
	    activeSubscriptions.push(chatSubscription);

	    const typingSubscription = stompClient.subscribe(`/topic/typing.${roomId}`, function (message) {
	        const data = JSON.parse(message.body);
	        if (data.senderId !== senderId && typingIndicator) {
	            typingIndicator.style.display = "block";
	            typingIndicator.textContent = "상대방이 입력 중...";
	            clearTimeout(typingTimeout);
	            typingTimeout = setTimeout(() => {
	                typingIndicator.style.display = "none";
	            }, 2000);
	        }
	    });
	    activeSubscriptions.push(typingSubscription);

	    stompClient.publish({
	        destination: `/app/chat.enterRoom.${roomId}`,
	        body: JSON.stringify({ userId: senderId })
	    });
		
		// 채팅방 입장 후 unreadCount 갱신
		const chatRoom = chatRoomsCache.find(room => room.id === roomId);
		    if (chatRoom) {
		        chatRoom.unreadCount = 0;
		        filterAndUpdateChatList();
		    }
	}

    function subscribeToUserStatus() {
        if (!isConnected) {
            console.warn("WebSocket이 연결되어 있지 않습니다. 먼저 연결을 시도합니다.");
            return connectWebSocket().then(() => subscribeToUserStatus());
        }

        stompClient.subscribe(`/topic/userStatus`, function (message) {
            const statusUpdate = JSON.parse(message.body);
            console.log("온라인 상태 업데이트 수신:", statusUpdate);
            const userId = parseInt(statusUpdate.userId);
            const isOnline = statusUpdate.isOnline;

            const chatItems = document.querySelectorAll(".chat-item");
            chatItems.forEach(item => {
                const chatRoomId = parseInt(item.getAttribute("data-id"));
                const chatRoom = chatRoomsCache.find(room => room.id === chatRoomId);
                if (chatRoom && chatRoom.otherUserId === userId) {
                    const statusIndicator = item.querySelector(".status-indicator");
                    if (statusIndicator) {
                        statusIndicator.textContent = isOnline ? "🟢" : "🔴";
                    }
                }
            });
        });
    }

	function subscribeToChatRoomUpdates() {
	    if (!stompClient || !stompClient.connected) {
	        console.error("STOMP 클라이언트가 연결되지 않았습니다.");
	        return;
	    }

	    stompClient.subscribe(`/topic/chatrooms.${senderId}`, function (message) {
	        const payload = JSON.parse(message.body);
	        console.log("채팅방 업데이트 메시지 수신:", payload);

	        if (payload.action === "create" && payload.chatRoomId) {
	            fetch(`/api/chat/rooms/${payload.chatRoomId}?currentUserId=${senderId}`)
	                .then(response => response.json())
	                .then(chatRoom => {
	                    if (!chatRoomsCache.find(room => room.id === chatRoom.id)) {
							if (!chatRoomsCache.find(room => room.id === chatRoom.id)) {
							    addChatRoomIfNotExists(chatRoom);
							}
	                        if (urlChatRoomId === chatRoom.id) {
	                            const chatItem = document.querySelector(`.chat-item[data-id='${chatRoom.id}']`);
	                            if (chatItem) openChatRoom(chatItem);
	                        }
							filterAndUpdateChatList();
	                    }
	                })
	                .catch(error => console.error("채팅방 정보 가져오기 실패:", error));
	        } else if (payload.action === "leave" && payload.chatRoomId) {
	            chatRoomsCache = chatRoomsCache.filter(room => room.id !== payload.chatRoomId);
	            updateChatList(chatRoomsCache);
				filterAndUpdateChatList();
	            if (activeChatRoomId === payload.chatRoomId) {
	                activeChatRoomId = null;
	                chatRoomId = null;
	                chatMain.setAttribute("data-chatroom-id", "");
	                document.getElementById("chatUserName").innerText = "";
	                messagesCache = [];
	                previewMessages.clear();
	                renderMessages();
	                window.history.pushState({}, '', '/chat');
	            }
	        }else if (payload.action === "update" && payload.chatRoomId) {
			            // 채팅방 정보 업데이트 (예: unreadCount 갱신)
			            fetch(`/api/chat/rooms/${payload.chatRoomId}?currentUserId=${senderId}`)
			                .then(response => response.json())
			                .then(updatedChatRoom => {
			                    const index = chatRoomsCache.findIndex(room => room.id === updatedChatRoom.id);
			                    if (index !== -1) {
			                        chatRoomsCache[index] = updatedChatRoom; // 캐시 업데이트
			                    } else {
			                        chatRoomsCache.push(updatedChatRoom);
			                    }
			                    filterAndUpdateChatList(); // UI 갱신
			                })
			                .catch(error => console.error("채팅방 업데이트 실패:", error));
			        }
	    });
	}

    function unsubscribeAll() {
        activeSubscriptions.forEach(subscription => {
            try {
                subscription.unsubscribe();
                console.log("구독 해제 성공");
            } catch (error) {
                console.warn("구독 해제 중 오류:", error);
            }
        });
        activeSubscriptions = [];
    }

	function initialize() {
	    connectWebSocket()
	        .then(() => {
	            subscribeToChatRoomUpdates();
	            subscribeToUserStatus();
	            return fetchChatRooms();
	        })
	        .then(() => {
	            console.log("fetchChatRooms 완료, chatRoomsCache:", chatRoomsCache);

	            // URL 파라미터 기반 채팅방 열기 시도
	            if (urlChatRoomId) {
	                const cachedRoom = chatRoomsCache.find(room => room.id === urlChatRoomId);
	                if (cachedRoom) {
	                    const chatItem = document.querySelector(`.chat-item[data-id='${urlChatRoomId}']`);
	                    if (chatItem) {
	                        openChatRoom(chatItem);
	                    } else {
	                        filterAndUpdateChatList();
	                        setTimeout(() => {
	                            const fallbackItem = document.querySelector(`.chat-item[data-id='${urlChatRoomId}']`);
	                            if (fallbackItem) openChatRoom(fallbackItem);
	                        }, 100);
	                    }
	                } else {
	                    // 캐시에 없으면 서버에서 강제로 로딩 후 열기
	                    fetch(`/api/chat/rooms/${urlChatRoomId}?currentUserId=${senderId}`)
	                        .then(res => {
	                            if (!res.ok) throw new Error("채팅방 조회 실패");
	                            return res.json();
	                        })
	                        .then(chatRoom => {
	                            addChatRoomIfNotExists(chatRoom);
	                            filterAndUpdateChatList();
	                            setTimeout(() => {
	                                const newChatItem = document.querySelector(`.chat-item[data-id='${chatRoom.id}']`);
	                                if (newChatItem) openChatRoom(newChatItem);
	                                else openChatRoomFallback(chatRoom); // 정말 마지막 fallback
	                            }, 100);
	                        })
	                        .catch(err => {
	                            console.error("URL 채팅방 로딩 실패:", err);
	                            alert("채팅방을 불러오지 못했습니다.");
	                        });
	                }
	            }
	        })
	        .catch(error => {
	            console.error("초기화 실패:", error);
	        });
	}

	
	function openChatRoomFallback(chatRoom) {
	    console.warn("fallback 방식으로 채팅방 열기 시도:", chatRoom);
	    chatRoomId = chatRoom.id;
	    activeChatRoomId = chatRoom.id;
	    chatMain.setAttribute("data-chatroom-id", chatRoom.id);
	    document.getElementById("chatUserName").innerText = chatRoom.otherUserNickName || "알 수 없는 사용자";

	    fetchChatMessages(chatRoom.id).then(() => {
	        subscribeToChatRoom(chatRoom.id);
	    });

	    window.history.pushState({}, '', `/chat?chatRoomId=${chatRoom.id}`);
	}



    let typingTimeout;
	messageInput.addEventListener("input", function () {
	    if (!chatRoomId || chatRoomId === 0 || isNaN(chatRoomId)) {
	        console.warn("chatRoomId가 설정되지 않았습니다. 타이핑 이벤트를 전송할 수 없습니다.");
	        return;
	    }
	    if (!isConnected || !stompClient) {
	        console.warn("WebSocket이 연결되어 있지 않습니다. 타이핑 이벤트를 전송할 수 없습니다.");
	        return;
	    }
	    console.log("타이핑 이벤트 전송 - senderId:", senderId);
	    clearTimeout(typingTimeout);
	    typingTimeout = setTimeout(() => {
	        stompClient.publish({
	            destination: `/app/chat.typing.${chatRoomId}`,
	            body: JSON.stringify({ senderId })
	        });
	    }, 500);
		
		// 채팅 전송 버튼 
      	const text = messageInput.value.trim();
          if (text !== "") {
              sendButton.classList.add("active");
              sendButton.classList.remove("disabled");
          } else {
              sendButton.classList.remove("active");
              sendButton.classList.add("disabled");
          }
	});

	function fetchChatMessages(chatRoomId, retryCount = 3) {
	    if (!chatRoomId || chatRoomId === 0 || isNaN(chatRoomId)) {
	        console.warn("chatRoomId가 유효하지 않습니다:", chatRoomId);
	        return Promise.reject("Invalid chatRoomId");
	    }
	    return fetch(`/api/chat/rooms/${chatRoomId}/messages?currentUserId=${senderId}`)
	        .then(response => {
	            if (!response.ok) {
	                if (response.status === 403) {
	                    // 사용자가 채팅방에 참여하지 않은 경우
	                    const shouldRejoin = confirm("이 채팅방에 참여하지 않았습니다. 다시 참여하시겠습니까?");
	                    if (shouldRejoin) {
	                        return fetch(`/api/chat/rooms/${chatRoomId}/rejoin?currentUserId=${senderId}`, {
	                            method: "POST"
	                        })
	                            .then(rejoinResponse => {
	                                if (!rejoinResponse.ok) {
	                                    throw new Error("재참여 실패");
	                                }
	                                // 재참여 성공 후 메시지 다시 조회
	                                return fetch(`/api/chat/rooms/${chatRoomId}/messages?currentUserId=${senderId}`);
	                            })
	                            .then(retryResponse => {
	                                if (!retryResponse.ok) {
	                                    throw new Error(`HTTP error! status: ${retryResponse.status}`);
	                                }
	                                return retryResponse.json();
	                            });
	                    } else {
	                        throw new Error("사용자가 채팅방에 참여하지 않았습니다.");
	                    }
	                }
	                throw new Error(`HTTP error! status: ${response.status}`);
	            }
	            return response.json();
	        })
	        .then(messages => {
	            console.log("API 응답 메시지:", messages);
	            if (!Array.isArray(messages)) {
	                console.error("채팅 메시지가 배열이 아닙니다!", messages);
	                return;
	            }
	            messagesCache = messages;
	            renderMessages();
	        })
	        .catch(error => {
	            console.error("채팅 메시지 가져오기 실패:", error);
	            if (retryCount > 0) {
	                console.log(`재시도 남은 횟수: ${retryCount}`);
	                return new Promise(resolve => setTimeout(resolve, 2000))
	                    .then(() => fetchChatMessages(chatRoomId, retryCount - 1));
	            }
	            alert("채팅 메시지를 가져오는 데 실패했습니다: " + error.message);
	            throw error;
	        });
	}

	function renderMessages() {
	    chatMessages.innerHTML = "";
	    let lastDate = null;

	    messagesCache.forEach((message, index) => {
	        const date = message.createdAt ? new Date(message.createdAt) : null;
	        const currentDateStr = date?.toDateString();
	        if (currentDateStr !== lastDate) {
	            const separator = document.createElement("div");
	            separator.classList.add("date-separator");
	            separator.textContent = formatTime(message.createdAt, "separator");
	            chatMessages.appendChild(separator);
	            lastDate = currentDateStr;
	        }

	        const el = createMessageElement(message, index === messagesCache.length - 1);
	        if (el) chatMessages.appendChild(el);
	    });

	    // 메시지가 없으면 "채팅이 없어요!" 표시
	    if (messagesCache.length === 0) {
	        noChatMessage.style.display = "block";
	        chatMessages.appendChild(noChatMessage);
	    } else {
	        noChatMessage.style.display = "none";
	    }

	    chatMessages.appendChild(typingIndicator);
	    requestAnimationFrame(() => {
	        scrollToBottom(true);
	    });
	}


    function createMessageElement(message, isLastMessage) {
        console.log(`메시지 ID: ${message.id}, senderName: ${message.senderName}`);
        const messageElement = document.createElement("div");
        const messageSenderId = parseInt(message.senderId);
        if (isNaN(messageSenderId)) {
            console.error("message.senderId가 유효한 숫자가 아닙니다:", message.senderId);
            return null;
        }
        const isSent = messageSenderId === senderId;
        console.log("메시지 정렬 확인 - message.senderId:", messageSenderId, "senderId:", senderId, "isSent:", isSent);
        messageElement.classList.add("message", isSent ? "sent" : "received");
        messageElement.setAttribute("data-message-id", message.id);

        const messageWrapper = document.createElement("div");
        messageWrapper.classList.add("message-wrapper");

        const contentElement = document.createElement("div");
        contentElement.classList.add("message-content");

        if (message.messageType === "IMAGE") {
            const img = document.createElement("img");
            img.src = message.content;
            img.classList.add("single-image");
            contentElement.appendChild(img);
        } else if (message.messageType === "IMAGE_GROUP") {
            const urls = JSON.parse(message.content);
            const wrapper = document.createElement("div");
            wrapper.classList.add("grouped-images");

            wrapper.setAttribute("data-urls", JSON.stringify(urls));

            const displayCount = Math.min(urls.length, 3);
            for (let i = 0; i < displayCount; i++) {
                const img = document.createElement("img");
                img.src = urls[i];
                img.classList.add("group-image");
                img.onerror = () => {
                    img.outerHTML = '<div class="image-error">이미지를 불러올 수 없습니다.</div>';
                };
                wrapper.appendChild(img);
            }

            if (urls.length > 3) {
                const countIndicator = document.createElement("div");
                countIndicator.classList.add("image-count");
                countIndicator.textContent = `${displayCount}/${urls.length}`;
                wrapper.appendChild(countIndicator);
            }

            contentElement.appendChild(wrapper);
            wrapper.scrollLeft = wrapper.scrollWidth;
        } else {
            contentElement.textContent = message.content;
        }

        messageWrapper.appendChild(contentElement);

        const statusElement = document.createElement("div");
        statusElement.classList.add("message-status");

        if (message.readBy) {
            receiverId = Object.keys(message.readBy).find(id => parseInt(id) !== senderId);
            console.log(`receiverId: ${receiverId}, senderId: ${senderId}`);
            console.log("message.readBy:", message.readBy);
            console.log("상대방이 읽었나요?", message.readBy[receiverId]);
        } else {
            console.warn(`메시지 ID: ${message.id}에 readBy 데이터가 없습니다.`);
        }

        if (isLastMessage) {
            if (isSent && message.readBy && receiverId && !message.readBy[receiverId]) {
                console.log(`마지막 메시지 ID: ${message.id}에 "안 읽음" 표시 추가`);
                const unreadIndicator = document.createElement("span");
                unreadIndicator.classList.add("unread-indicator");
                unreadIndicator.textContent = "안 읽음";
                statusElement.appendChild(unreadIndicator);
            }

            if (!isSent && message.readBy && message.readBy[senderId]) {
                console.log(`마지막 메시지 ID: ${message.id}에 "읽음" 표시 추가`);
                const readIndicator = document.createElement("span");
                readIndicator.classList.add("read-indicator");
                readIndicator.textContent = "읽음";
                statusElement.appendChild(readIndicator);
            }
        }

        if (message.createdAt) {
            const timeElement = document.createElement("span");
            timeElement.classList.add("message-time");
            timeElement.textContent = formatTime(message.createdAt, "time");
            statusElement.appendChild(timeElement);
        }

        messageWrapper.appendChild(statusElement);
        messageElement.appendChild(messageWrapper);

        return messageElement;
    }

    function displayMessage(message) {
        if (!message.readBy) {
            message.readBy = {};
        }

        if (parseInt(message.senderId) === senderId) {
            const receiverId = chatRoomsCache
                .find(r => r.id === chatRoomId)
                ?.otherUserId;
            if (receiverId) {
                message.readBy[receiverId] = false;
            }
        }
        renderMessages();
        scrollToBottom();
    }

    function updateMessageReadStatus(readUpdate) {
        const userId = readUpdate.userId;
        console.log(`updateMessageReadStatus 호출 - userId: ${userId}`);

        let updated = false;
        messagesCache = messagesCache.map(message => {
            if (message.readBy && !message.readBy[userId]) {
                console.log(`메시지 ID: ${message.id}의 읽음 상태 업데이트 - userId: ${userId}`);
                message.readBy[userId] = true;
                updated = true;
            }
            return message;
        });

        if (!updated) {
            console.log("업데이트된 메시지가 없습니다.");
			const chatRoom = chatRoomsCache.find(room => room.id === activeChatRoomId);
        }if (chatRoom) {
		    chatRoom.unreadCount = 0; // 읽음 상태 반영 후 읽지 않은 메시지 개수 초기화
		    filterAndUpdateChatList(); // 채팅방 목록 갱신
		} else {
            console.log("업데이트 후 messagesCache:", messagesCache);
        }

        renderMessages();
    }
	
	function updateChatRoomInfo(chatRoomId) {
	    const chatRoom = chatRoomsCache.find(room => room.id === chatRoomId);
	    if (!chatRoom) return;

	    // 마지막 메시지 및 읽음 상태 업데이트
	    filterAndUpdateChatList(); // UI 갱신

	    // 특정 채팅방에 대한 unreadCount 갱신
	    if (chatRoom.unreadCount === 0) {
	        console.log(`채팅방 ${chatRoomId}의 unreadCount가 0으로 업데이트되었습니다.`);
	    }
	}

	function openChatRoom(chatItem) {
	    let newChatRoomId = parseInt(chatItem.getAttribute("data-id"));
	    const chatRoom = chatRoomsCache.find(room => room.id === newChatRoomId);

	    if (!chatRoom) {
	        console.error("채팅방 정보를 찾을 수 없습니다:", newChatRoomId);
	        alert("채팅방 정보를 찾을 수 없습니다.");
	        return;
	    }
		
			// 안 읽음 카운트를 0으로 초기화
		    chatRoom.unreadCount = 0;
		    filterAndUpdateChatList(); // UI 갱신

	    document.querySelectorAll(".chat-item.active-room").forEach(item => {
	        item.classList.remove("active-room");
	    });
	    chatItem.classList.add("active-room");

	    activeChatRoomId = newChatRoomId;
	    chatMain.setAttribute("data-chatroom-id", newChatRoomId);
	    chatRoomId = newChatRoomId;

	    document.getElementById("chatUserName").innerText = chatRoom.otherUserNickName || "알 수 없는 사용자";

	    // 경고 메시지 표시 (첫 번째 열림 시에만)
	    const hasSeenWarning = localStorage.getItem(`warningSeen_${newChatRoomId}`);
	    if (!hasSeenWarning) {
	        const warningMessage = document.createElement("div");
	        warningMessage.classList.add("warning-message");
	        warningMessage.innerHTML = `
            <div class="warning-box">
              <div class="warning-text">
                <span class="warning-icon">⚠️</span>
                <p>
                  비방, 욕설, 성희롱, 개인정보 요구 등은<br>
                  <strong>모두 형사처벌 대상</strong>이 될 수 있습니다.<br>
                  피싱 · 스미싱 · 불법 거래 유도 등 사이버 범죄도<br>
                  <strong>엄중한 법적 책임</strong>을 지게 됩니다.
                </p>
              </div>
              <div class="warning-action">
                <button id="closeWarning">확인</button>
              </div>
            </div>
	        `;
	        chatMessages.innerHTML = ""; // 기존 내용 지우기
	        chatMessages.appendChild(warningMessage);

	        const closeButton = warningMessage.querySelector("#closeWarning");
	        closeButton.addEventListener("click", () => {
	            warningMessage.remove();
	            localStorage.setItem(`warningSeen_${newChatRoomId}`, "true");
	            fetchChatMessages(chatRoomId).then(() => {
	                subscribeToChatRoom(chatRoomId);
	            });
	        });
	    } else {
	        fetchChatMessages(chatRoomId).then(() => {
	            subscribeToChatRoom(chatRoomId);
	        });
	    }

	    window.history.pushState({}, '', `/chat?chatRoomId=${chatRoomId}`);
	}



	function updateChatList(chatRooms) {
	    console.log("채팅방 목록 데이터:", chatRooms);
	    chatRooms.sort((a, b) => {
	        const timeA = a.lastMessageTime ? new Date(a.lastMessageTime).getTime() : 0;
	        const timeB = b.lastMessageTime ? new Date(b.lastMessageTime).getTime() : 0;
	        return timeB - timeA;
	    });

	    const fragment = document.createDocumentFragment();
	    const searchTerm = searchChat.value.trim().toLowerCase();

	    if (chatRooms.length === 0) {
	        const noResults = document.createElement("li");
	        noResults.classList.add("no-results");
	        noResults.textContent = "채팅 리스트가 비어 있습니다.";
	        fragment.appendChild(noResults);
	    } else {
	        chatRooms.forEach(chat => {
	            console.log(`채팅방 ID: ${chat.id}, otherUserNickName: ${chat.otherUserNickName}`);
	            const chatItem = document.createElement("li");
	            chatItem.classList.add("chat-item");
	            chatItem.setAttribute("data-id", chat.id);
	            if (chat.id === activeChatRoomId) {
	                chatItem.classList.add("active-room");
	            }
	            let unreadBadge = (chat.unreadCount > 0)
	                ? `<span class="unread-badge small">${chat.unreadCount}</span>` 
	                : "";
	            let lastMessageTime = chat.lastMessageTime ? formatTime(chat.lastMessageTime, "list") : "";
	            let lastMessage = chat.lastMessage || "메시지가 없습니다.";
	            if (lastMessage.length > 20) {
	                lastMessage = lastMessage.substring(0, 20) + "...";
	            }
	            const statusIndicator = chat.otherUserIsOnline ? "🟢" : "🔴";
	            
	            const otherUserName = chat.otherUserNickName ? chat.otherUserNickName.split(",")[0].trim() : "알 수 없는 사용자";
	            
	            // 검색어 하이라이팅
	            let displayName = otherUserName;
	            if (searchTerm) {
	                const regex = new RegExp(`(${searchTerm})`, "gi");
	                displayName = otherUserName.replace(regex, '<span class="highlight">$1</span>');
	            }

                let avatarContent;
                if (!chat.otherUserAvatar || chat.otherUserAvatar.trim() === "") {
                    console.log(`채팅방 ID: ${chat.id}의 otherUserAvatar가 비어 있습니다. 기본 이모지 표시.`);
                    avatarContent = `<div class="chat-avatar emoji">🐸</div>`;
                } else {
                    const avatarPath = `/images/avatar/${chat.otherUserAvatar}`;
                    console.log(`이미지 경로: ${avatarPath}`); // 디버깅 로그 추가
                    avatarContent = `<img src="${avatarPath}" class="chat-avatar" alt="상대방 이미지" onerror="console.error('이미지 로드 실패: ${avatarPath}'); this.outerHTML='<div class=\\'chat-avatar emoji\\'>🐸</div>'">`;
                }

	            chatItem.innerHTML = `
	                <input type="checkbox" class="chat-select-checkbox">
	                <div class="chat-avatar-wrapper">
	                    ${avatarContent}
	                    <span class="status-indicator">${statusIndicator}</span>
	                </div>
	                <div class="chat-info">
	                    <div class="chat-title">
	                        <span>${displayName}</span>
	                        <span class="chat-time">${lastMessageTime}</span>
	                    </div>
	                    <div class="chat-preview">
	                        <span>${lastMessage}</span>
	                        ${chat.unreadCount > 0 ? `<span class="unread-badge small">${chat.unreadCount}</span>` : ""}
	                    </div>
	                </div>
	            `;
	            chatItem.addEventListener("click", function (e) {
	                if (e.target.classList.contains("chat-select-checkbox")) return;
	                openChatRoom(chatItem);
	            });
	            fragment.appendChild(chatItem);
	        });
	    }

	    chatList.innerHTML = "";
	    chatList.appendChild(fragment);
	}
	
	// 채팅 버튼 클릭 시 호출
	function getCurrentUserId() {
	    const userId = document.body.dataset.userId;
	    if (!userId) {
	        console.error("사용자 ID를 찾을 수 없습니다.");
	        throw new Error("사용자 ID가 설정되지 않았습니다.");
	    }
	    return parseInt(userId, 10);
	}
	
	let isCreatingChatRoom = false;

	const chatButtons = document.querySelectorAll(".btn-chat");
	chatButtons.forEach(button => {
	    button.addEventListener("click", debounce(async () => {
	        if (isCreatingChatRoom) return;
	        isCreatingChatRoom = true;
	        button.disabled = true;

	        const otherUserId = parseInt(button.getAttribute("data-other-user-id"));
	        if (!otherUserId) {
	            console.error("상대방 ID가 없습니다!");
	            alert("상대방 ID가 필요합니다.");
	            button.disabled = false;
	            isCreatingChatRoom = false;
	            return;
	        }

	        try {
	            if (!chatRoomsCache || chatRoomsCache.length === 0) {
	                await fetchChatRooms();
	            }
	            await ensureChatRoom(senderId, otherUserId);
	        } catch (err) {
	            console.error("채팅 버튼 처리 중 오류:", err);
	            alert("채팅방 생성 실패: " + err.message);
	        } finally {
	            button.disabled = false;
	            isCreatingChatRoom = false;
	        }
	    }, 300));
	});

	function debounce(func, wait) {
	    let timeout;
	    return function (...args) {
	        clearTimeout(timeout);
	        timeout = setTimeout(() => func.apply(this, args), wait);
	    };
	}

	function formatDateTime(dateTime) {
	    if (!dateTime) return "";
	    try {
	        const date = new Date(dateTime);
	        if (isNaN(date.getTime())) {
	            console.error("잘못된 날짜 형식:", dateTime);
	            return "";
	        }
	        return date.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
	    } catch (error) {
	        console.error("날짜 포맷팅 오류:", error, dateTime);
	        return "";
	    }
	}

    let lastSentTime = 0;
    const MIN_SEND_INTERVAL = 1000;

	function sendMessage() {
	    const now = Date.now();
	    if (now - lastSentTime < MIN_SEND_INTERVAL) {
	        console.log("너무 빠른 메시지 전송 시도. 무시됨.");
	        return;
	    }

	    if (isSending) {
	        console.log("이미 메시지 전송 중입니다. 중복 전송 방지.");
	        return;
	    }
	    isSending = true;
	    lastSentTime = now;

	    if (!chatRoomId || chatRoomId === 0 || isNaN(chatRoomId)) {
	        console.warn("채팅방이 선택되지 않았습니다. 메시지를 전송할 수 없습니다.");
	        alert("채팅방을 먼저 선택해주세요!");
	        isSending = false;
	        return;
	    }

	    const messageContent = messageInput.value.trim();
	    if (messageContent === "") {
	        isSending = false;
	        return;
	    }
	    const chatMessage = {
	        chatRoomId: chatRoomId,
	        senderId: senderId,
	        messageType: "TEXT",
	        content: messageContent,
	        createdAt: new Date().toISOString()
	    };
	    console.log("전송할 메시지:", chatMessage);
	    if (isConnected && stompClient) {
	        stompClient.publish({
	            destination: "/app/chat.sendMessage",
	            body: JSON.stringify(chatMessage)
	        });
	    } else {
	        console.warn("WebSocket이 연결되어 있지 않습니다. 연결을 시도합니다...");
	        connectWebSocket().then(() => {
	            stompClient.publish({
	                destination: "/app/chat.sendMessage",
	                body: JSON.stringify(chatMessage)
	            });
	        }).catch(err => {
	            console.error("WebSocket 연결 실패:", err);
	            alert("WebSocket 연결에 실패했습니다. 메시지를 전송할 수 없습니다.");
	        });
	    }
	    messageInput.value = "";
	    scrollToBottom();
	    setTimeout(() => {
	        isSending = false;
	    }, 500);
	}

    sendButton.addEventListener("click", () => {
        if (previewMessages.size > 0) {
            const files = Array.from(previewMessages.keys());
            sendImageGroup(files);
            previewMessages.forEach((previewElement, file) => {
                previewElement.remove();
                URL.revokeObjectURL(file);
            });
            previewMessages.clear();
        } else {
            sendMessage();
        }
    });

    messageInput.addEventListener("keypress", function (event) {
        if (event.key === "Enter") {
            event.preventDefault();
            sendAllMessages();
            scrollToBottom();
        }
    });

    attachButton.addEventListener("click", function () {
        fileInput.click();
    });

    async function resizeImage(file, maxWidth = 1024, maxHeight = 1024, quality = 0.8) {
        return new Promise((resolve, reject) => {
            const img = new Image();
            img.src = URL.createObjectURL(file);
            img.onload = () => {
                const canvas = document.createElement("canvas");
                let width = img.width;
                let height = img.height;

                if (width > height) {
                    if (width > maxWidth) {
                        height = Math.round((height * maxWidth) / width);
                        width = maxWidth;
                    }
                } else {
                    if (height > maxHeight) {
                        width = Math.round((width * maxHeight) / height);
                        height = maxHeight;
                    }
                }

                canvas.width = width;
                canvas.height = height;
                const ctx = canvas.getContext("2d");
                ctx.drawImage(img, 0, 0, width, height);

                canvas.toBlob(blob => {
                    const resizedFile = new File([blob], file.name, {
                        type: file.type,
                        lastModified: Date.now()
                    });
                    resolve(resizedFile);
                }, file.type, quality);
            };
            img.onerror = reject;
        });
    }

    fileInput.addEventListener("change", async function (event) {
        let files = Array.from(event.target.files);
        if (files.length === 0) return;

        const MAX_FILE_SIZE = 100 * 1024 * 1024;
        let oversizedFiles = files.filter(file => file.size > MAX_FILE_SIZE);
        if (oversizedFiles.length > 0) {
            alert(`다음 파일이 100MB를 초과하여 업로드할 수 없습니다: ${oversizedFiles.map(file => file.name).join(", ")}`);
            files = files.filter(file => file.size <= MAX_FILE_SIZE);
        }

        if (files.length === 0) return;

        let quality = 0.8;
        let resizedFiles = await Promise.all(files.map(file => resizeImage(file, 1024, 1024, quality)));

        let attempts = 0;
        const maxAttempts = 3;
        while (attempts < maxAttempts) {
            oversizedFiles = resizedFiles.filter(file => file.size > MAX_FILE_SIZE);
            if (oversizedFiles.length === 0) break;

            quality -= 0.2;
            if (quality <= 0) {
                alert(`다음 파일은 리사이징 후에도 10MB를 초과하여 업로드할 수 없습니다: ${oversizedFiles.map(file => file.name).join(", ")}`);
                resizedFiles = resizedFiles.filter(file => file.size <= MAX_FILE_SIZE);
                break;
            }
            resizedFiles = await Promise.all(files.map(file => resizeImage(file, 1024, 1024, quality)));
            attempts++;
        }

        if (resizedFiles.length === 0) return;

        if (resizedFiles.length > 10) {
            alert("최대 10개의 이미지만 첨부할 수 있습니다.");
            resizedFiles = resizedFiles.slice(0, 10);
        }

        const supportedFormats = ["image/jpeg", "image/png", "image/gif"];
        resizedFiles = resizedFiles.filter(file => {
            if (!supportedFormats.includes(file.type)) {
                alert(`${file.name}은 지원하지 않는 파일 형식입니다. JPEG, PNG, GIF만 지원합니다.`);
                return false;
            }
            return true;
        });

        if (resizedFiles.length === 0) return;

        previewMessages.forEach((previewElement, file) => {
            previewElement.remove();
            URL.revokeObjectURL(file);
        });
        previewMessages.clear();

        const messageElement = document.createElement("div");
        messageElement.classList.add("message", "sent", "preview");

        const messageWrapper = document.createElement("div");
        messageWrapper.classList.add("message-wrapper");

        const contentElement = document.createElement("div");
        contentElement.classList.add("message-content");

        const imageWrapper = document.createElement("div");
        imageWrapper.classList.add("preview-images");

        resizedFiles.forEach((file, index) => {
            const img = document.createElement("img");
            img.src = URL.createObjectURL(file);
            img.classList.add("preview-image");
            imageWrapper.appendChild(img);

            previewMessages.set(file, messageElement);

            if (index === resizedFiles.length - 1) {
                const actions = document.createElement("div");
                actions.classList.add("preview-actions");

                const sendButton = document.createElement("button");
                sendButton.classList.add("send-preview");
                sendButton.textContent = "전송";
                sendButton.addEventListener("click", () => {
                    sendButton.disabled = true;
                    cancelButton.disabled = true;
                    sendAllMessages().finally(() => {
                        sendButton.disabled = false;
                        cancelButton.disabled = false;
                    });
                });

                const cancelButton = document.createElement("button");
                cancelButton.classList.add("cancel-preview");
                cancelButton.textContent = "취소";
                cancelButton.addEventListener("click", () => {
                    previewMessages.forEach((_, file) => {
                        URL.revokeObjectURL(file);
                    });
                    previewMessages.clear();
                    messageElement.remove();
                });

                actions.appendChild(sendButton);
                actions.appendChild(cancelButton);
                contentElement.appendChild(actions);
            }
        });

        contentElement.appendChild(imageWrapper);
        messageWrapper.appendChild(contentElement);
        messageElement.appendChild(messageWrapper);

        chatMessages.appendChild(messageElement);
        scrollToBottom();

        imageWrapper.scrollLeft = imageWrapper.scrollWidth;

        fileInput.value = "";
    });

	function sendImage(file) {
	    if (!file) return;
	    const formData = new FormData();
	    formData.append("file", file);
	    formData.append("roomId", chatRoomId);
	    formData.append("senderId", senderId);

	    fetch("/api/chat/upload", {
	        method: "POST",
	        body: formData
	    })
	        .then(response => {
	            if (!response.ok) {
	                if (response.status === 413) {
	                    throw new Error("파일 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다.");
	                }
	                throw new Error(`파일 업로드 실패: ${response.statusText}`);
	            }
	            return response.text();
	        })
	        .then(imageUrl => {
	            const chatMessage = {
	                chatRoomId: chatRoomId,
	                senderId: senderId,
	                messageType: "IMAGE",
	                content: imageUrl
	            };
	            if (isConnected && stompClient) {
	                stompClient.publish({
	                    destination: "/app/chat.sendMessage",
	                    body: JSON.stringify(chatMessage)
	                });
	                scrollToBottom();
	            } else {
	                console.warn("WebSocket이 연결되어 있지 않습니다. 연결을 시도합니다...");
	                connectWebSocket().then(() => {
	                    stompClient.publish({
	                        destination: "/app/chat.sendMessage",
	                        body: JSON.stringify(chatMessage)
	                    });
	                    scrollToBottom();
	                }).catch(err => {
	                    console.error("WebSocket 연결 실패:", err);
	                    alert("WebSocket 연결에 실패했습니다. 메시지를 전송할 수 없습니다.");
	                });
	            }
	        })
	        .catch(error => {
	            console.error("파일 업로드 실패:", error);
	            alert(`파일 업로드에 실패했습니다: ${file.name} - ${error.message}`);
	        });
	}
	
	let chatRoomCreationLock = new Map();

	async function ensureChatRoom(userId1, userId2) {
	    const key = `${userId1}-${userId2}`;
	    if (chatRoomCreationLock.has(key)) {
	        console.log(`이미 ${key}에 대한 채팅방 생성 요청 진행 중`);
	        return chatRoomCreationLock.get(key);
	    }

	    const promise = (async () => {
	        try {
	            const res = await fetch(`/api/chat/rooms/find?userId1=${userId1}&userId2=${userId2}`);
	            if (res.ok) {
	                const roomDto = await res.json();
	                if (!chatRoomsCache.find(r => r.id === roomDto.id)) {
	                    addChatRoomIfNotExists(roomDto);
	                    filterAndUpdateChatList();
	                }
	                const chatItem = document.querySelector(`.chat-item[data-id='${roomDto.id}']`);
	                if (chatItem) openChatRoom(chatItem);
	                return roomDto;
	            }

	            const response = await fetch(`/api/chat/rooms?userId1=${userId1}&userId2=${userId2}`, {
	                method: "POST",
	                headers: { "Content-Type": "application/json" }
	            });

	            if (!response.ok) {
	                const error = await response.json();
	                throw new Error(error.error || "채팅방 생성 실패");
	            }

	            const chatRoomDto = await response.json();
	            chatRoomsCache.push(chatRoomDto);
	            filterAndUpdateChatList();
	            const chatItem = document.querySelector(`.chat-item[data-id='${chatRoomDto.id}']`);
	            if (chatItem) openChatRoom(chatItem);
	            return chatRoomDto;
	        } catch (err) {
	            console.error("채팅방 처리 중 오류:", err);
	            throw err;
	        } finally {
	            chatRoomCreationLock.delete(key);
	        }
	    })();

	    chatRoomCreationLock.set(key, promise);
	    return promise;
	}

    let chatRoomsCache = [];

	function fetchChatRooms(retryCount = 3) {
	    return fetch(`/api/chat/rooms/user/${userId}`)
	        .then(response => {
	            if (!response.ok) throw new Error("채팅방 목록 가져오기 실패");
	            return response.json();
	        })
	        .then(data => {
	            console.log("채팅방 목록:", data);
	            chatRoomsCache = data;
	            filterAndUpdateChatList();

	            if (urlChatRoomId) {
	                const targetChatRoom = chatRoomsCache.find(room => room.id === urlChatRoomId);
	                if (targetChatRoom) {
	                    setTimeout(() => {
	                        const targetChatItem = document.querySelector(`.chat-item[data-id='${urlChatRoomId}']`);
	                        if (targetChatItem) {
	                            targetChatItem.click();
	                        }
	                    }, 100);
	                } else {
	                    console.warn("URL chatRoomId에 해당하는 채팅방을 찾을 수 없습니다:", urlChatRoomId);
	                    // 서버에서 직접 조회
	                    fetch(`/api/chat/rooms/${urlChatRoomId}?currentUserId=${senderId}`)
	                        .then(res => {
	                            if (!res.ok) {
	                                throw new Error("채팅방 조회 실패");
	                            }
	                            return res.json();
	                        })
	                        .then(chatRoom => {
								if (!chatRoomsCache.find(room => room.id === chatRoom.id)) {
								    addChatRoomIfNotExists(chatRoom);
								}
	                            setTimeout(() => {
	                                const targetChatItem = document.querySelector(`.chat-item[data-id='${urlChatRoomId}']`);
	                                if (targetChatItem) {
	                                    targetChatItem.click();
	                                }
	                            }, 100);
	                        })
	                        .catch(error => {
	                            console.error("채팅방 조회 실패:", error);
	                            alert("해당 채팅방을 찾을 수 없습니다: " + error.message);
	                        });
	                }
	            }
	        })
	        .catch(error => {
	            console.error("채팅방 목록 가져오기 실패:", error);
	            if (retryCount > 0) {
	                setTimeout(() => fetchChatRooms(retryCount - 1), 2000);
	            } else {
	                chatList.innerHTML = '<li class="error">채팅방 목록을 불러오지 못했습니다.</li>';
	            }
	        });
	}


    initialize();

	window.addEventListener('beforeunload', function () {
	    unsubscribeAll();
	    if (stompClient && isConnected) {
	        stompClient.deactivate();
	    }
	});

    const deleteBtn = document.querySelector(".delete-btn");
	deleteBtn.addEventListener("click", async () => {
	    const selectedChatIds = Array.from(document.querySelectorAll(".chat-select-checkbox"))
	        .filter(checkbox => checkbox.checked)
	        .map(checkbox => {
	            const chatItem = checkbox.closest(".chat-item");
	            return parseInt(chatItem.getAttribute("data-id"));
	        });

	    if (selectedChatIds.length === 0) {
	        alert("삭제할 채팅방을 선택해주세요.");
	        return;
	    }

	    const confirmDelete = confirm(`${selectedChatIds.length}개 채팅방에서 나가시겠습니까?`);
	    if (!confirmDelete) return;

	    for (const chatRoomIdToLeave of selectedChatIds) {
	        try {
	            const response = await fetch(`/api/chat/rooms/${chatRoomIdToLeave}/leave`, {
	                method: "POST",
	                headers: {
	                    "Content-Type": "application/json"
	                },
	                body: JSON.stringify({ userId: senderId })
	            });

	            if (!response.ok) {
	                const errorData = await response.json();
	                throw new Error(errorData.error || "나가기 실패");
	            }

	            const data = await response.json();
	            console.log(`채팅방 ${chatRoomIdToLeave} 나가기 완료, 삭제 여부: ${data.isDeleted}`);

	            // 현재 열린 채팅방이면 UI 초기화
	            if (chatRoomId === chatRoomIdToLeave) {
	                chatRoomId = null;
	                activeChatRoomId = null;
	                chatMain.setAttribute("data-chatroom-id", "");
	                document.getElementById("chatUserName").innerText = "";
	                chatMessages.innerHTML = "";
	                messagesCache = [];
	                typingIndicator.style.display = "none";
	                unsubscribeAll();
	            }

	            // 캐시와 UI 업데이트는 마지막에 한 번만 수행할 수 있도록
	            if (data.isDeleted) {
	                chatRoomsCache = chatRoomsCache.filter(room => room.id !== chatRoomIdToLeave);
	            }

	        } catch (err) {
	            console.error(`채팅방 ${chatRoomIdToLeave} 나가기 중 오류:`, err);
	            alert(`채팅방 나가기에 실패했습니다: ${err.message}`);
	        }
	    }

	    // 모든 삭제 완료 후 UI 갱신
	    filterAndUpdateChatList();
	});

});