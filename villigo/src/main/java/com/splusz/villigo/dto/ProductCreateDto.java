package com.splusz.villigo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class ProductCreateDto {

    private Long rentalCategoryId;
    private Long userId;
    private String productName;
    private Long brandId;
    private Long colorId;
    private String detail;
    private int fee;
    private String postName;
    private String customBrand;
}
