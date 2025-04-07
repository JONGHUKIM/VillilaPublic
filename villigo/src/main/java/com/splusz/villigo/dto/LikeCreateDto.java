package com.splusz.villigo.dto;

import lombok.Data;

@Data
public class LikeCreateDto {

    private Long itemId;
    private Long userId;
    private Long rentalCategoryId;
}
