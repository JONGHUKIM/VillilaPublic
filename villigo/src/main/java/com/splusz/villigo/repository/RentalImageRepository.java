package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.RentalImage;

public interface RentalImageRepository extends JpaRepository<RentalImage, Long> {

    List<RentalImage> findByProductId(Long productId);

    @Query("SELECT r.id FROM RentalImage r WHERE r.product.id = :productId")
    List<Long> findIdByProductId(@Param("productId") Long productId);

    List<RentalImage> findAllByProductIdIn(@Param("productId") List<Long> ids);

}
