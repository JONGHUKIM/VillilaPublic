package com.splusz.villigo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splusz.villigo.domain.Car;

public interface CarRepository extends JpaRepository<Car, Long>{

    Car findByProductId(Long productId);
}
