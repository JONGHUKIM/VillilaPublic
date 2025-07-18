package com.splusz.villigo.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.service.S3FileStorageService;
import com.splusz.villigo.storage.FileStorageException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class RentalImageController {

    private final S3FileStorageService s3FileStorageService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public RentalImageController(S3FileStorageService s3FileStorageService) {
        this.s3FileStorageService = s3FileStorageService;
    }

    @GetMapping("/{imageName:.+}")
    public ResponseEntity<String> getRentalImage(@PathVariable String imageName) {
        log.info("GET /api/images/{}", imageName);
        try {
            // S3에서 다운로드용 pre-signed URL 생성 (예: product_images/ 접두어 추가)
            String s3Key = "product_images/" + imageName; // S3 키 구조에 맞게 조정
            String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(s3Key, Duration.ofHours(1));
            return ResponseEntity.ok(presignedUrl);
        } catch (FileStorageException e) {
            log.error("Failed to load image from S3: {}", imageName, e);
            return ResponseEntity.badRequest().body("이미지 로드 실패: " + e.getMessage());
        }
    }
}
