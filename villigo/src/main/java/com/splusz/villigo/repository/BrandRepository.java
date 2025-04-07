package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splusz.villigo.domain.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    List<Brand> findByRentalCategoryId(Long rentalCategoryId);
    
}
