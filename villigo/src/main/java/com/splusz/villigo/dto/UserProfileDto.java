package com.splusz.villigo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
	private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String region;
    private String avatar;
    private String avatarImageUrl;
    private Long themeId;
    private String theme;
    private int jjamPoints;
    private int mannerScore;
    private String socialType;
}