package com.splusz.villigo.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class CarCreateDto extends ProductCreateDto {

    private int old;
    private Boolean drive;
    private int minRentalTime;
    
}

