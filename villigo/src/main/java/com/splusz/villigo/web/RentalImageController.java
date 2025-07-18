package com.splusz.villigo.web;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.service.RentalImageService;
import com.splusz.villigo.service.S3FileStorageService;
import com.splusz.villigo.storage.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class RentalImageController {

    private final S3FileStorageService s3FileStorageService;

    @Value("${aws.s3.bucket}")
    private String bucketName;
    
    private final RentalImageService rentalImageService; 

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
    
    @GetMapping("/migrate-to-s3") // 새로운 엔드포인트 경로
    public ResponseEntity<String> migrateProductImagesToS3() {
        log.warn("🚨 상품 이미지 S3 마이그레이션 API 호출됨!");
        try {
            int migratedCount = rentalImageService.migrateLocalImagesToS3(); // RentalImageService의 새 메서드 호출
            return ResponseEntity.ok("상품 이미지 S3 마이그레이션 완료. " + migratedCount + "개 처리됨.");
        } catch (Exception e) {
            log.error("상품 이미지 S3 마이gration 실패: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("상품 이미지 S3 마이그레이션 실패: " + e.getMessage());
        }
    }
    
    
}
