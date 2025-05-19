package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    List<Product> findByUser_IdOrderByCreatedTimeDesc(Long userId);

    List<Product> findAllByIdIn(@Param("id") List<Long> ids);
}
