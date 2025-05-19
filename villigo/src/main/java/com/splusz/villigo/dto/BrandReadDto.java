package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Brand;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
public class BrandReadDto {
	private Long id;
	private String name;
	private String imagePath;
	
	public static BrandReadDto fromEntity(Brand brand) {
        BrandReadDto dto = new BrandReadDto();
        dto.setId(brand.getId());
        dto.setName(brand.getName());
        dto.setImagePath(brand.getImagePath());
        return dto;
	}
}
