package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchedProductDto {

    private Long id;
    private Long rentalCategoryId;
    private String productName;
    private Integer fee;
    private String postName;
    private String fullAddress;
    private Double latitude;
    private Double longitude;
    private Long imageId;
    private String filePath;

    public static SearchedProductDto fromEntity(ProductImageMergeDto product, Address address) {
        return SearchedProductDto.builder()
            .id(product.getId())
            .rentalCategoryId(product.getRentalCategoryId())
            .productName(product.getProductName())
            .fee(product.getFee())
            .postName(product.getPostName())
            .fullAddress(address.getFullAddress())
            .latitude(address.getLatitude())
            .longitude(address.getLongitude())
            .imageId(product.getImageId())
            .filePath(product.getFilePath())
            .build();
    }
}
