package com.splusz.villigo.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.domain.UserJjam;
import com.splusz.villigo.repository.UserJjamRepository;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserJjamService {

    private final UserJjamRepository userJjamRepository;
    private final UserRepository userRepository;

    // 🔹 유저 총 젤리 개수 조회
    @Transactional(readOnly = true)
    public int getUserTotalJjams(Long userId) {
        Integer totalJjams = userJjamRepository.findTotalJjamsByUserId(userId);
        return (totalJjams != null) ? totalJjams : 0; // null이면 0 반환
    }

    // 🔹 유저 쩸 구매 (충전)
    @Transactional
    public UserJjam purchaseJjam(Long userId, int quantity) {
        // 1. 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 젤리 충전 기록 생성
        UserJjam userJjam = new UserJjam();
        userJjam.setUser(user);
        userJjam.setTransactionDate(LocalDate.now());
        userJjam.setTransactionAmount(quantity); // 양수는 충전

        // 3. 저장
        userJjamRepository.save(userJjam);

        // 4. 총 보유 젤리 조회
        int totalJjams = userJjamRepository.findTotalJjamsByUserId(userId);

        log.info("🔄 젤리 충전 완료 - User ID: {}, 충전량: {}, 현재 보유 젤리: {}",
                 userId, quantity, totalJjams);

        return userJjam;
    }

    // 🔹 유저 젤리 차감 (예약 시 사용)
    @Transactional
    public UserJjam subtractJjams(Long userId, int quantity) {
        // 1. 유저 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 2. 젤리 차감 기록 생성 (음수로 저장)
        UserJjam userJjam = new UserJjam();
        userJjam.setUser(user);
        userJjam.setTransactionDate(LocalDate.now());
        userJjam.setTransactionAmount(-quantity); // 음수는 차감

        // 3. 저장
        userJjamRepository.save(userJjam);

        // 4. 남은 젤리 조회
        int remaining = userJjamRepository.findTotalJjamsByUserId(userId);

        log.info("💸 젤리 차감 완료 - User ID: {}, 차감량: {}, 남은 젤리: {}",
                 userId, quantity, remaining);

        return userJjam;
    }
}
