package com.splusz.villigo.repository;

import com.splusz.villigo.domain.Review;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    
    // Find reviews by target user
    List<Review> findByTarget(User target);
    
    // Add this method to check if a review exists for a given reservation
    boolean existsByReservation(Reservation reservation);
} 
