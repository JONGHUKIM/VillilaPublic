package com.splusz.villigo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDto {
    private Long id; // 예약 ID (기존 필드)
    private String details; // 예: "대여 날짜: 2025-03-30" (기존 필드)
    private Long chatRoomId; // 채팅방 ID (기존 필드)

    // ReservationService의 convertToReservationDto에서 사용되는 필드들 추가
    private Long rentalCategoryId; // 상품 종류 ID
    private String productName; // 제품 이름
    private String imageUrl; // 상품 이미지 URL (S3 Pre-signed URL)
    private Integer fee; // 요금 (int 타입으로 가정, Long이라면 Long으로 변경)
    private String rentalDate; // 대여 날짜 (String으로 형식화된)
    private String rentalTimeRange; // 대여 시간 범위 (String으로 형식화된)
    private Integer status; // 예약 상태
    private Long productOwnerId; // 상품 소유자 ID
    private String renterNickname;
    
    private Long renterId;
}