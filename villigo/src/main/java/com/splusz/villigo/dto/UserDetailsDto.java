package com.splusz.villigo.dto;

import java.util.List;

import lombok.Data;

@Data
public class UserDetailsDto {
    private Long id;                  // 유저 ID (users.id)
    private String nickname;          // 유저 닉네임 (users.nickname)
    private String region;            // 활동 지역 (users.region)
    private String avatar;            // 프로필 이미지 (users.avatar)
    private String interestCategory;  // 관심 상품 카테고리 (가정)
    private List<PostSummaryDto> posts;  // 유저가 올린 상품 목록
    private List<ReviewDto> reviews;     // 유저에 대한 후기 목록
    private int mannerScore;
}
