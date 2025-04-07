package com.splusz.villigo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.splusz.villigo.domain.Jjam;

@Repository
public interface JjamRepository extends JpaRepository<Jjam, Long> {
}
