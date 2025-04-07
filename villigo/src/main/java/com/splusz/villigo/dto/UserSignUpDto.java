package com.splusz.villigo.dto;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;

import lombok.Data;

@Data
public class UserSignUpDto {
	private String username;
	private String password;
	private String nickname;
	private String email;
	private String phone;
	private String region;
	private Long themeId;
	
	// DTO객체를 엔터티(User) 객체로 변환
	public User toEntity(PasswordEncoder passwordEncoder, Theme theme) {
		return User.builder()
				.username(username)
				.password(passwordEncoder.encode(password))
				.nickname(nickname)
				.email(email)
				.phone(phone)
				.region(region)
				.theme(theme)
				.build();
	}
}
