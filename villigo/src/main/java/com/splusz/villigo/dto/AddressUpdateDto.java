package com.splusz.villigo.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class AddressUpdateDto {

	private String sido;
	private String fullAddress;
	private String sigungu;
	private int zonecode;
	private String bname;
	private Double longitude;
	private Double latitude;
}
