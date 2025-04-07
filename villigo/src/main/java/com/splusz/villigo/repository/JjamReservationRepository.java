package com.splusz.villigo.repository;

import com.splusz.villigo.domain.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JjamReservationRepository extends JpaRepository<Reservation, Long> {}
