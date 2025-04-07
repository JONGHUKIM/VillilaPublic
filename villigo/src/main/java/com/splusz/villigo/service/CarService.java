package com.splusz.villigo.service;

import java.text.SimpleDateFormat;

import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Car;
import com.splusz.villigo.domain.Color;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.CarCreateDto;
import com.splusz.villigo.dto.CarUpdateDto;
import com.splusz.villigo.repository.AddressRepository;
import com.splusz.villigo.repository.BrandRepository;
import com.splusz.villigo.repository.CarRepository;
import com.splusz.villigo.repository.ColorRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalCategoryRepository;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarService {

    private final ProductRepository prodRepo;
    private final CarRepository carRepo;
    private final BrandRepository brandRepo;
    private final ColorRepository colorRepo;
    private final UserRepository userRepo;
    private final RentalCategoryRepository rentalCateRepo;

    public Product create(CarCreateDto dto, String username, Long rentalCategoryId) {
        Brand brand = brandRepo.findById(dto.getBrandId()).orElseThrow();
        Color color = colorRepo.findById(dto.getColorId()).orElseThrow();
        User user = userRepo.findByUsername(username).orElseThrow();
        RentalCategory rentalCategory = rentalCateRepo.findById(rentalCategoryId).orElseThrow();

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

       Car cEntity = Car.builder()
           .product(pEntity)
           .old(dto.getOld())
           .drive(dto.getDrive())
           .minRentalTime(dto.getMinRentalTime())
           .build();

       carRepo.save(cEntity);

        return product;
    }

    public Car readByProductId(Long productId) {
        Car car = carRepo.findByProductId(productId);
        return car;
    }

    public Product update(Long productId, CarUpdateDto dto) {
        Product pEntity = prodRepo.findById(productId).orElseThrow();
        pEntity.update(dto);
        prodRepo.save(pEntity);

        Car cEntity = carRepo.findByProductId(productId);
        cEntity.update(dto);
        carRepo.save(cEntity);

        return pEntity;
    }

    public void deleteById(Long id) {
        carRepo.deleteById(id);
    }
}
