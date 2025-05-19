package com.splusz.villigo.dto;

import lombok.Data;

@Data
public class PostDetailsDto {
    private Long id;              // 상품 ID (Product.id)
    private Long userId;          // 유저 ID (Product.user.id)
    private String productName;   // 상품 이름 (Product.productName)
    private String postName;      // 게시글 제목 (Product.postName)
    private String detail;        // 상품 설명 (Product.detail)
    private int fee;              // 대여료 (Product.fee)
    private String image;         // 상품 이미지 (가정: Product에 이미지 필드가 없으므로 별도로 처리 필요)
    private String nickname;      // 유저 닉네임 (Product.user.nickname)
    private String region;        // 유저 활동 지역 (Product.user.region)
    private String brandName;
    private String colorNumber;
    private String rentalCategoryName;
}
