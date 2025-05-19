package com.splusz.villigo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.Bag;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Color;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.BagCreateDto;
import com.splusz.villigo.dto.BagUpdateDto;
import com.splusz.villigo.dto.ProductUpdateDto;
import com.splusz.villigo.repository.BagRepository;
import com.splusz.villigo.repository.BrandRepository;
import com.splusz.villigo.repository.ColorRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalCategoryRepository;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class BagService {
    
    private final ProductRepository prodRepo;
    private final BagRepository bagRepo;
    private final BrandRepository brandRepo;
    private final ColorRepository colorRepo;
    private final UserRepository userRepo;
    private final RentalCategoryRepository rentalCateRepo;

    public Product create(BagCreateDto dto, String username, Long rentalCategoryNumber) {

       Brand brand = brandRepo.findById(dto.getBrandId()).orElseThrow();
       Color color = colorRepo.findById(dto.getColorId()).orElseThrow();
       User user = userRepo.findByUsername(username).orElseThrow();
       RentalCategory rentalCategory = rentalCateRepo.findById(rentalCategoryNumber).orElseThrow();

       Product pEntity = Product.builder()
           .rentalCategory(rentalCategory)
           .user(user)
           .productName(dto.getProductName())
           .brand(brand)
           .color(color)
           .detail(dto.getDetail())
           .fee(dto.getFee())
           .postName(dto.getPostName())
           .build();

       Product product = prodRepo.save(pEntity);
        return product;
    }

    public Bag readByProductId(Long productId) {
        Bag bag = bagRepo.findById(productId).orElseThrow();
        return bag;
    }

    public Product update(Long productId, BagUpdateDto dto) {
        Product entity = prodRepo.findById(productId).orElseThrow();
        log.info(dto.getDetail());
        log.info(dto.getPostName());
        entity.update(dto);
        prodRepo.save(entity);

        return entity;
    }

    public void deleteByProductId(Long productId) {
        bagRepo.deleteById(productId);
    }
}
