package com.splusz.villigo.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UpdateAvatarRequestDto {
    private MultipartFile avatarFile; // 업로드할 프로필 이미지 파일
}