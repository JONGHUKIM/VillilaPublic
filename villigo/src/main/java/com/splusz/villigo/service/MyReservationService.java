package com.splusz.villigo.service;

import com.splusz.villigo.dto.MyReservationDto;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.repository.MyReservationRepository;
import com.splusz.villigo.repository.RentalImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyReservationService {

    @Autowired
    private MyReservationRepository myReservationRepository;

    @Autowired
    private RentalImageRepository rentalImageRepository;

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

            // ⭐️ 오너 ID 설정
            dto.setProductOwnerId(reservation.getProduct().getUser().getId());

            dto.setRentalDate(reservation.getStartTime().format(dateFormatter));
            String timeRange = reservation.getStartTime().format(timeFormatter) + " ~ " +
                               reservation.getEndTime().format(timeFormatter);
            dto.setRentalTimeRange(timeRange);

            List<RentalImage> rentalImages = rentalImageRepository.findByProductId(reservation.getProduct().getId());
            String imagePath = rentalImages.isEmpty() ? "/images/default.jpg" : rentalImages.get(0).getFilePath();
            dto.setImagePath(imagePath);

            dto.setStatus(reservation.getStatus());

            return dto;
        }).collect(Collectors.toList());
    }
}