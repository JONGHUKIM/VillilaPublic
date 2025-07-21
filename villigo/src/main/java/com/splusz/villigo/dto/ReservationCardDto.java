package com.splusz.villigo.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReservationCardDto {
    private Long rentalCategoryId; // 상품 종류 
    private Long reservationId; // 예약 ID
    private String renterNickname; // 예약자 닉네임
    private Long renterId; // 예약자 ID
    private String productName; // 제품 이름 (예: "벤츠 C클래스")
    private String rentalDate; // 대여 날짜 (start_time에서 가져옴)
    private String rentalTimeRange; // 대여 시간 범위 (예: "10:00 ~ 16:00")
    private String fee; // 요금 (JJAM 단위)
    private Integer status; // 예약 상태 (0: 예약신청, 1: 대기중, 2: 거래중, 3: 거래완료, 4: 거절됨)
    private Long productId; // 상품 아이디
    private String imageUrl; // S3 Pre-signed URL
}