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
    

    // functions
    function axiosPaging(category, pageNum) {
        switch(category){
            case 1: // 내 상품
                uri = `/api/mypage/myproduct?p=${pageNum}`;
                axios
                .get(uri)
                .then((response) => { myProductPaging(response.data) })
                .catch((error) => { console.log(error); });
            case 2: // 찜 상품
                uri = `/api/mypage/likeproduct?p=${pageNum}`;
                axios
                .get(uri)
                .then((response) => { likeProductPaging(response.data) })
                .catch((error) => { console.log(error); });
        }

    }

	function myProductPaging(data) {
	    if(data.totalElements == 0) {
	        myProductDiv.innerHTML = '등록한 상품이 없습니다!'
	        return;
	    }
	    let html = '';
	    data.content.forEach(product => { // product는 ProductImageMergeDto 객체
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
	                        <p class="product-fee">${product.fee}JJAM</p>
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
	        likeProductDiv.innerHTML = '<span>찜 목록이 비어있습니다!</span>' // 메시지 변경
	        return;
	    }
	    let html = ''
	    data.content.forEach(product => { // product는 ProductImageMergeDto 객체
			    html += `
			        <div class="product-card">
			            <button class="heart-btn active" onclick="toggleHeart(this)">❤️</button>` // productId를 인자로 넘기도록 수정할 수 있음
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
			                    <img src="${product.filePath}" alt="찜상품"> </a>
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
			                <p><strong>${product.fee}JJAM</strong></p>
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
        switch(category) {
            case 1:
                html = `
                <button class="moreViewBtn" id="btnMoreView">더보기(${pageNumber+1} / ${totalPages})</button>
                `;
                divMoreViewMyProduct.innerHTML = html;
                break;
            case 2:
                html = `
                <button class="moreViewBtn" id="btnMoreView">더보기(${pageNumber+1} / ${totalPages})</button>
                `;
                divMoreViewLike.innerHTML = html;
        }

        document.getElementById("btnMoreView").addEventListener('click', () => {
            axiosPaging(category, pageNumber+1)
        });
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