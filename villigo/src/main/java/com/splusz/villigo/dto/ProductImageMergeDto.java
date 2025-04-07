package com.splusz.villigo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductImageMergeDto {

    private Long id;
    private Long rentalCategoryId;
    private String productName;
    private Integer fee;
    private String postName;
    private Long imageId;
    private String filePath;
    private String rentalCategory;

}
