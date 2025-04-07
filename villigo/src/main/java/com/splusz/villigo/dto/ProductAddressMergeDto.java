package com.splusz.villigo.dto;

import com.querydsl.core.annotations.QueryProjection;

public class ProductAddressMergeDto {

    private Long id;
    private Long rentalCategoryId;
    private String productName;
    private Integer fee;
    private String postName;
    private String fullAddress;
    private Double latitude;
    private Double longitude;

    public ProductAddressMergeDto(Long id, Long rentalCategoryId, String productName, Integer fee, 
        String postName, String fullAddress, Double latitude, Double longitude) {
            this.id = id;
            this.rentalCategoryId = rentalCategoryId;
            this.productName = productName;
            this.fee = fee;
            this.postName = postName;
            this.fullAddress = fullAddress;
            this.latitude = latitude;
            this.longitude = longitude;
        }
}
