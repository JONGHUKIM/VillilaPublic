package com.splusz.villigo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.splusz.villigo.repository.UserJjamRepository;
import com.splusz.villigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JjamReservationService {
    private final UserJjamRepository userJjamRepository;
    private final UserRepository userRepository;

    // ✅ 유저 총 젤리 개수 조회
    @Transactional(readOnly = true)
    public int getUserTotalJjams(Long userId) {
        Integer totalJjams = userJjamRepository.findTotalJjamsByUserId(userId);
        return (totalJjams != null) ? totalJjams : 0; // null이면 0 반환
    }

    // ✅ 유저 젤리 차감 기능 추가
    @Transactional
    public void subtractJjams(Long userId, int fee) {
        userJjamRepository.subtractJjams(userId, fee);
        log.info("🔄 젤리 차감 완료 - User ID: {}, 차감된 젤리: {}", userId, fee);
    }
}
