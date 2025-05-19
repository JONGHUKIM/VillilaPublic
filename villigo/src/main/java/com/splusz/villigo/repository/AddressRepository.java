package com.splusz.villigo.repository;

import java.awt.print.Pageable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Address findByProductId(Long productId);

    List<Address> findAllByProduct_IdIn(@Param("productId") List<Long> ids);
    
    List<Address> findTop20BySidoContaining(String sido);
}
