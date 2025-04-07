package com.splusz.villigo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder
public class ProductUpdateDto {

    private Long id;
    private String postName;
    private String detail;
    private int fee;
}
