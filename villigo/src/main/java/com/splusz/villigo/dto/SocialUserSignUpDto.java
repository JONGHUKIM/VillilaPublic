package com.splusz.villigo.dto;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SocialUserSignUpDto {
    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    private String nickname;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "전화번호는 010-1234-5678 형식으로 입력해야 합니다.")
    private String phone;

    @NotBlank(message = "지역은 필수 입력 항목입니다.")
    private String region;

    private Long themeId;
    
    private boolean marketingConsent; // 마케팅 동의 필드 추가

    public User toEntity(Theme theme) {
        return User.builder()
                .nickname(nickname)
                .phone(phone)
                .region(region)
                .theme(theme)
                .marketingConsent(marketingConsent) // 마케팅 동의 설정
                .build();
    }
}