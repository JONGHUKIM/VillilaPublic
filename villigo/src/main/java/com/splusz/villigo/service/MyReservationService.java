package com.splusz.villigo.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.dto.MyReservationDto;
import com.splusz.villigo.dto.RentalImageDto;
import com.splusz.villigo.repository.MyReservationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyReservationService {

	
    private final MyReservationRepository myReservationRepository;
    private final RentalImageService rentalImageService;

    public List<MyReservationDto> getMyReservations(Long userId) {
        List<Reservation> reservations = myReservationRepository.findByRenterIdWithProduct(userId);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return reservations.stream().map(reservation -> {
            MyReservationDto dto = new MyReservationDto();
            dto.setReservationId(reservation.getId());

            if (reservation.getRetalCategory() != null) {
                dto.setRentalCategoryId(reservation.getRetalCategory().getId());
            }

            dto.setProductName(reservation.getProduct().getProductName());
            dto.setFee((long) reservation.getProduct().getFee());
            dto.setProductId(reservation.getProduct().getId());

            dto.setProductOwnerId(reservation.getProduct().getUser().getId());

            dto.setRentalDate(reservation.getStartTime().format(dateFormatter));
            String timeRange = reservation.getStartTime().format(timeFormatter) + " ~ " +
                                 reservation.getEndTime().format(timeFormatter);
            dto.setRentalTimeRange(timeRange);

            // S3 Pre-signed URL을 가져와서 imageUrl 설정
            List<RentalImageDto> rentalImages = rentalImageService.readByProductId(reservation.getProduct().getId());
            String imageUrl = rentalImages.isEmpty() ? "/images/default-product.png" : rentalImages.get(0).getImageUrl();
            dto.setImageUrl(imageUrl);

            dto.setStatus(reservation.getStatus());

            return dto;
        }).collect(Collectors.toList());
    }
}