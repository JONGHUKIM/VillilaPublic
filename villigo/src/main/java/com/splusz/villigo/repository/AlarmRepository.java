package com.splusz.villigo.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splusz.villigo.domain.Alarm;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {

	Page<Alarm> findByReceiverId(Long id, Pageable pageable);
	
	List<Alarm> findByReceiverId(Long id);
	
	@Query("select a from Alarm a "
			+ "where a.receiver.id = :id "
			+ "and a.status = false")
	Page<Alarm> findByReceiverIdAndStatus(@Param("id") Long id, Pageable pageable);
	
	@Query("select a from Alarm a "
			+ "where a.receiver.id = :id "
			+ "and a.status = false")
	List<Alarm> findByReceiverIdAndStatus(@Param("id") Long id);
}
