package com.splusz.villigo.repository;

import com.splusz.villigo.domain.UserJjam;
import com.splusz.villigo.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserJjamRepository extends JpaRepository<UserJjam, Long> {

    // 🔹 특정 유저의 총 젤리 개수를 조회 (NULL 방지)
    @Query("SELECT COALESCE(SUM(u.transactionAmount), 0) FROM UserJjam u WHERE u.user.id = :userId")
    Integer findTotalJjamsByUserId(@Param("userId") Long userId);

    // 🔹 특정 유저의 최근 충전 내역 조회 (최신순 정렬, 최신이 가장 위)
    @Query("SELECT u FROM UserJjam u WHERE u.user.id = :userId ORDER BY u.transactionDate DESC")
    List<UserJjam> findRecentTransactionsByUserId(@Param("userId") Long userId);

    // 🔹 특정 유저의 특정 기간 내 총 젤리 충전량 조회
    @Query("SELECT COALESCE(SUM(u.transactionAmount), 0) FROM UserJjam u WHERE u.user.id = :userId AND u.transactionDate BETWEEN :startDate AND :endDate")
    Integer findTotalJjamsByUserIdWithinPeriod(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 🔹 특정 유저의 충전 내역을 페이징 처리하여 조회 (최신순 정렬)
    @Query("SELECT u FROM UserJjam u WHERE u.user.id = :userId ORDER BY u.transactionDate DESC")
    Page<UserJjam> findTransactionsByUserIdPaged(@Param("userId") Long userId, Pageable pageable);

    // 🔹 특정 유저의 마지막 충전 내역 가져오기 (가장 최근 날짜)
    @Query("SELECT u FROM UserJjam u WHERE u.user.id = :userId ORDER BY u.transactionDate DESC LIMIT 1")
    UserJjam findLatestTransactionByUserId(@Param("userId") Long userId);

    // ✅ 🔹 젤리 차감 기능 (예약 시 사용)
    @Modifying
    @Transactional
    @Query("UPDATE UserJjam u SET u.transactionAmount = u.transactionAmount - :fee WHERE u.user.id = :userId")
    void subtractJjams(@Param("userId") Long userId, @Param("fee") int fee);

    // ✅ 🔹 젤리 충전 기록 추가 (충전 후 기록용)
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO UserJjam (user_id, transaction_date, transaction_amount) VALUES (:userId, CURRENT_TIMESTAMP, :amount)", nativeQuery = true)
    void addJjamTransaction(@Param("userId") Long userId, @Param("amount") int amount);

    // ✅ 🔹 특정 유저의 최근 젤리 사용 내역 조회 (예약 등에 사용된 내역)
    @Query("SELECT u FROM UserJjam u WHERE u.user.id = :userId AND u.transactionAmount < 0 ORDER BY u.transactionDate DESC LIMIT 1")
    UserJjam findLatestJjamUsageByUserId(@Param("userId") Long userId);
    
    // 특정 사용자의 모든 UserJjam 레코드를 조회
    List<UserJjam> findByUser(User user);
}
