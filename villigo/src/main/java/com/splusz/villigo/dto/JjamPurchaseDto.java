package com.splusz.villigo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JjamPurchaseDto {
    private Long userId;   // 유저 ID
    private int quantity;  // 충전할 젤리 개수
}
