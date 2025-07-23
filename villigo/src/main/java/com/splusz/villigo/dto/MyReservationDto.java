package com.splusz.villigo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class MyReservationDto {
	private Long rentalCategoryId; // 상품 종류 
    private Long reservationId; // 예약 ID
    private String productName; // 제품 이름 (예: "벤츠 C클래스")
    private String rentalDate; // 대여 날짜 (start_time에서 가져옴)
    private String rentalTimeRange; // 대여 시간 범위 (예: "10:00 ~ 16:00")
    private Long fee; // 요금 (JJAM 단위)
    private Long displayFee;
    private String imageUrl; // S3 Pre-signed URL
    private Integer status; // 예약 상태 (0: 예약신청, 1: 대기중, 2: 거래중, 3: 거래완료, 4: 거절됨)
    private Long productId; // 상품 아이디
    private Long productOwnerId;
    private Long chatRoomId;
    private String renterNickname;
    
    /**
     * 5% 서비스 수수료를 적용하고 10원 단위로 반올림한 요금을 계산하여 설정
     */
    public void calculateDisplayFee() {
        if (this.fee != null) {
            // 5% 수수료 계산
            double serviceFee = this.fee * 0.05;
            
            // 10원 단위로 반올림
            long roundedServiceFee = Math.round(serviceFee / 10.0) * 10;
            
            // 최종 표시 요금 계산
            this.displayFee = this.fee + roundedServiceFee;
        }
    }
    
    /**
     * fee를 설정할 때 자동으로 displayFee도 계산
     */
    public void setFee(Long fee) {
        this.fee = fee;
        calculateDisplayFee();
    }
}