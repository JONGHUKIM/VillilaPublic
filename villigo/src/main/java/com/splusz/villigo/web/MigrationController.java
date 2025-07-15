package com.splusz.villigo.web;

import com.splusz.villigo.service.MigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // 권한 부여를 위해
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/migrate") // 관리자만 접근 가능한 경로
@RequiredArgsConstructor
@Slf4j
public class MigrationController {

    private final MigrationService migrationService;

    // 아바타 마이그레이션 실행 API (관리자 권한 필요)
    @PostMapping("/avatars")
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한이 있는 사용자만 접근 가능
    public ResponseEntity<String> migrateAvatars() {
        try {
            int count = migrationService.migrateUserAvatarsToS3();
            return ResponseEntity.ok(count + "개의 아바타 이미지가 성공적으로 S3로 마이그레이션되었습니다.");
        } catch (Exception e) {
            log.error("아바타 이미지 마이그레이션 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("아바타 이미지 마이그레이션 중 오류 발생: " + e.getMessage());
        }
    }

    // 채팅 이미지 마이그레이션 실행 API (관리자 권한 필요)
    @PostMapping("/chat-images")
    @PreAuthorize("hasRole('ADMIN')") // ADMIN 권한이 있는 사용자만 접근 가능
    public ResponseEntity<String> migrateChatImages() {
        try {
            int count = migrationService.migrateChatImagesToS3();
            return ResponseEntity.ok(count + "개의 채팅 이미지가 성공적으로 S3로 마이그레이션되었습니다.");
        } catch (Exception e) {
            log.error("채팅 이미지 마이그레이션 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("채팅 이미지 마이그레이션 중 오류 발생: " + e.getMessage());
        }
    }
}