
// -------------------------
// 브랜드 데이터
// -------------------------

// 브랜드 데이터 불러오기 완료
/*const brandData = {
  car: [
    { name: 'BMW', img: '/images/test/Alfa-Romeo.png' },
    { name: '벤츠', img: '/images/test/Aston_Martin.png' },
    { name: '현대', img: '/images/test/Jaguar.png' },
    { name: '기아', img: '/images/test/Koenigsegg.png' },
    { name: '기아', img: '/images/test/Lagonda.png' },
    { name: '기아', img: '/images/test/Maybach.png' },
    { name: '기아', img: '/images/test/Mercedes.png' },
    { name: '기아', img: '/images/test/tesla.png' },
	{ name: '현대', img: '/images/test/Jaguar.png' },
    { name: 'BMW', img: '/images/test/Alfa-Romeo.png' },
    { name: '벤츠', img: '/images/test/Aston_Martin.png' },
    { name: '현대', img: '/images/test/Jaguar.png' },
    { name: '기아', img: '/images/test/Koenigsegg.png' },
    { name: '기아', img: '/images/test/Lagonda.png' },
    { name: '기아', img: '/images/test/Maybach.png' },
    { name: '기아', img: '/images/test/Mercedes.png' },
    { name: '기아', img: '/images/test/tesla.png' },
    { name: 'BMW', img: '/images/test/Alfa-Romeo.png' },
    { name: '벤츠', img: '/images/test/Aston_Martin.png' },
    { name: '현대', img: '/images/test/Jaguar.png' },
    { name: '기아', img: '/images/test/Koenigsegg.png' },
    { name: '기아', img: '/images/test/Lagonda.png' },
    { name: '기아', img: '/images/test/Maybach.png' },
    { name: '기아', img: '/images/test/Mercedes.png' },
    { name: '기아', img: '/images/test/tesla.png' }
   
    
  ],
  bag: [
    { name: '샤넬', img: '/images/test/Celine.png' },
    { name: '루이비통', img: '/images/test/chanel.png' },
    { name: '프라다', img: '/images/test/gucci.png' },
    { name: '프라다', img: '/images/test/hermes.png' },
    { name: '샤넬', img: '/images/test/louisvuitton.png' },
    { name: '샤넬', img: '/images/test/ysl.png' },
    { name: '샤넬', img: '/images/test/Celine.png' },
    { name: '루이비통', img: '/images/test/chanel.png' },
    { name: '프라다', img: '/images/test/gucci.png' },
    { name: '프라다', img: '/images/test/hermes.png' },
    { name: '샤넬', img: '/images/test/louisvuitton.png' },
    { name: '샤넬', img: '/images/test/ysl.png' },
    { name: '샤넬', img: '/images/test/Celine.png' },
    { name: '루이비통', img: '/images/test/chanel.png' },
    { name: '프라다', img: '/images/test/gucci.png' },
    { name: '프라다', img: '/images/test/hermes.png' },
    { name: '샤넬', img: '/images/test/louisvuitton.png' },
    { name: '샤넬', img: '/images/test/ysl.png' }
  
  ]
}; */

/*const sampleProducts = [
  { name: '샘플 상품 A', price: '270,000원~', img: '/images/testbag2.jfif' },
  { name: '샘플 상품 B', price: '308,500원~', img: '/images/testbag3.jfif' },
  { name: '샘플 상품 C', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 D', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 B', price: '308,500원~', img: '/images/testbag3.jfif' },
  { name: '샘플 상품 C', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 D', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 B', price: '308,500원~', img: '/images/testbag3.jfif' },
  { name: '샘플 상품 C', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 D', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 B', price: '308,500원~', img: '/images/testbag3.jfif' },
  { name: '샘플 상품 C', price: '160,600원~', img: '/images/testbag.jfif' },
  { name: '샘플 상품 D', price: '160,600원~', img: '/images/testbag.jfif' }
];*/

const container = document.getElementById("homeProductsData");
const homeProducts  = JSON.parse(container.dataset.homeproducts);
console.log(homeProducts);
const recentProducts = homeProducts.recent;
const themeProducts = homeProducts.theme;
const regionProducts = homeProducts.region;
let selectedFilters = {};

