package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.splusz.villigo.domain.Reservation;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	
	List<Reservation> findByProductId(Long id);
	
	List<Reservation> findByRenterId(Long id);
	
	List<Reservation> findByProductRentalCategoryId(Long id);
	
	Page<Reservation> findByProductUserId(Long id, Pageable pageable);
}
