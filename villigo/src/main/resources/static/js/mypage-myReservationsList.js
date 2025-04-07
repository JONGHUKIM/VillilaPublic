/**
 * Rental Images 경로와 Details 주소만 생성
 */
function makeMyReservationElements({ content }) {
    let htmlStr = '';
    if (content.length === 0) {
        htmlStr = '<p>예약이 없습니다.</p>';
    } else {
        for (const dto of content) {
            // 상품 디테일 페이지 링크 URL 생성
            let postDetailsUrl = '/products/details';
            switch (dto.rentalCategoryId) {
                case 1: // bag
                    postDetailsUrl += `/bag/${dto.productId}`;
                    break;
                case 2: // car
                    postDetailsUrl += `/car/${dto.productId}`;
                    break;
            }

            // Rental Images 경로 생성
            const imagePath = `/images/rentalImages/${dto.imagePath}`;

            // 경로와 주소만 포함된 HTML
            htmlStr += `
            <div class="reservation-card" data-reservation-id="${dto.reservationId}">
              <div class="res-img">
                <a href="${postDetailsUrl}">
                  <img src="${imagePath}" alt="차량 이미지">
                </a>
              </div>
              <div class="res-info">
                <p class="car-name">
                  <a href="${postDetailsUrl}"><strong>${dto.productName}</strong></a>
                </p>
              </div>
            </div>
            `;
        }
    }
    return htmlStr;
}