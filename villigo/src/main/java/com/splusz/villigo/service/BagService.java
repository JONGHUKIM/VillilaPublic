package com.splusz.villigo.service;

import com.splusz.villigo.domain.*;
import com.splusz.villigo.dto.BagCreateDto;
import com.splusz.villigo.dto.BagUpdateDto;
import com.splusz.villigo.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BagService {

    private final ProductRepository productRepository;
    private final BagRepository bagRepository;
    private final BrandRepository brandRepository;
    private final ColorRepository colorRepository;
    private final RentalCategoryRepository rentalCategoryRepository;

    /**
     * 가방 등록
     */
    @Transactional
    public Product create(BagCreateDto dto, User user, Long rentalCategoryId) {
        Brand brand = brandRepository.findById(dto.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 브랜드 ID"));
        Color color = colorRepository.findById(dto.getColorId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 색상 ID"));
        RentalCategory rentalCategory = rentalCategoryRepository.findById(rentalCategoryId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 카테고리 ID"));

        Product product = Product.builder()
                .rentalCategory(rentalCategory)
                .user(user)
                .productName(dto.getProductName())
                .brand(brand)
                .color(color)
                .detail(dto.getDetail())
                .fee(dto.getFee())
                .postName(dto.getPostName())
                .build();

        return productRepository.save(product);
    }

    /**
     * 제품 ID로 가방 정보 조회
     */
    @Transactional(readOnly = true)
    public Bag readByProductId(Long productId) {
        return bagRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 제품 ID의 가방을 찾을 수 없습니다."));
    }

    /**
     * 제품 정보 수정
     */
    @Transactional
    public Product update(Long productId, BagUpdateDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 제품을 찾을 수 없습니다."));

        log.info("가방 수정 detail={}, postName={}", dto.getDetail(), dto.getPostName());
        product.update(dto); // 엔티티 내부에서 업데이트 처리
        return productRepository.save(product);
    }

    /**
     * 제품 ID로 가방 삭제
     */
    @Transactional
    public void deleteByProductId(Long productId) {
        bagRepository.deleteById(productId);
    }
}
