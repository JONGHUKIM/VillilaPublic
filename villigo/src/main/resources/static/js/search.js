document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const selectedFiltersContainer = document.getElementById('selectedFilters');
    const filterButtons = document.querySelectorAll('.category-btn .brand-btn, .color-circle, .location-btn, .price-btn');
    const categoryButtons = document.querySelectorAll('.category-btn');
    const mapButtonSection = document.getElementById('mapButtonSection');
    const priceSearchBtn = document.getElementById('priceSearchBtn');
    const searchResultDiv = document.getElementById('searchResultDiv');
    const paginationDiv = document.getElementById('paginationDiv');
    const urlParams = new URLSearchParams(window.location.search);
    const brand = urlParams.get('brand');

    let selectedFilters = {};
    let latestSearchResults = [];

    // 유저 닉네임인지 판별하는 함수
    function isUserNickname(query) {
        const nicknamePattern = /^[a-zA-Z0-9가-힣]{2,20}$/;
        const hasFilter = Object.keys(selectedFilters).length > 0;
        return nicknamePattern.test(query) && !hasFilter;
    }

    //  선택된 필터 표시 업데이트 함수
    function updateSelectedFilters() {
        selectedFiltersContainer.innerHTML = '';

        for (const [type, filterArrays] of Object.entries(selectedFilters)) {
            filterArrays.forEach(filterArray => {
                const filterTag = document.createElement('div');
                filterTag.classList.add('selected-filter');
                filterTag.innerHTML = `
                    <span>${filterArray.value}</span>
                    <button data-type="${type}" data-value="${filterArray.value}" data-source="${filterArray.source}">X</button>
                `;
                selectedFiltersContainer.appendChild(filterTag);
            });
        }

        // 삭제 버튼 동작
        document.querySelectorAll('.selected-filter button').forEach(btn => {
            btn.addEventListener('click', () => {
                const type = btn.dataset.type;
                const value = btn.dataset.value;

                const index = selectedFilters[type].findIndex(item => item.value === value);
                if (index > -1) {
                    selectedFilters[type].splice(index, 1);
                    if (selectedFilters[type].length === 0) {
                        delete selectedFilters[type];
                    }
                    document.querySelector(`[data-filter="${type}"][data-value="${value}"]`)?.classList.remove('selected');
                    updateSelectedFilters();
                    performSearch();
                }
                
                checkCategoryButtonsCleared();
            });
        });

    }

    // 검색 실행 함수
    const performSearch = (page) => {
        const query = searchInput.value.trim();
        const isUserSearch = isUserNickname(query);
        const hasFilter = Object.keys(selectedFilters).length > 0;

        // 검색 실행 구현!
        let selectFilters = document.querySelectorAll('.selected-filter');

        const filterMap = Array.from(selectFilters).reduce((result, filter) => {
            const button = filter.querySelector('button');
            const type = button.dataset.type;
            const source = button.dataset.source;

            if (!result[type]) {
                result[type] = [];
              }
              result[type].push(source);
              return result;
            }, {});
        const priceMin = document.getElementById("priceMin").value.trim();
        const priceMax = document.getElementById("priceMax").value.trim();

        filterMap.keyword = [searchInput.value.trim()];
        filterMap.page = [page];
        console.log(filterMap);

        axios.post(`/api/search`, filterMap )
            .then((response) => {
                console.log("✅ 응답:", response.data);
                console.log("📦 content:", response.data.content);
                let html = '';
                searchResultDiv.innerHTML = html;
                paginationDiv.innerHTML = html;
                if(response.data.totalElements === 0) {
                    html = `<span>검색된 결과가 없습니다!</span>`;
                    mapButtonSection.classList.add('hidden');
                } else {
                    mapButtonSection.classList.remove('hidden');
                    response.data.content.forEach(product => {
						const displayFee = Math.round(product.fee * 1.05); // 5% 수수료 추가
                        html += `
                            <div class="result-item">`
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
                                    <img src="/images/rentals/${product.filePath}" alt="상품 이미지">
                                </a>`
                        switch(product.rentalCategoryId) {
                            case 1:
                                html += `
                                <a class="product-content" href="/post/details/bag?id=${product.id}">`
                                break;
                            case 2:
                                html += `
                                <a class="product-content" href="/post/details/car?id=${product.id}">`
                                break;
                        }
                        html += `
                                    <p><Strong>${product.postName}</Strong></p>
                                    <p><Strong>${product.fee} JJAM</Strong></p>
                                </a>
                            </div>
                        `;
                    });
                }
                searchResultDiv.innerHTML = html;
                makePagination(response.data);
                latestSearchResults = response.data.content;
            })
            .catch((error) => {
                console.error("❌ 요청 실패:", error);
                if (error.response) {
                    console.log("💥 서버 응답:", error.response.data);
                }
            });
    };

    // 검색 버튼 / 엔터 키 이벤트
    searchBtn.addEventListener('click', (e) => {
        performSearch();
    });
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            performSearch();
        }
    });

    // 가격 validation
    priceSearchBtn.addEventListener('click', () => {
        const minInput = document.getElementById('priceMin');
        const maxInput = document.getElementById('priceMax');

        const min = parseInt(minInput.value, 10);
        const max = parseInt(maxInput.value, 10);

        // 유효성 검사
        if (isNaN(min) || isNaN(max)) {
            alert('숫자를 입력해주세요.');
            return;
        }

        if (min < 0 || max < 0) {
            alert('0원 이상의 금액만 입력 가능합니다.');
            return;
        }

        if (min > max) {
            alert('최소 금액이 최대 금액보다 클 수 없습니다.');
            return;
        }

        document.getElementById('priceMin').value = '';
        document.getElementById('priceMax').value = '';

        const value = `${min}쨈 ~ ${max}쩸`;
        selectedFilters['price'] = [{ value: value, source: `${min},${max}`}];

        document.querySelectorAll('.price-btn').forEach(b => b.classList.remove('selected'));
        updateSelectedFilters();
        performSearch();
    });

    // 필터 버튼 클릭 이벤트
    filterButtons.forEach(button => {
        button.addEventListener('click', () => handleFilterButtonClick(button));
    });
    
    function handleFilterButtonClick (button) {
        const filterType = button.dataset.filter;
        const filterValue = button.dataset.value;
        const filterSource = button.dataset.source;

        if (!selectedFilters[filterType]) {
            selectedFilters[filterType] = [];
        }

        if (button.classList.contains('price-btn')) {
            const isSelected = button.classList.contains('selected');
    
            // 모두 초기화
            document.querySelectorAll('.price-btn').forEach(b => b.classList.remove('selected'));
            delete selectedFilters['price'];
            document.getElementById('priceMin').value = '';
            document.getElementById('priceMax').value = '';
    
            if (isSelected) {
                // 이미 선택된 버튼을 다시 누르면 해제만 하고 끝
                updateSelectedFilters();
                performSearch();
                return;
            }
    
            // 새로 선택
            button.classList.add('selected');
            selectedFilters['price'] = [{ value: filterValue, source: filterSource }];
            updateSelectedFilters();
            performSearch();
            return;
        }

        const index = selectedFilters[filterType].findIndex(item => item.value === filterValue);

        if (index > -1) {
            selectedFilters[filterType].splice(index, 1);
            button.classList.remove('selected');
            if (selectedFilters[filterType].length === 0) {
                delete selectedFilters[filterType];
            }
        } else {
            if (filterType === 'price') {
                selectedFilters[filterType] = [];
                document.querySelectorAll(`[data-filter="${filterType}"]`).forEach(b => b.classList.remove('selected'));
            }
            selectedFilters[filterType].push({ value : filterValue, source : filterSource });
            button.classList.add('selected');
        }
        updateSelectedFilters();
        performSearch();
    }
    
    // 카테고리 버튼 누를시 전체 해제 기능 (라디오 버튼 비슷하게)
    categoryButtons.forEach( btn => {
        btn.addEventListener('click', function () {
            const isSelected = btn.classList.contains("selected");
            const filterType = this.dataset.filter;  // 필터 타입 가져오기
            const filterValue = this.dataset.value;  // 필터 값 가져오기
            const filterSource = this.dataset.source;  // 소스 값 가져오기
            console.log(isSelected , filterType , filterValue);
            
            const currentSelected = document.querySelector('.category-btn.selected');
            const isSwitchingCategory = currentSelected && currentSelected !== btn;
            
            if (isSelected) {
                delete selectedFilters[filterType];
                btn.classList.remove("selected");
                checkCategoryButtonsCleared();
                updateSelectedFilters();
                performSearch();
                return;
            }
            
            if (isSwitchingCategory) {
                // 다른 카테고리 버튼이 선택된 상태에서 새로운 버튼 클릭
                const oldCategoryValue = currentSelected.dataset.value;
                const oldCategorySource = currentSelected.dataset.source;
                console.log(`카테고리 전환: ${oldCategoryValue} -> ${filterValue}`);
                currentSelected.classList.remove("selected"); // 기존 선택 해제
                delete selectedFilters[filterType]; // 기존 필터 제거
            }
            
            ['brand', 'color', 'price', 'location'].forEach(filter => {
                if (selectedFilters[filter]) {
                    delete selectedFilters[filter];
                    document.querySelectorAll(`[data-filter="${filter}"]`).forEach(b => b.classList.remove('selected'));
                }
            });

            nonRefresgCatrgoryFilters();

            console.log("categoryID", filterSource);
            makeBrandColumn(filterSource);
            
            btn.classList.add("selected");
            selectedFilters[filterType] = [{ value: filterValue, source: filterSource }];
            updateSelectedFilters();
            performSearch();
            

        });
    });
    
    // 모든 필터 초기화
    function refreshFilters() {

        document.querySelectorAll(".category-btn").forEach( b => {
            b.classList.remove("selected");
        });
        document.querySelectorAll(".filter-btn").forEach( b => {
            b.classList.remove("selected");
        });
        document.querySelectorAll("#selectedFilters button").forEach( b => {
            if(b.dataset.type !== "category") b.click();
        });

    }
    
    function nonRefresgCatrgoryFilters() {

        document.querySelectorAll(".filter-btn").forEach( b => {
            // 카테고리 버튼은 제외
            if (!b.classList.contains("category-btn")) {
                b.classList.remove("selected");
            }
        });
        document.querySelectorAll("#selectedFilters button").forEach(b => {
            if(b.dataset.type !== "category") b.click();
        });
    }

    function checkCategoryButtonsCleared() {
        const anySelected = Array.from(document.querySelectorAll('.category-btn'))
            .some(btn => btn.classList.contains('selected'));
    
        if (!anySelected) {
            console.log("모든 카테고리 버튼이 해제");
            
            if (!selectedFilters['category']) {
                makeBrandColumn(99);
            }
        }
    }

    function makeBrandColumn(categoryId) {
        axios.post(`/api/brand`, { rentalCategoryId : categoryId }, {
            headers: { 
                'Content-Type' : 'application/json' 
            }
        })
        .then((response) => {
            const brands = response.data;
            console.log(brands);
            const brandDiv = document.getElementById("brandDiv");
            let html = ''
            brandDiv.innerHTML = html;
            
            for(let brand of brands) {
                html += `
                    <button class="filter-btn category-btn" data-filter="brand" data-value="${brand.name}" 
                    data-source="${brand.id}">${brand.name}</button>
                `
            }
            brandDiv.innerHTML = html;
            
            document.querySelectorAll('#brandDiv .filter-btn').forEach(btn => {
                btn.removeEventListener('click', handleFilterButtonClick)
                btn.addEventListener('click', () => handleFilterButtonClick(btn));
            });
        })
            .catch((error) => console.log(error));
            
            const params = new URLSearchParams(window.location.search);
            const brandId = String(params.get("brandId"));
            if (brandId) {
              // data-source 속성이 전달된 brandId와 일치하는 버튼 찾기
              const targetBtn = document.querySelector(`.category-btn[data-source="${brandId}"]`);
              console.log(targetBtn)
              if (targetBtn) {
                // 버튼 클릭 이벤트 발생
                targetBtn.click();
              }
            }
        }


    let map = null;
    let mapInitialized = false;
    // 초기 지도 생성
    document.getElementById("toggleMapBtn").addEventListener("click", () => {
        const mapElement = document.getElementById("map");
        mapElement.classList.toggle("open");
        
        if (mapElement.classList.contains("open")) {
            paginationDiv.style.display = "none";
        

            // 지도가 열린 상태에서만 초기화 및 마커 설정
            if (mapElement.classList.contains("open")) {
                if (!mapInitialized) {
                    const mapContainer = document.getElementById('map-box');
                    const mapOption = {
                        center: new kakao.maps.LatLng(37.5, 126.9), // 기본 중심
                        level: 3
                    };
                    map = new kakao.maps.Map(mapContainer, mapOption);
                    mapInitialized = true;
                }
    
                // 지도 크기 갱신 후 마커 표시
                setTimeout(() => {
                    map.relayout(); // 지도 크기 재계산
                    makeMarkerMap(latestSearchResults);
                }, 500); // DOM 업데이트 후 실행 보장
            } 
        }
        else {
            paginationDiv.style.display = "block";
        }
    });

    let markers = [];

    function makeMarkerMap(contents) {
        if (!map || !contents || contents.length === 0) return;

        // 기존 마커 제거 (수정 추가)
        markers.forEach(marker => {
            marker.setMap(null);
        });
        markers = []; // 마커 배열 초기화 (수정 추가)

        const bounds = new kakao.maps.LatLngBounds();

        contents.forEach(item => {
            console.log(item);
            let baseUrl = 'http://192.168.14.20:8080/post/details/';
            let latlng = new kakao.maps.LatLng(parseFloat(item.latitude), parseFloat(item.longitude));
            let imgSize = new kakao.maps.Size(50, 50);
            let imgSource = `/images/rentals/${item.filePath}`;
            let markerImg = new kakao.maps.MarkerImage(imgSource, imgSize);
            let marker = new kakao.maps.Marker({
                position: latlng,
                clickable: true,
                image: markerImg
            });
            kakao.maps.event.addListener(marker, 'click', function() {
                let url;
                switch(item.rentalCategoryId) {
                    case 1:
                        url = baseUrl + `bag?id=${item.id}`;
                        break;
                    case 2:
                        url = baseUrl + `car?id=${item.id}`;
                        break;
                }
                window.open(url, '_blank');
            });
            marker.setMap(map);

            markers.push(marker); // 새 마커를 배열에 저장 (수정 추가)
            bounds.extend(latlng);
        });

        map.setBounds(bounds);
    }



    if (brand) {
        const tryClickBrand = () => {
            const brandBtn = document.querySelector(`[data-filter="brand"][data-value="${brand}"]`);
            if (brandBtn) {
                brandBtn.classList.add("selected");
                selectedFilters["brand"] = [{ value: brand, source: brandBtn.dataset.source }];
                updateSelectedFilters();
                performSearch();
            } else {
                setTimeout(tryClickBrand, 200);
            }
        };
        tryClickBrand();
    }


    function makePagination(data) {
        console.log("makePagination", data);
        const basUrl = `/api/search`;
        const startPage = Math.max(0, data.number - 2);
        const endPage = Math.min(data.totalPages - 1, data.number + 2);
        console.log("startPage:", startPage, "endPage:", endPage, "current:", data.number, "totalPages:", data.totalPages);
        let html = '';
        paginationDiv.innerHTML = html;
        html += `
            <nav aria-label="Page navigation">
                <ul class="pagination justify-content-center">
                    <li class="page-item " id="prevBtn" data-page="${data.number - 1}">
                        <a class="page-link">&lt;</a>
                    </li>
        `;
        for (let i = startPage; i <= endPage; i++) {
            if (i === data.number) {
                html += `<li class="page-item active" data-page="${i}"><a class="page-link">${i + 1}</a></li>`;
            } else {
                html += `<li class="page-item" data-page="${i}"><a class="page-link">${i + 1}</a></li>`;
            }
        }
        html += `
                    <li class="page-item" id="nextBtn" data-page="${data.number + 1}">
                        <a class="page-link">&gt;</a>
                    </li>
                </ul>
            </nav>
        `;

        paginationDiv.innerHTML = html;

        const prevBtn = document.getElementById("prevBtn");
        const nextBtn = document.getElementById("nextBtn");
        if(data.first) {
            prevBtn.classList.add("disabled");
        } 
        if(data.last) {
            nextBtn.classList.add("disabled");
        }

        document.querySelectorAll('.page-item').forEach(item => {
            item.addEventListener('click', () => {
                if (item.classList.contains('disabled') || item.classList.contains('active')) return;
                const page = item.dataset.page;
                console.log("page:", page);
                performSearch(page); // 👈 이 함수가 페이지 요청 수행
            });
        });
    }

    // 초기화 버튼
    document.getElementById("refresh-btn").addEventListener("click", function () {
        searchInput.value = '';
        refreshFilters();
        selectedFilters = {};
        updateSelectedFilters();
        performSearch();
    });

    
    performSearch(); // 처음 들어오자마자 한번실행 (초기 리스트 보기)
    makeBrandColumn(99); // 초기 브랜드 그려주기
});
