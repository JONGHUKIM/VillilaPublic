package com.splusz.villigo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDto {

    // 후기 등록용
    private Long targetId;
    private Long reservationId;
    private String content; 
    private Long keywordId;
    private int isOwner; // 디폴트: 0, 상품 주인이 작성한 후기: 1

    // 후기 조회용
    private Long userId;
    private String userName;
    private String userImage;
    private String userImageUrl;
    private int score; // 후기 별점 점수
    private int mannerScore; // 추가된 필드

    @Builder
    public ReviewDto(Long targetId, Long reservationId, String content, int isOwner, Long keywordId,
                     Long userId, String userName, String userImage, int score, int mannerScore) {
        this.targetId = targetId;
        this.reservationId = reservationId;
        this.content = content;
        this.isOwner = isOwner; // 새로 추가한 필드
        this.keywordId = keywordId;
        this.userId = userId;
        this.userName = userName;
        this.userImage = userImage;
        this.score = score;
        this.mannerScore = mannerScore; // 추가된 필드
    }
}
