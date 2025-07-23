document.addEventListener('DOMContentLoaded', () => {

    const myProductTab = document.getElementById("myProductTab");
    const myProductDiv = document.getElementById("myProductDiv");
    const likeProductTab = document.getElementById("likeProductTab")
    const likeProductDiv = document.getElementById("likeProductDiv");
    const divMoreViewMyProduct = document.getElementById("divMoreViewMyProduct");
    const divMoreViewLike = document.getElementById("divMoreViewLike");
    let uri = '';
    axiosPaging(1, 0);
    myProductTab.addEventListener('click', () => {
        myProductDiv.innerHTML = '';
        axiosPaging(1, 0)
    });

    likeProductTab.addEventListener('click', () => {
        likeProductDiv.innerHTML = '';
        axiosPaging(2, 0)
    });
	
	// 가격에 5%를 적용하고 10원 단위로 반올림하는 함수
	function calculateServiceFee(originalPrice) {
	    // 5% 적용
	    const feeAmount = originalPrice * 0.05;
	    
	    // 10원 단위로 반올림
	    // Math.round를 사용하여 5원 이상은 올림, 4원 이하는 내림
	    const roundedFee = Math.round(feeAmount / 10) * 10;
	    
	    return roundedFee;
	}
    

    // functions
	function axiosPaging(category, pageNum) {
	    switch(category){
	        case 1: // 내 상품
	            uri = `/api/mypage/myproduct?p=${pageNum}`;
	            axios
	            .get(uri)
	            .then((response) => { myProductPaging(response.data) })
	            .catch((error) => { console.log(error); });
	            break;
	        case 2: // 찜 상품
	            uri = `/api/mypage/likeproduct?p=${pageNum}`;
	            axios
	            .get(uri)
	            .then((response) => { likeProductPaging(response.data) })
	            .catch((error) => { console.log(error); });
	            break;
	    }
	}

	function myProductPaging(data) {
	    if(data.totalElements == 0) {
	        myProductDiv.innerHTML = '등록한 상품이 없습니다!'
	        return;
	    }
	    let html = '';
	    data.content.forEach(product => {
	        // 원래 가격에 5% 서비스 수수료 적용
			console.log('원래 가격:', product.fee, typeof product.fee);
	        const serviceFee = calculateServiceFee(product.fee);
			console.log('서비스 수수료:', serviceFee);
	        const displayPrice = product.fee + serviceFee;
			console.log('최종 표시 가격:', displayPrice);
	        
	        html += `
	                <div class="product-card">`;
	                switch(product.rentalCategoryId) {
	                    case 1:
	                        html += `
	                        <a href="/post/details/bag?id=${product.id}">`
	                        break;
	                    case 2:
	                        html += `
	                        <a href="/post/details/car?id=${product.id}">`
	                        break;
	                }
	        html += `
	                            <img src="${product.filePath}" alt="상품 이미지"> </a>
	                        <p>`
	                switch(product.rentalCategoryId) {
	                    case 1:
	                        html += `
	                            <a href="/post/details/bag?id=${product.id}">`
	                        break;
	                    case 2:
	                        html += `
	                            <a href="/post/details/car?id=${product.id}">`
	                        break;
	                }
	        html += `
	                                <p class="product-name"><strong>${product.productName}</strong></p>
	                            </a>
	                        </p>
	                        <p class="product-fee">${displayPrice.toLocaleString()}원</p>
	                    </div>
	                `;
	    });

	    myProductDiv.innerHTML += html;

	    console.log(data);
	    const pageNumber = data.pageable.pageNumber;
	    const totalPages = data.totalPages
	    if(pageNumber < totalPages-1) {
	        moreView(1, pageNumber, totalPages);
	    } else {
	        deleteBtnMoreView(1);
	    }
	}

	function likeProductPaging(data) {
	    if(data.totalElements === 0) {
	        likeProductDiv.innerHTML = '<span>찜한 상품이 없습니다!</span>'
	        return;
	    }
	    let html = ''
	    data.content.forEach(product => {
	        // 원래 가격에 5% 서비스 수수료 적용
	        const serviceFee = calculateServiceFee(product.fee);
	        const displayPrice = product.fee + serviceFee;
	        
	        html += `
	            <div class="product-card">
	                <button class="heart-btn active" onclick="toggleHeart(this)">❤️</button>`
	        switch(product.rentalCategoryId) {
	            case 1:
	                html += `
	                <a href="/post/details/bag?id=${product.id}">`
	                break;
	            case 2:
	                html += `
	                <a href="/post/details/car?id=${product.id}">`
	                break;
	        }
	        html += `
	                    <img src="${product.filePath}" alt="찜상품">
	                </a>
	                <p>`
	        switch(product.rentalCategoryId) {
	            case 1:
	                html += `
	                    <a href="/post/details/bag?id=${product.id}">`
	                break;
	            case 2:
	                html += `
	                    <a href="/post/details/car?id=${product.id}">`
	                break;
	        }
	        html += `
	                        <strong>${product.productName}</strong>
	                    </a>
	                </p>
	                <p><strong>${displayPrice.toLocaleString()}원</strong></p>
	                <button class="delete-btn" style="display: none;" onclick="deleteCard(this, ${product.id})">삭제</button>
	            </div>`
	    });
	    likeProductDiv.innerHTML += html;

	    console.log(data);
	    const pageNumber = data.pageable.pageNumber;
	    const totalPages = data.totalPages
	    console.log(pageNumber, totalPages);
	    if(pageNumber < totalPages-1) {
	        moreView(2, pageNumber, totalPages);
	    } else {
	        deleteBtnMoreView(2);
	    }
	}
    
    	// 좋아요 기능
        function toggleHeart(btn) {
           const card = btn.closest(".product-card");
           const deleteBtn = card.querySelector(".delete-btn");
         
           if (btn.classList.contains("active")) {
             btn.classList.remove("active");
             btn.textContent = "🤍"; // 하트 비활성
             deleteBtn.style.display = "block"; // 삭제 버튼 표시
           } else {
             btn.classList.add("active");
             btn.textContent = "❤️"; // 하트 활성
             deleteBtn.style.display = "none";
           }
         }
         
         window.toggleHeart = toggleHeart;
         
         //  찜 카드 삭제
         function deleteCard(btn, productId) {
           const card = btn.closest(".product-card");
           if (confirm("상품을 삭제하시겠습니까?")) {
             fetch(`/api/like/no?id=${productId}`)
                 .then((response) => response)
                 .then((data) => {
                     console.log(data);
                     card.remove();
                 })
                 .catch(error => {
                     console.error("좋아요 해제 실패:", error);
                 });
           }
         }
         window.deleteCard = deleteCard;


		 function moreView(category, pageNumber, totalPages) {
		     console.log("더보기 버튼 그리기");
		     let html = ''; // html 변수를 함수 스코프 내에 선언
		     switch(category) {
		         case 1:
		             html = `
		             <button class="moreViewBtn" id="btnMoreViewMyProduct">더보기(${pageNumber+1} / ${totalPages})</button> `;
		             divMoreViewMyProduct.innerHTML = html;
		             document.getElementById("btnMoreViewMyProduct").addEventListener('click', () => { // id 변경
		                 axiosPaging(category, pageNumber+1);
		             });
		             break;
		         case 2:
		             html = `
		             <button class="moreViewBtn" id="btnMoreViewLike">더보기(${pageNumber+1} / ${totalPages})</button> `;
		             divMoreViewLike.innerHTML = html;
		             document.getElementById("btnMoreViewLike").addEventListener('click', () => { // id 변경
		                 axiosPaging(category, pageNumber+1);
		             });
		             break;
		     }
		 }

    function deleteBtnMoreView(category) {
        switch(category) {
            case 1:
                document.getElementById("divMoreViewMyProduct").innerHTML = '';
                break;
            case 2:
                document.getElementById("divMoreViewLike").innerHTML = '';
                break;
        }
    }
});