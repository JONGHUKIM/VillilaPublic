package com.splusz.villigo.dto;

import lombok.Data;

@Data
public class UserProfileDto {
	private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String region;
    private String avatar;
    private Long themeId;
    private String theme;
    private int jjamPoints;
    private int mannerScore;
}