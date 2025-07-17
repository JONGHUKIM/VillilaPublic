package com.splusz.villigo.service;

import com.splusz.villigo.domain.*;
import com.splusz.villigo.dto.ReviewDto;
import com.splusz.villigo.repository.*;
import com.splusz.villigo.storage.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewKeywordRepository reviewKeywordRepository;
    private final ReservationRepository reservationRepository;
    private final S3FileStorageService s3FileStorageService;

    // 후기 작성 및 매너 점수 업데이트
    @Transactional
    public Reservation submitReview(Long writerId, ReviewDto dto) {
        log.info("후기 작성 요청: writerId={}, dto={}", writerId, dto);

        User writer = userRepository.findById(writerId)
            .orElseThrow(() -> new IllegalArgumentException("작성자 없음"));
        User target = userRepository.findById(dto.getTargetId())
            .orElseThrow(() -> new IllegalArgumentException("상대방 없음"));
        ReviewKeyword keyword = reviewKeywordRepository.findById(dto.getKeywordId())
            .orElseThrow(() -> new IllegalArgumentException("키워드 없음"));
        Reservation reservation = reservationRepository.findById(dto.getReservationId())
            .orElseThrow(() -> new IllegalArgumentException("예약 없음"));

        // 후기 객체 생성
        Review review = new Review();
        review.setWriter(writer);
        review.setTarget(target);
        review.setContent(dto.getContent());
        review.setKeyword(keyword);
        review.setReservation(reservation); // 예약 연결
        reviewRepository.save(review);
        log.info("후기 객체 생성 완료");

        // 예약 상태 변경
        if (dto.getIsOwner() == 1) { // 상품 주인이 작성한 후기면
        	reservation.setStatus(7); // 예약 상태를 후기 작성 완료로 변경
        } else {
        	reservation.setStatus(3); // 예약 상태를 완료로 변경
        }
        reservationRepository.save(reservation); // 예약 상태 업데이트
        log.info("예약 상태 업데이트 완료(id={})", reservation.getId());

        // 매너 점수 업데이트
        updateMannerScore(target, keyword.getScore()); // 리뷰 후기자가 받은 점수를 반영

        log.info("후기 저장 완료: writer={}, target={}, keyword={}, content={}", writer.getId(), target.getId(), keyword.getKeyword(), dto.getContent());
        
        return reservation;
    }

    // 매너 점수 업데이트
    private void updateMannerScore(User user, int scoreChange) {
        int currentMannerScore = user.getMannerScore();  // 현재 매너 점수 (int 타입으로 가져옵니다)
        int newMannerScore = currentMannerScore + scoreChange;  // 점수 변화 후 새로운 매너 점수
        user.setMannerScore(newMannerScore);  // 매너 점수 갱신
        userRepository.save(user);  // 유저 정보 저장
        log.info("매너 점수 업데이트: userId={}, newMannerScore={}", user.getId(), newMannerScore);
    }


    // 후기 조회
    @Transactional(readOnly = true)
    public List<ReviewDto> getReviewsForUser(Long userId) {
        log.info("후기 조회 요청: userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("유저를 찾을 수 없음"));
        List<Review> reviews = reviewRepository.findByTarget(user);

        log.info("조회된 리뷰 개수: {}", reviews.size());

        return reviews.stream()
<<<<<<< HEAD
            .map(review -> {
                User writer = review.getWriter(); // 리뷰 작성자 User 엔티티
                
                // --- 리뷰 작성자의 아바타 URL (Pre-signed URL) 생성 및 설정 ---
                String writerAvatarS3Key = writer.getAvatar(); // 작성자 User 엔티티에서 S3 Key 가져옴
                String userImageUrl = null; // DTO에 설정할 최종 이미지 URL
=======
                .map(review -> {
                    User writer = review.getWriter(); // 리뷰 작성자 User 엔티티
>>>>>>> 07f3b7f (탈퇴회원_ 으로 시작하면 탈퇴회원으로만 보여지게끔 수정, 탈퇴회원은 기본 이모지로 변경)

                    String userNameForDisplay;
                    String userImageUrlForDisplay;
                    
                    // 사용자가 탈퇴한 회원인지 확인 (username이 탈퇴회원_으로 시작하는지)
                    if (writer.getUsername() != null && writer.getUsername().startsWith("탈퇴회원_")) {
                        userNameForDisplay = "탈퇴회원"; // 이름은 "탈퇴회원"으로 고정
                        userImageUrlForDisplay = "/images/default-avatar.png"; // 탈퇴회원은 기본 이모티콘/이미지
                    } else {
                        // 일반 회원인 경우 닉네임 또는 유저네임 사용
                        userNameForDisplay = writer.getNickname() != null && !writer.getNickname().trim().isEmpty() ? writer.getNickname() : writer.getUsername();
                        if (userNameForDisplay == null || userNameForDisplay.trim().isEmpty()) {
                            userNameForDisplay = "사용자_" + writer.getId(); // 비어있으면 ID로 대체
                        }

                        // 아바타 S3 Key를 Pre-signed URL로 변환
                        String writerAvatarS3Key = writer.getAvatar();
                        if (StringUtils.hasText(writerAvatarS3Key)) {
                            try {
                                userImageUrlForDisplay = s3FileStorageService.generateDownloadPresignedUrl(writerAvatarS3Key, Duration.ofMinutes(5));
                            } catch (FileStorageException e) {
                            	log.error("리뷰 작성자 아바타 Pre-signed URL 생성 실패 (S3 Key: {}): {}", writerAvatarS3Key, e.getMessage(), e);
                                userImageUrlForDisplay = "/images/default-avatar.png"; // 오류 시 기본 이미지
                            }
                        } else {
                            userImageUrlForDisplay = "/images/default-avatar.png"; // 아바타 없는 경우 기본 이미지
                        }
                    }
<<<<<<< HEAD
                } else {
                    // 아바타 S3 Key가 없는 경우 (아바타 미설정)
                    userImageUrl = "/images/default-avatar.png"; // 기본 이미지 URL 설정 (클라이언트에서 처리할 경로)
                }
                // --- 아바타 URL 설정 끝 ---
=======
>>>>>>> 07f3b7f (탈퇴회원_ 으로 시작하면 탈퇴회원으로만 보여지게끔 수정, 탈퇴회원은 기본 이모지로 변경)

                    return ReviewDto.builder()
                        .userId(writer.getId())
                        .userName(userNameForDisplay) // 수정된 이름 사용
                        .userImage(writer.getAvatar()) // S3 Key 
                        .userImageUrl(userImageUrlForDisplay) // 수정된 이미지 URL 사용
                        .score(review.getKeyword().getScore())
                        .content(review.getContent())
                        .keywordId(review.getKeyword().getId())
                        .targetId(review.getTarget().getId())
                        .reservationId(review.getReservation().getId())
                        .build();
                })
                .toList();
        }
} 
