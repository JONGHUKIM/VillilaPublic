package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.splusz.villigo.domain.Color;

public interface ColorRepository extends JpaRepository<Color, Long> {

    List<Color> findByRentalCategoryId(Long rentalCategoryId);

    @Query("select c from Color c where c.id in (select min(c2.id) from Color c2 group by c2.colorNumber)")
    List<Color> findDistinctByColorNumber();
}
