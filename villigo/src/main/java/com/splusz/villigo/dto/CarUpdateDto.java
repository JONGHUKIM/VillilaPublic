package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Car;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class CarUpdateDto extends ProductUpdateDto {

    private Boolean drive;
    private int minRentalTime;

}
