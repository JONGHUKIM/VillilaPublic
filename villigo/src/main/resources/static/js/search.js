document.addEventListener('DOMContentLoaded', () => {
    const searchInput = document.getElementById('searchInput');
    const searchBtn = document.getElementById('searchBtn');
    const selectedFiltersContainer = document.getElementById('selectedFilters');
    const filterButtons = document.querySelectorAll('.brand-btn, .color-circle, .location-btn, .price-btn');
    const categoryButtons = document.querySelectorAll('.category-btn');
    const mapButtonSection = document.getElementById('mapButtonSection');
    const priceSearchBtn = document.getElementById('priceSearchBtn');
    const searchResultDiv = document.getElementById('searchResultDiv');
    const urlParams = new URLSearchParams(window.location.search);
    const brand = urlParams.get('brand');

    let selectedFilters = {};
    let latestSearchResults = [];
    let currentPage = 0; // 현재 페이지 번호
    let isLoading = false; // 데이터 로드 중 여부 플래그
    let hasMoreData = true; // 추가 데이터가 있는지 여부

    // 유저 닉네임인지 판별하는 함수
    function isUserNickname(query) {
        const nicknamePattern = /^[a-zA-Z0-9가-힣]{2,20}$/;
        const hasFilter = Object.keys(selectedFilters).length > 0;
        return nicknamePattern.test(query) && !hasFilter;
    }

    // 선택된 필터 표시 업데이트 함수
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
                    resetAndSearch();
                }
                
                checkCategoryButtonsCleared();
            });
        });
		
		updateCategorySelectionUI(); // 카테고리 강조 상태 복구
    }

    // 검색 초기화 및 새로 검색
    function resetAndSearch() {
		console.log('📍 resetAndSearch 실행됨');
		window.scrollTo(0, 0);
        currentPage = 0;
		isLoading = false;
        hasMoreData = true;
        searchResultDiv.innerHTML = ''; // 결과 초기화
        latestSearchResults = []; // 결과 데이터 초기화
        performSearch();
    }

    // 검색 실행 함수 (무한 스크롤용)
    const performSearch = () => {
        if (isLoading || !hasMoreData) return; // 로드 중이거나 더 이상 데이터가 없으면 중단

        isLoading = true; // 로드 시작
        const query = searchInput.value.trim();

        const filterMap = {}; // 빈 객체로 시작하여 filterMap을 새로 구성합니다.
		
		// selectedFilters 객체를 직접 사용하여 filterMap 구성
		// selectedFilters는 모든 선택된 필터 정보를 담고 있는 JavaScript 객체입니다.
		for (const type in selectedFilters) {
		    // selectedFilters의 속성(키)이 자신(own property)의 것인지 확인합니다.
		    if (selectedFilters.hasOwnProperty(type)) {
		        // 각 필터 타입(예: 'rentalCategory', 'brand', 'price')에 해당하는 배열의 'source' 값들을 모아 새로운 배열로 만듭니다.
		        filterMap[type] = selectedFilters[type].map(item => item.source);
		    }
		}

        filterMap.keyword = [searchInput.value.trim()];
        filterMap.page = [currentPage];
        filterMap.size = [6]; // 한 페이지당 6개씩
        console.log("필터 맵:", filterMap);

		axios.post('/api/search', filterMap)
		    .then((response) => {
		        console.log("✅ 응답:", response.data);
		        console.log("📦 content:", response.data.content);
		        let html = '';
		        if (response.data.totalElements === 0 && currentPage === 0) {
		            html = '<span>검색된 결과가 없습니다!</span>';
		            if (mapButtonSection) {
		                mapButtonSection.classList.add('hidden');
		            }
		            hasMoreData = false;
		        } else {
		            if (mapButtonSection) {
		                mapButtonSection.classList.remove('hidden');
		            }
		            if (response.data.content.length === 0) {
		                hasMoreData = false;
		                return;
		            }
		            response.data.content.forEach(product => {
		                const displayFee = Math.round(product.fee * 1.05); // 5% 수수료 추가
		                html += `
		                    <div class="result-item">
		                `;
		                switch (product.rentalCategoryId) {
		                    case 1:
		                        html += `
		                        <a href="/post/details/bag?id=${product.id}">
		                        `;
		                        break;
		                    case 2:
		                        html += `
		                        <a href="/post/details/car?id=${product.id}">
		                        `;
		                        break;
		                }
		                html += `
		                            <img src="${product.filePath}" alt="상품 이미지"> <!-- S3 URL 사용 -->
		                        </a>
		                `;
		                switch (product.rentalCategoryId) {
		                    case 1:
		                        html += `
		                        <a class="product-content" href="/post/details/bag?id=${product.id}">
		                        `;
		                        break;
		                    case 2:
		                        html += `
		                        <a class="product-content" href="/post/details/car?id=${product.id}">
		                        `;
		                        break;
		                }
		                html += `
		                            <p><strong>${product.postName}</strong></p>
		                            <p><strong>${displayFee} 원</strong></p>
		                        </a>
		                    </div>
		                `;
		            });
		            latestSearchResults = latestSearchResults.concat(response.data.content);
		        }
		        searchResultDiv.insertAdjacentHTML('beforeend', html);
		        isLoading = false;
		        currentPage++;
		    })
		    .catch((error) => {
		        console.error("요청 실패:", error);
		        if (error.response) {
		            console.log("서버 응답:", error.response.data);
		        }
		        isLoading = false;
		    });
    };

    // 검색 버튼 / 엔터 키 이벤트
    searchBtn.addEventListener('click', (e) => {
        resetAndSearch();
    });
    searchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') {
            resetAndSearch();
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

        const value = `${min}원 ~ ${max}원`;
        selectedFilters['price'] = [{ value: value, source: `${min},${max}`}];

        document.querySelectorAll('.price-btn').forEach(b => b.classList.remove('selected'));
        updateSelectedFilters();
        resetAndSearch();
    });

    // 필터 버튼 클릭 이벤트
    filterButtons.forEach(button => {
        button.addEventListener('click', () => handleFilterButtonClick(button));
    });
    
    function handleFilterButtonClick(button) {
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
                resetAndSearch();
                return;
            }
    
            // 새로 선택
            button.classList.add('selected');
            selectedFilters['price'] = [{ value: filterValue, source: filterSource }];
            updateSelectedFilters();
            resetAndSearch();
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
            selectedFilters[filterType].push({ value: filterValue, source: filterSource });
            button.classList.add('selected');
        }
        updateSelectedFilters();
        resetAndSearch();
    }
    
    // 카테고리 버튼 누를시 전체 해제 기능 (라디오 버튼 비슷하게)
	categoryButtons.forEach(btn => {
	    btn.addEventListener('click', function () {
	        const filterType = this.dataset.filter;
	        const filterValue = this.dataset.value;
	        const filterSource = this.dataset.source;
	        const isSelected = this.classList.contains('selected');

	        console.log('카테고리 버튼 클릭, filterValue:', filterValue, 'filterSource:', filterSource, 'isSelected:', isSelected);

	        // 이미 선택된 버튼을 클릭한 경우 해제
	        if (isSelected) {
	            this.classList.remove('selected');
	            delete selectedFilters[filterType];
	            updateSelectedFilters();
	            makeBrandColumn(99); // 모든 브랜드 표시 (CUSTOM 포함)
	            resetAndSearch();
	            return;
	        }

	        // 다른 카테고리 버튼 선택 해제
	        document.querySelectorAll('.category-btn').forEach(b => {
	            if (b !== this) b.classList.remove('selected');
	        });
	        this.classList.add('selected');

	        // 다른 필터(브랜드, 컬러, 가격, 지역) 초기화
	        ['brand', 'color', 'price', 'location'].forEach(filter => {
	            if (selectedFilters[filter]) {
	                delete selectedFilters[filter];
	                document.querySelectorAll(`[data-filter="${filter}"]`).forEach(b => b.classList.remove('selected'));
	            }
	        });

	        // 선택된 카테고리 필터 업데이트
	        selectedFilters[filterType] = [{ value: filterValue, source: filterSource }];
	        updateSelectedFilters();
	        makeBrandColumn(filterSource);
	        resetAndSearch();
	    }, { once: false }); // 이벤트 중복 방지
	});
    
    // 모든 필터 초기화
	function refreshFilters() {
	    document.querySelectorAll('.filter-btn').forEach(b => b.classList.remove('selected'));
	    selectedFilters = {};
	    updateSelectedFilters();
	    makeBrandColumn(99); // 모든 브랜드 표시
	    resetAndSearch();
	}
    
	function nonRefreshCategoryFilters() {
	    document.querySelectorAll('.filter-btn').forEach(b => {
	        if (!b.classList.contains('category-btn')) {
	            b.classList.remove('selected');
	        }
	    });
	    ['brand', 'color', 'price', 'location'].forEach(filter => {
	        if (selectedFilters[filter]) {
	            delete selectedFilters[filter];
	        }
	    });
	    updateSelectedFilters();
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
	
	// 현재 선택된 카테고리를 버튼에 다시 반영
	function updateCategorySelectionUI() {
	    const categoryFilter = selectedFilters['rentalCategory'];
	    if (!categoryFilter || categoryFilter.length === 0) return;

	    const selectedValue = categoryFilter[0].value;

	    document.querySelectorAll('.category-btn').forEach(btn => {
	        if (btn.dataset.value === selectedValue) {
	            btn.classList.add('selected');
	        } else {
	            btn.classList.remove('selected');
	        }
	    });
	}

	function makeBrandColumn(categoryId) {
	    console.log('makeBrandColumn 호출, categoryId:', categoryId);
	    axios.post('/api/brand', { rentalCategoryId: categoryId }, {
	        headers: { 'Content-Type': 'application/json' }
	    })
	    .then((response) => {
	        console.log('브랜드 응답:', response.data);
	        const brands = response.data;
	        const brandDiv = document.getElementById('brandDiv');
	        let html = '';

	        if (brands.length === 0) {
	            html = '<span>해당 카테고리에 브랜드가 없습니다.</span>';
	        } else {
	            for (let brand of brands) {
	                html += `
	                    <button class="filter-btn brand-btn" 
	                            data-filter="brand" 
	                            data-value="${brand.name}" 
	                            data-source="${brand.id}">${brand.name}</button>
	                `;
	            }
	        }

	        brandDiv.innerHTML = html;

	        // 새로운 브랜드 버튼에 이벤트 리스너 추가
	        document.querySelectorAll('#brandDiv .filter-btn').forEach(btn => {
	            btn.addEventListener('click', () => handleFilterButtonClick(btn));
	        });

	        // 기존 브랜드 필터 초기화
			if (selectedFilters['brand']) {
			    const currentSelectedBrands = selectedFilters['brand'].map(b => b.value);
			    updateSelectedFilters();

			    document.querySelectorAll('#brandDiv .filter-btn').forEach(btn => {
			        if (currentSelectedBrands.includes(btn.dataset.value)) {
			            btn.classList.add('selected');
			        }
			    });
			}
	    })
	    .catch((error) => {
	        console.error('브랜드 로드 실패:', error);
	        document.getElementById('brandDiv').innerHTML = '<span>브랜드를 불러오는데 실패했습니다.</span>';
	    });
	}


	// 지도 보기 추후 업데이트 예정
	/*
    let map = null;
    let mapInitialized = false;
    // 초기 지도 생성
    document.getElementById("toggleMapBtn").addEventListener("click", () => {
        const mapElement = document.getElementById("map");
        mapElement.classList.toggle("open");
        
        if (mapElement.classList.contains("open")) {
            // 지도가 열린 상태에서만 초기화 및 마커 설정
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
    });

    let markers = [];

    function makeMarkerMap(contents) {
        if (!map || !contents || contents.length === 0) return;

        // 기존 마커 제거
        markers.forEach(marker => {
            marker.setMap(null);
        });
        markers = []; // 마커 배열 초기화

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
                switch (item.rentalCategoryId) {
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

            markers.push(marker); // 새 마커를 배열에 저장
            bounds.extend(latlng);
        });

        map.setBounds(bounds);
    }
	*/
	
	const category = urlParams.get('category');

	if (category) {
	    // 해당 버튼 찾아서 source값 가져오기
	    const btn = document.querySelector(`.category-btn[data-value="${category}"]`);
	    const source = btn ? btn.dataset.source : category;

	    selectedFilters['rentalCategory'] = [{ value: category, source }];
	    updateSelectedFilters();
	    makeBrandColumn(source);  // 이 부분이 중요
	    resetAndSearch();
	}
	
    if (brand) {
        const tryClickBrand = () => {
            const brandBtn = document.querySelector(`[data-filter="brand"][data-value="${brand}"]`);
            if (brandBtn) {
                brandBtn.classList.add("selected");
                selectedFilters["brand"] = [{ value: brand, source: brandBtn.dataset.source }];
                updateSelectedFilters();
                resetAndSearch();
            } else {
                setTimeout(tryClickBrand, 200);
            }
        };
        tryClickBrand();
    }

    // 스크롤 이벤트 감지
    window.addEventListener('scroll', () => {
        if ((window.innerHeight + window.scrollY) >= document.body.offsetHeight - 100 && !isLoading) {
            performSearch();
        }
    });

    // 초기화 버튼
	document.getElementById("refresh-btn").addEventListener("click", function () {
	    searchInput.value = '';
	    refreshFilters(); // 내부에서 resetAndSearch까지 다 처리하므로 여기서 끝내야 함
	});

    // 초기 검색 실행
    performSearch();
    makeBrandColumn(99); // 초기 브랜드 그려주기
});