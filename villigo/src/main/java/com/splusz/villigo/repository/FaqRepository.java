package com.splusz.villigo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.splusz.villigo.domain.Faq;

import java.util.Optional;

public interface FaqRepository extends JpaRepository<Faq, Long> {
    Optional<Faq> findByQuestionContaining(String keyword);
}
