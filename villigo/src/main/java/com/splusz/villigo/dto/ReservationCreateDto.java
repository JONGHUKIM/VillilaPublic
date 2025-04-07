package com.splusz.villigo.dto;

import java.time.LocalDateTime;

import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.Reservation;

import lombok.Data;

@Data
public class ReservationCreateDto {
	private Long productId;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	
	public Reservation toEntity(Product product) {
		return Reservation.builder()
				.product(product)
				.startTime(startTime)
				.endTime(endTime)
				.build();
	}
}
