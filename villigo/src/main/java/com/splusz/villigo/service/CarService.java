package com.splusz.villigo.service;

import com.splusz.villigo.domain.*;
import com.splusz.villigo.dto.CarCreateDto;
import com.splusz.villigo.dto.CarUpdateDto;
import com.splusz.villigo.repository.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final ProductRepository productRepository;
    private final CarRepository carRepository;
    private final BrandRepository brandRepository;
    private final ColorRepository colorRepository;
    private final RentalCategoryRepository rentalCategoryRepository;

    /**
     * 자동차 등록
     */
    @Transactional
    public Product create(CarCreateDto dto, User user, Long rentalCategoryId) {
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

        product = productRepository.save(product);

        Car car = Car.builder()
                .product(product)
                .old(dto.getOld())
                .drive(dto.getDrive())
                .minRentalTime(dto.getMinRentalTime())
                .build();

        carRepository.save(car);

        return product;
    }

    /**
     * 제품 ID로 자동차 정보 조회
     */
    @Transactional(readOnly = true)
    public Car readByProductId(Long productId) {
        return carRepository.findByProductId(productId);
    }

    /**
     * 자동차 정보 수정
     */
    @Transactional
    public Product update(Long productId, CarUpdateDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("해당 제품을 찾을 수 없습니다."));
        product.update(dto);
        productRepository.save(product);

        Car car = carRepository.findByProductId(productId);
        car.update(dto);
        carRepository.save(car);

        return product;
    }

    /**
     * 자동차 삭제
     */
    @Transactional
    public void deleteById(Long id) {
        carRepository.deleteById(id);
    }
}