// -------------------------
// 브랜드 슬라이더 로직
// -------------------------
let currentBrandPage = 0;
const brandsPerPage = 8;

// 브랜드 데이터 불러오기
const bData = document.getElementById("brandData");
const bagbrand = JSON.parse(bData.dataset.bagbrands);
const carbrand = JSON.parse(bData.dataset.carbrands);
const brandData = {
    bag : bagbrand.map(brand => ({
        id: brand.id,
        name: brand.name,
        img: `/images/test/` + brand.imagePath,
    })),
    car : carbrand.map(brand => ({
        id: brand.id,
        name: brand.name,
        img: `/images/test/` + brand.imagePath,
    }))
};

// btn 매개변수 추가
function selectCategory(cat, btn = null) {
  const emblemBox = document.getElementById('brandEmblems');
  emblemBox.dataset.category = cat;
  currentBrandPage = 0;
  updateBrandView();

  // 버튼 강조 효과 처리
  if (btn) {
    const allButtons = document.querySelectorAll(".category-btn");
    allButtons.forEach(b => b.classList.remove("active"));
    btn.classList.add("active");
  }
}

function updateBrandView() {
  const emblemBox = document.getElementById('brandEmblems');
  const cat = emblemBox.dataset.category;
  
  // 초기화
  emblemBox.innerHTML = '';
  
  // 현재 페이지에 표시할 브랜드 계산
  const startIndex = currentBrandPage * brandsPerPage;
  const endIndex = startIndex + brandsPerPage;
  const currentPageBrands = brandData[cat].slice(startIndex, endIndex);
  
  // 브랜드 렌더링 및 클릭 이벤트 추가
    currentPageBrands.forEach(b => {
      emblemBox.innerHTML += `
        <div class="brand-item" onclick="selectBrand('${b.name}')">
          <img src="${b.img}" alt="${b.name}">
          <div>${b.name}</div>
        </div>
      `;
    });
}

function selectBrand(brandName) {
  const url = `/search?brand=${encodeURIComponent(brandName)}`;
  window.location.href = url;
}

function scrollBrand(direction, event) {
  // 이벤트 버블링 방지
  if (event) {
    event.stopPropagation();
	event.preventDefault();
  }
  
  const emblemBox = document.getElementById('brandEmblems');
  const cat = emblemBox.dataset.category;
  const totalBrands = brandData[cat].length;
  const maxPage = Math.ceil(totalBrands / brandsPerPage) - 1;
  
  // 페이지 변경
  currentBrandPage += direction;
  
  // 범위 체크
  if (currentBrandPage > maxPage) currentBrandPage = 0;
  if (currentBrandPage < 0) currentBrandPage = maxPage;
  
  // 뷰 업데이트
  updateBrandView();
}

// -------------------------
// 프로모션 슬라이드
// -------------------------
const slides = [
  {
    img: "/images/cherryblossom1.png",
    title: "🌸 봄맞이 JJAM 추가 혜택",
    subtitle: "최대 20% 보너스!",
    link: "/jjam/shop",
    position: "center center"
  },
  {
    img: "/images/car3.jpg",
    title: "👋🏻 처음 인사 드려요!",
    subtitle: "빌리고라고 합니다",
    link: "/search",
    position: "bottom center"
  }
];

let currentSlide = 0;
let slideInterval = null;
let isPlaying = true;
let isFirstRender = true;

const carousel = document.getElementById("carousel");
const progressIndicator = document.getElementById("progressIndicator");
const toggleBtn = document.getElementById("sliderToggleBtn");
const pauseIcon = document.getElementById("pauseIcon");
const playIcon = document.getElementById("playIcon");

function renderSlide(index) {
  const s = slides[index];
  const slide = document.createElement("div");
  slide.classList.add("promotion-card");

  if (!isFirstRender) {
    slide.classList.add("slide-in");
  } else {
    slide.style.transform = "translateX(0)";
  }

  const textClass = index === 1 ? "light-text" : "dark-text";

  slide.innerHTML = `
    <a href="${s.link}" class="promotion-link">
      <div class="promotion-bg">
        <img src="${s.img}" alt="slide" style="object-position: ${s.position || 'center center'};">
      </div>
      <div class="promotion-text ${textClass}">
        <div>${s.title}</div>
        <small>${s.subtitle}</small>
      </div>
    </a>
  `;

  carousel.appendChild(slide);

  if (!isFirstRender && carousel.children.length > 1) {
    const [first] = carousel.children;
    setTimeout(() => {
      carousel.removeChild(first);
    }, 500);
  }

  isFirstRender = false;
  updateProgressBar(index);
}

