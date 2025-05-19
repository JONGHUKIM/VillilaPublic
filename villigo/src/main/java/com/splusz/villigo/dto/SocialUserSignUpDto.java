package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;

import lombok.Data;

@Data
public class SocialUserSignUpDto {
	private String nickname;
	private String phone;
	private String region;
	private Long themeId;
	
	// DTO객체를 엔터티(User) 객체로 변환
	public User toEntity(Theme theme) {
		return User.builder()
				.nickname(nickname)
				.phone(phone)
				.region(region)
				.theme(theme)
				.build();
	}
}
