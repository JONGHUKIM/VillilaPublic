package com.splusz.villigo.dto;

import lombok.Data;

@Data
public class PostSummaryDto {
    private Long id;          // 상품 ID (products.id)
    private String title;     // 게시글 제목 (products.post_name)
    private String image;     // 상품 이미지 (rental_images.file_path)
    private int price;        // 대여료 (products.fee)
    private String rentalCategory;
}