function updateProgressBar(index) {
  const percentPerSlide = 100 / slides.length;
  progressIndicator.style.width = `${percentPerSlide}%`;
  progressIndicator.style.left = `${percentPerSlide * index}%`;
}

function startSlider() {
  if (slideInterval) return;
  slideInterval = setInterval(() => {
    currentSlide = (currentSlide + 1) % slides.length;
    renderSlide(currentSlide);
  }, 4000);
  isPlaying = true;
  pauseIcon.style.display = "block";
  playIcon.style.display = "none";
}

function stopSlider() {
  clearInterval(slideInterval);
  slideInterval = null;
  isPlaying = false;
  pauseIcon.style.display = "none";
  playIcon.style.display = "block";
}

function toggleSlider() {
  isPlaying ? stopSlider() : startSlider();
}

toggleBtn.onclick = toggleSlider;

// 상단 공지
function startNoticeSlider() {
  const messages = [
    "빌리고에 오신 걸 환영합니다.",
    "봄맞이 JJAM 최대 20% 충전 혜택을 받아보세요!",
	"슈퍼카와 명품백을 합리적인 가격에 이용해 보세요.",
    "궁금하신 점은 오른쪽 하단 빌리 챗을 이용해 보세요.",
    
  ];

  const slideContainer = document.getElementById("noticeSlide");

  // 메시지 DOM 생성
  messages.forEach(msg => {
    const div = document.createElement("div");
    div.className = "notice-text";
    div.textContent = msg;
    slideContainer.appendChild(div);
  });

  // 클론 추가 (첫 번째 메시지를 복제해서 맨 뒤에 붙임)
  const firstClone = slideContainer.children[0].cloneNode(true);
  slideContainer.appendChild(firstClone);

  const messageCount = messages.length;
  let current = 0;
  const itemHeight = 24;
  const intervalTime = 3000;
  let isResetting = false;

  setInterval(() => {
    if (isResetting) return;

    current++;
    slideContainer.style.transition = "transform 0.7s ease-in-out";
    slideContainer.style.transform = `translateY(-${current * itemHeight}px)`;

    // 마지막 클론(복제 메시지)까지 도달했을 때
    if (current === messageCount) {
      isResetting = true;
      setTimeout(() => {
        // 트랜지션 제거하고 바로 첫 번째로 위치 초기화
        slideContainer.style.transition = "none";
        slideContainer.style.transform = `translateY(0)`;
        current = 0;

        // 다음 루프를 위해 살짝 대기
        setTimeout(() => {
          isResetting = false;
        }, 50);
      }, 500); // 트랜지션이 끝나고 나서 리셋
    }
  }, intervalTime);
}
// -------------------------
// 상품 카드 렌더링 + 슬라이더 관리
// -------------------------
const itemsPerSlide = 4;
const sliderStates = {}; // 각 슬라이더 상태 저장

function loadProducts(sectionId, products) {
    const container = document.getElementById(sectionId);
    container.innerHTML = '';

    if (sectionId === 'AllProducts') {
        products.forEach(p => {
            const card = document.createElement('div');
            card.className = 'product-card grid-card';
            const displayFee = Math.round(p.fee * 1.05); // 5% 수수료 추가
            card.innerHTML = `
                <img src="${p.img}" alt="${p.name}">
                <div class="mt-1">
                    <div class="product-name">${p.name}</div>
                    <div class="product-price">${displayFee.toLocaleString()} JJAM</div>
                </div>
            `;
            container.appendChild(card);
        });
    } else {
        const totalSlides = Math.ceil(products.length / itemsPerSlide);

        for (let i = 0; i < totalSlides; i++) {
            const slideWrapper = document.createElement('div');
            slideWrapper.className = 'slide-wrapper';

            const startIdx = i * itemsPerSlide;
            const endIdx = Math.min(startIdx + itemsPerSlide, products.length);

            for (let j = startIdx; j < endIdx; j++) {
                const p = products[j];
                const card = document.createElement('div');
                card.className = 'product-card';
                const displayFee = Math.round(p.fee * 1.05); // 5% 수수료 추가
                let html = '';
                switch (p.rentalCategoryId) {
                    case 1:
                        html += `<a href="/post/details/bag?id=${p.id}">`;
                        break;
                    case 2:
                        html += `<a href="/post/details/car?id=${p.id}">`;
                        break;
                }
                html += `
                    <img src="/images/rentals/${p.filePath}" alt="${p.postName}"></a>
                    <div class="mt-1">
                        <div class="product-name">${p.postName}</div>
                        <div class="product-price">${displayFee.toLocaleString()} JJAM</div>
                    </div>
                `;
                card.innerHTML = html;
                slideWrapper.appendChild(card);
            }

            container.appendChild(slideWrapper);
        }

        initializeSlider(sectionId);
    }
}

