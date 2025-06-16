package com.splusz.villigo.dto;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserSignUpDto {
	@NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Pattern(regexp = "^[a-z0-9]{3,}$", message = "아이디는 영문 소문자 또는 영문 소문자,숫자 조합으로 3글자 이상이어야 합니다.")
    private String username;
	
	@NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{4,}$", 
             message = "비밀번호는 최소 4글자이며, 영문 대소문자, 숫자, 특수기호를 포함해야 합니다.")
    private String password;
	
	@NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
	private String passwordConfirm;
	
	@NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    private String nickname;
	
	@NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", 
             message = "이메일 형식이 올바르지 않습니다. (예: user@domain.com)")
    private String email;
	
	@NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식으로 입력해야 합니다.")
    private String phone;
    
	private String region;
	private Long themeId;
	private boolean marketingConsent; // 마케팅 동의 필드 추가
	
	public void validatePasswordMatch() {
	    if (!password.equals(passwordConfirm)) {
	        throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
	    }
	}
	
	// DTO객체를 엔터티(User) 객체로 변환
	public User toEntity(PasswordEncoder passwordEncoder, Theme theme) {
        return User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .email(email)
                .phone(phone) // 하이픈 포함된 전화번호 저장
                .region(region)
                .theme(theme)
                .marketingConsent(marketingConsent) // 마케팅 동의 설정
                .build();
    }
}
