package com.splusz.villigo.repository;

import com.splusz.villigo.domain.ReviewKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewKeywordRepository extends JpaRepository<ReviewKeyword, Long> {
    // 기본적인 findById, findAll 등이 자동 제공됨
}
 