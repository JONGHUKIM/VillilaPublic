package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.Like;

public interface LikeRepository extends JpaRepository<Like, Long> {

    @Transactional
    Integer deleteByUser_IdAndProduct_Id(Long userId, Long productId);

    List<Like> findByUser_IdOrderByCreatedTime(Long userId);
}
