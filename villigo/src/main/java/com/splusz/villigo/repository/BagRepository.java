package com.splusz.villigo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splusz.villigo.domain.Bag;

public interface BagRepository extends JpaRepository<Bag, Long> {

    public Bag findByProductId(Long productId);
}
