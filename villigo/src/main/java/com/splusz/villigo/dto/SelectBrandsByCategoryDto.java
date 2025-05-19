package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Brand;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class SelectBrandsByCategoryDto {

	private Long id;
	private String name;
	private String imagePath;
	
	public SelectBrandsByCategoryDto(Brand brand) {
		this.id = brand.getId();
		this.name = brand.getName();
		this.imagePath = brand.getImagePath();
	}
}
