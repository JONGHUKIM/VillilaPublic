package com.splusz.villigo.dto;

import lombok.Data;

@Data
public class PostSummaryDto {
    private Long id;          // 상품 ID (products.id)
    private String title;     // 게시글 제목 (products.post_name)
    private String image;     // 상품 이미지 (rental_images.file_path)
    private int price;        // 대여료 (products.fee)
    private Long displayFee;
    private String rentalCategory;
    
    // 반올림 처리 타입을 위한 enum
    public enum RoundingType {
        ROUND,    // 반올림
        FLOOR,    // 버림
        CEILING   // 올림
    }

    /**
     * 5% 서비스 수수료를 적용하고 지정된 방식으로 반올림한 요금을 계산하여 설정
     * @param roundingType 반올림 처리 방식
     * @param roundingUnit 반올림 단위 (기본 10원)
     */
    public void calculateDisplayFee(RoundingType roundingType, int roundingUnit) {
        if (this.price > 0) {
            double serviceFee = this.price * 0.05;
            long totalFee = this.price + Math.round(serviceFee);
            
            // 지정된 방식으로 반올림 처리
            long roundedFee = applyRounding(totalFee, roundingType, roundingUnit);
            this.displayFee = roundedFee;
        }
    }

    /**
     * 기본 설정으로 계산 (반올림, 10원 단위)
     */
    public void calculateDisplayFee() {
        calculateDisplayFee(RoundingType.ROUND, 10);
    }

    /**
     * 반올림 처리 방식에 따라 계산
     * @param roundingType 반올림 처리 방식만 지정 (10원 단위 고정)
     */
    public void calculateDisplayFee(RoundingType roundingType) {
        calculateDisplayFee(roundingType, 10);
    }

    /**
     * 반올림 처리를 적용하는 헬퍼 메서드
     */
    private long applyRounding(long amount, RoundingType roundingType, int unit) {
        switch (roundingType) {
            case FLOOR:
                // 버림: amount를 unit으로 나눈 몫에 unit을 곱함
                return (amount / unit) * unit;
            
            case CEILING:
                // 올림: amount를 unit으로 나눈 몫+1에 unit을 곱함 (나머지가 있을 경우)
                return ((amount + unit - 1) / unit) * unit;
            
            case ROUND:
            default:
                // 반올림: amount + (unit/2)를 unit으로 나눈 몫에 unit을 곱함
                return ((amount + unit / 2) / unit) * unit;
        }
    }

    /**
     * fee를 설정할 때 자동으로 displayFee도 계산 (기본 반올림)
     */
    public void setFee(int fee) {
        this.price = fee;
        calculateDisplayFee();
    }

    /**
     * fee를 설정할 때 지정된 방식으로 displayFee 계산
     */
    public void setFee(int fee, RoundingType roundingType) {
        this.price = fee;
        calculateDisplayFee(roundingType);
    }

    /**
     * fee를 설정할 때 지정된 방식과 단위로 displayFee 계산
     */
    public void setFee(int fee, RoundingType roundingType, int roundingUnit) {
        this.price = fee;
        calculateDisplayFee(roundingType, roundingUnit);
    }
}