function initializeSlider(sectionId) {
  const container = document.getElementById(sectionId);
  const slideWrappers = container.querySelectorAll('.slide-wrapper');
  const slideCount = slideWrappers.length;

  sliderStates[sectionId] = { currentIndex: 0, totalPages: slideCount };

  // 슬라이드 컨테이너의 width를 100%로 설정
  container.style.width = '100%';

  // 각 슬라이드의 flex 설정
  slideWrappers.forEach(wrapper => {
    // 각 슬라이드의 너비를 flex로 처리
    wrapper.style.flex = '0 0 100%'; // 슬라이드의 너비를 100%로 설정
  });

  // 슬라이드 컨테이너에서 transform 초기화
  container.style.transform = 'translateX(0)';
}

function scrollProducts(sectionId, direction) {
  const container = document.getElementById(sectionId);
  const state = sliderStates[sectionId];
  if (!state) return;

  state.currentIndex = Math.max(0, Math.min(state.totalPages - 1, state.currentIndex + direction));
  container.style.transform = `translateX(-${state.currentIndex * 100}%)`;
}

/* function loadMoreAllProducts() {
  const container = document.getElementById("AllProducts");
  const limitedClass = container.classList.contains("limited");
  
  // 현재 'limited' 클래스가 있다면 3줄 추가
  if (limitedClass) {
    container.classList.remove("limited");
    
    // 'limited' 클래스를 제거하고 더 많은 상품을 표시
    const productItems = container.querySelectorAll('.product-card');
    const totalItems = sampleProducts.length;
    const itemsPerLine = 4; // 한 줄에 4개씩
    const currentRows = Math.ceil(productItems.length / itemsPerLine); // 현재까지 표시된 줄 수
    
    // 3줄씩 추가
    const startIdx = currentRows * 3 * itemsPerLine; // 3줄 * 4개 상품씩
    const endIdx = Math.min(startIdx + 3 * itemsPerLine, totalItems); // 상품 리스트 끝까지

    // 상품 카드 추가
    for (let i = startIdx; i < endIdx; i++) {
      const p = sampleProducts[i];
      const card = document.createElement('div');
      card.className = 'product-card';
      card.innerHTML = `<img src="${p.img}" alt="${p.name}"><div class="mt-1"><div class="product-name">${p.name}</div><div class="product-price">${p.price}</div></div>`;
      container.appendChild(card);
    }
    
    // 더 이상 추가할 상품이 없다면 버튼 숨기기
    if (endIdx === totalItems) {
      document.querySelector(".more-btn-wrap").style.display = "none";
    }
  }
} */

function goSearchPage(brandId) {
    window.location.href = "http://localhost:8080/search?brandId=" + brandId;
}




// -------------------------
// 초기 실행 + 전역 등록
// -------------------------
selectCategory('car');
loadProducts('recommendProducts', themeProducts);
loadProducts('topLocalProducts', regionProducts);
loadProducts('newProducts', recentProducts);
//loadProducts('AllProducts', recentProducts);
renderSlide(currentSlide);
startSlider();
window.scrollBrand = scrollBrand;
window.selectCategory = selectCategory;
startNoticeSlider();
window.scrollProducts = scrollProducts;
// window.loadMoreAllProducts = loadMoreAllProducts;