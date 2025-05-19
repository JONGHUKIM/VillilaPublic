package com.splusz.villigo.repository;

import com.splusz.villigo.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MyReservationRepository extends JpaRepository<Reservation, Long> {

    // renter_id로 예약을 조회하는 메서드 - 생성시간(createdTime)을 기준으로 내림차순 정렬
    List<Reservation> findByRenterIdOrderByCreatedTimeDesc(Long renterId);

    // renter_id로 예약을 조회하면서 Product 정보를 함께 가져오는 메서드 - 생성시간(createdDate)을 기준으로 내림차순 정렬
    @Query("SELECT r FROM Reservation r JOIN FETCH r.product p WHERE r.renter.id = :renterId ORDER BY r.createdTime DESC")
    List<Reservation> findByRenterIdWithProduct(@Param("renterId") Long renterId);
}
