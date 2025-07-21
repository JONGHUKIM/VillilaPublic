package com.splusz.villigo.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Color;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.dto.BrandReadDto;
import com.splusz.villigo.dto.PostSummaryDto;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.dto.ReservationDto;
import com.splusz.villigo.dto.SearchedProductDto;
import com.splusz.villigo.dto.SelectBrandsByCategoryDto;
import com.splusz.villigo.repository.AddressRepository;
import com.splusz.villigo.repository.BrandRepository;
import com.splusz.villigo.repository.ColorRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalCategoryRepository;
import com.splusz.villigo.repository.RentalImageRepository;
import com.splusz.villigo.storage.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository prodRepo;
    private final BrandRepository brandRepo;
    private final ColorRepository colorRepo;
    private final RentalCategoryRepository rentalCateRepo;
    private final RentalImageRepository rentalImgRepo;
    private final AddressRepository addrRepo;
    private final ReservationService reservationService;
    private final S3FileStorageService s3FileStorageService;

    public List<RentalCategory> readRentalCategories() {
        List<RentalCategory> reatalCategories = rentalCateRepo.findAll();
        return reatalCategories;
    }
    
    // 전체 브랜드 dto로 가져오기
    public List<BrandReadDto> readBrandDto(Long rentalCategoryId) {
    	List<BrandReadDto> brandDto = readBrands(rentalCategoryId).stream()
    			.map(BrandReadDto :: fromEntity)
    			.collect(Collectors.toList());
    	return brandDto;
    }

    // 전체 브랜드 가져오기
    public List<Brand> readAllBrands() {
        List<Brand> brands = brandRepo.findAll();
        return brands;
    }

    // 브랜드 가져오기
    public List<Brand> readBrands(Long rentalCategoryId) {
        List<Brand> brands = brandRepo.findByRentalCategoryId(rentalCategoryId);
        return brands;
    }
    
    public Page<Brand> readBrandsPaging(Integer pageNum, String category) {
    	Long rentalCategoryId = 1L;
    	switch(category) {
	    case "bag" :
	    	rentalCategoryId = 1L;
	    case "car" :
	    	rentalCategoryId = 2L;
	    }
        List<Brand> brands = brandRepo.findByRentalCategoryId(rentalCategoryId);
        Pageable pageable = PageRequest.of(pageNum, 8);
        Page<Brand> pagingbrands = listToPage(brands, pageable);
        
        return pagingbrands;
    }
    
    // SelectBrandsByCategoryDto 가져오기
    public List<SelectBrandsByCategoryDto> readSelectBrandsByCategory(Long rentalCategoryId) {
        List<Brand> brands = new ArrayList<>();
        if(rentalCategoryId == 99L) {
            brands = brandRepo.findAll();
            brands.removeIf(brand -> brand.getId() == 1L);
        } else {
            brands = brandRepo.findByRentalCategoryId(rentalCategoryId);
        }
    	List<SelectBrandsByCategoryDto> brandsDto = brands.stream()
    			.map(SelectBrandsByCategoryDto :: new)
    			.collect(Collectors.toList());
    	return brandsDto;
    }

    // 전체 색상 가져오기(카테고리 별 중복제거거)
    public List<Color> readAllColors() {
        List<Color> colors = colorRepo.findDistinctByColorNumber();
        return colors;
    }

    // 가방 색상 가져오기
    public List<Color> readColors(Long rentalCategoryId) {
        List<Color> colors = colorRepo.findByRentalCategoryId(rentalCategoryId);
        return colors;
    }

    public Product readById(Long id) {
        return prodRepo.findById(id).orElseThrow();
    }

    public void deleteById(Long id) {
    		prodRepo.deleteById(id);
    }

    // 브랜드 직접 입력시 브랜드 만들기
    public Brand createBrand(Long rentalCategortId, String name) {
        log.info("createBrand(rentalCategoryId={}, name={})", rentalCategortId, name);
        RentalCategory rentalCategory = rentalCateRepo.findById(rentalCategortId).orElseThrow();
        Brand entity = Brand.builder()
        .name(name.toUpperCase())
        .imagePath(name.toLowerCase() + ".jpg")
        .rentalCategory(rentalCategory)
        .build();
        brandRepo.save(entity);
        return entity;
    }

    public <T> Page<T> listToPage(List<T> list, Pageable pageable) {
        if(list == null || list.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = pageSize * currentPage;

        if(startItem >= list.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, list.size());
        }

        int endItem = Math.min(startItem + pageSize, list.size());

        List<T> subList = list.subList(startItem, endItem);

        return new PageImpl<>(subList, pageable, list.size());
    }
    
    // 첫 번째 이미지를 ProductImageMergeDto에 추가
    @Transactional(readOnly = true)
    public List<ProductImageMergeDto> addFirstImageInProduct(List<Product> products) {
        List<Long> ids = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        List<RentalImage> rentalImages = rentalImgRepo.findAllByProductIdIn(ids);

        Map<Long, List<RentalImage>> imagesMap = rentalImages.stream()
                .collect(Collectors.groupingBy(img -> img.getProduct().getId()));

        List<ProductImageMergeDto> result = new ArrayList<>();

        for (Product product : products) {
            Long productId = product.getId();
            List<RentalImage> imageList = imagesMap.getOrDefault(productId, Collections.emptyList());
            RentalImage pickedImage = imageList.isEmpty() ? null : imageList.get(0); // <--- 이미지가 없으면 null로 전달

            result.add(buildProductImageMergeDto(product, pickedImage)); // <--- 헬퍼 메서드 호출
        }
        return result;
    }

    public List<SearchedProductDto> addAddressInProduct(List<ProductImageMergeDto> products) {
        List<Long> ids = products.stream()
            .map(ProductImageMergeDto :: getId)
            .collect(Collectors.toList());

        List<Address> addresses = addrRepo.findAllByProduct_IdIn(ids);

        Map<Long, Address> addressMap = addresses.stream()
            .collect(Collectors.toMap(address -> address.getProduct().getId(), Function.identity()));


        List<SearchedProductDto> searchedProduct = products.stream()
            .map(product -> {
                Address address = addressMap.get(product.getId());
                return SearchedProductDto.fromEntity(product, address);
            })
            .collect(Collectors.toList());

        return searchedProduct;
    }

    // 제품의 요금 가져오기
    public int readFeeById(Long id) {
    	log.info("readFeeById(id={})", id);
    	
    	return prodRepo.findById(id).orElseThrow().getFee();
    }
    
    // 가방, 자동차 한글에서 영어로 하드 코딩 추후 필요하면 추가해야함
    
    private String mapCategoryToCode(String categoryName) {
        return switch (categoryName) {
            case "가방" -> "bag";
            case "자동차" -> "car";
            default -> "all";
        };
    }
    
    // 특정 유저의 상품 목록을 조회
    @Transactional(readOnly = true)
    public List<PostSummaryDto> getUserProducts(Long userId) {
        log.info("getUserProducts(userId={})", userId);

        List<Product> products = prodRepo.findByUser_IdOrderByCreatedTimeDesc(userId);

        // addRandomImageInProduct가 이미 URL을 포함한 ProductImageMergeDto를 반환
        // ProductImageMergeDto의 filePath 필드에 S3 URL이 있으므로, PostSummaryDto.image에 그대로 할당
        return addFirstImageInProduct(products).stream()
                .map(productImageMergeDto -> {
                    PostSummaryDto dto = new PostSummaryDto();
                    dto.setId(productImageMergeDto.getId());
                    dto.setTitle(productImageMergeDto.getPostName());
                    dto.setPrice(productImageMergeDto.getFee());
                    dto.setImage(productImageMergeDto.getFilePath()); // <--- filePath에 이미 URL이 있으므로 그대로 사용
                    dto.setRentalCategory(mapCategoryToCode(productImageMergeDto.getRentalCategory()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    // 상품 상세 정보 조회
    public Product getProductById(Long productId) {
        log.info("getProductById(productId={})", productId);
        return prodRepo.findById(productId).orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + productId));
    }
    
    public Map<String, List<ProductImageMergeDto>> readHomeProducts(Long rentalCategoryId, String region) {
        Map<String, List<ProductImageMergeDto>> resultMap = new HashMap<>();

        List<Address> readBySido = addrRepo.findTop20BySidoContaining(region);
        List<Long> ids = readBySido.stream()
                .map(address -> address.getProduct().getId())
                .collect(Collectors.toList());

        List<Product> recentProducts = prodRepo.recentProducts();
        List<Product> themeProducts = prodRepo.themeProducts(rentalCategoryId);
        List<Product> regionProducts = prodRepo.findAllByIdIn(ids);

        // recent는 랜덤이 아니라 항상 첫 번째 이미지 고정
        List<ProductImageMergeDto> recentProductImageMergeDto = addFirstImageInProduct(recentProducts);
        List<ProductImageMergeDto> themeProductImageMergeDto = addFirstImageInProduct(themeProducts);
        List<ProductImageMergeDto> regionProductImageMergeDto = addFirstImageInProduct(regionProducts);

        resultMap.put("recent", recentProductImageMergeDto);
        resultMap.put("theme", themeProductImageMergeDto);
        resultMap.put("region", regionProductImageMergeDto);

        return resultMap;
    }

    
    @Transactional // 스프링의 @Transactional 사용
    public void deleteProduct(Long productId) {
        // 모든 관련 예약 조회 (ReservationDto 리스트로 받음)
        List<ReservationDto> reservations = reservationService.readAll(productId);
        
        if (reservations != null && !reservations.isEmpty()) {
            // 삭제 가능한 예약만 필터링 후 삭제 (상태 4, 5, 7)
            reservations.stream()
                .filter(reservationDto -> reservationDto.getStatus() == 4 || reservationDto.getStatus() == 5 || reservationDto.getStatus() == 7) // <--- ReservationDto 사용
                .map(ReservationDto::getId) // <--- ReservationDto의 getId() 사용
                .forEach(reservationService::delete); // 예약 삭제 서비스 호출
            
            // 남은 예약이 있는지 확인 (ReservationDto 리스트로 받음)
            List<ReservationDto> remainingReservations = reservationService.readAll(productId);
            if (remainingReservations != null && !remainingReservations.isEmpty()) {
                throw new IllegalStateException("삭제할 수 없는 예약이 존재합니다.");
            }
        }
        prodRepo.deleteById(productId);
    }
    
    public List<Brand> readBrandsByCategory(Long rentalCategoryId) {
        log.info("readBrandsByCategory(rentalCategoryId={})", rentalCategoryId);
        List<Brand> brands;
        if (rentalCategoryId == null || rentalCategoryId == 99L) {
            brands = brandRepo.findAll();
            log.info("모든 브랜드 반환 (CUSTOM 포함): {}", brands);
        } else {
            brands = brandRepo.findByRentalCategoryId(rentalCategoryId).stream()
                .filter(brand -> !brand.getName().equalsIgnoreCase("CUSTOM"))
                .collect(Collectors.toList());
            log.info("카테고리 {} 브랜드 반환 (CUSTOM 제외): {}", rentalCategoryId, brands);
        }
        return brands;
    }
    
    // ProductImageMergeDto를 생성할 때 filePath를 Pre-signed URL로 변환하는 헬퍼 메서드
    private ProductImageMergeDto buildProductImageMergeDto(Product product, RentalImage pickedImage) {
        String imageUrl = null;
        String actualS3Key = null; // generateDownloadPresignedUrl에 전달할 실제 S3 Key

        if (pickedImage != null && pickedImage.getFilePath() != null) {
            String dbFilePath = pickedImage.getFilePath();

            // dbFilePath가 이미 S3 Key 형태인지 확인
            if (dbFilePath.startsWith("product_images/") || dbFilePath.startsWith("avatars/") || dbFilePath.startsWith("chat_images/") || dbFilePath.startsWith("test/")) {
                actualS3Key = dbFilePath; // 이미 올바른 S3 Key
            } else {
                // dbFilePath가 순수 파일명(예: "UUID.png")인 경우, "product_images/" 접두사를 붙여 S3 Key를 완성
                actualS3Key = "product_images/" + dbFilePath;
            }

            try {
                // generateDownloadPresignedUrl에 실제 S3 Key (actualS3Key)를 전달
                imageUrl = s3FileStorageService.generateDownloadPresignedUrl(actualS3Key, Duration.ofHours(1));
                // 로그 확인: Generated presigned URL for S3 Key [actualS3Key]: [생성된 올바른 S3 URL]
                log.info("ProductService: Generated presigned URL for S3 Key {}: {}", actualS3Key, imageUrl);
            } catch (FileStorageException e) {
                log.error("ProductService: S3 Pre-signed URL 생성 실패 for {}: {}", actualS3Key, e.getMessage(), e);
                imageUrl = "/images/default-product.png"; // 실패 시 대체 이미지
            }
        } else {
            imageUrl = "/images/default-product.png"; // 이미지가 없는 경우 기본 이미지
        }

        return ProductImageMergeDto.builder()
                .id(product.getId())
                .rentalCategoryId(product.getRentalCategory().getId())
                .rentalCategory(product.getRentalCategory().getCategory())
                .productName(product.getProductName())
                .fee(product.getFee())
                .postName(product.getPostName())
                .imageId(pickedImage != null ? pickedImage.getId() : null)
                .filePath(imageUrl) // filePath 필드에 S3 Pre-signed URL을 담음
                .build();
    }
}
