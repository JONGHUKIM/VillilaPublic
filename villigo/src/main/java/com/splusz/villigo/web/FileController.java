package com.splusz.villigo.web;

import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.service.S3FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map; // Map 임포트

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3FileStorageService s3FileStorageService;

    public FileController(S3FileStorageService s3FileStorageService) {
        this.s3FileStorageService = s3FileStorageService;
    }

    // 파일 업로드 API (서버 직접 업로드 방식)
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드할 파일이 없습니다.");
        }
        try {
            // S3에 저장될 최종 키(경로 포함)를 여기서 만듭니다. 예: temp_uploads/UUID.확장자
            String targetS3Key = "temp_uploads/" + S3FileStorageService.createUniqueFileName(file.getOriginalFilename());

            // S3 업로드 서비스 호출 (이제 targetS3Key를 직접 받음)
            String fileKey = s3FileStorageService.uploadFile(
                file.getInputStream(),
                targetS3Key, // <--- 최종 S3 키 전달
                file.getContentType()
            );
            // 여기에 파일 메타데이터를 DB에 저장하는 로직 추가
            // (이 컨트롤러는 범용이므로, DB 저장은 다른 서비스에서 fileKey를 받아 처리할 수도 있습니다.)
            return ResponseEntity.ok("파일 업로드 성공! S3 키: " + fileKey);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("파일 데이터 처리 중 오류 발생: " + e.getMessage());
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("S3 업로드 서비스 오류: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("파일 업로드 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    // 다운로드 Pre-signed URL 발급 API
    @GetMapping("/download-url")
    public ResponseEntity<String> getDownloadUrl(@RequestParam("fileKey") String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("파일 키는 필수입니다.");
        }
        try {
            String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(fileKey, Duration.ofMinutes(5));
            return ResponseEntity.ok(presignedUrl);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Pre-signed 다운로드 URL 생성 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("다운로드 URL 생성 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    // 업로드 Pre-signed URL 발급 API
    @PostMapping("/upload-url")
    public ResponseEntity<Map<String, String>> getUploadUrl(
        @RequestParam("filename") String targetS3Key,
        @RequestParam("contentType") String contentType) {

        if (targetS3Key == null || targetS3Key.trim().isEmpty() || contentType == null || contentType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "파일 이름(경로 포함)과 Content-Type은 필수입니다."));
        }
        try {
            // 프론트엔드에서 보낸 filename (이미 chat_images/채팅방ID/UUID.확장자 형태임)을 그대로 S3 키로 사용
            // 여기서 다시 고유 파일명을 만들거나 temp_uploads/ 를 붙일 필요 없음
            Map<String, String> presignedUrlData = s3FileStorageService.generateUploadPresignedUrl(targetS3Key, contentType, Duration.ofMinutes(5));
            return ResponseEntity.ok(presignedUrlData);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "Pre-signed 업로드 URL 생성 실패: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("message", "업로드 URL 생성 중 알 수 없는 오류 발생: " + e.getMessage()));
        }
    }

    // 파일 삭제 API
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("fileKey") String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("파일 키는 필수입니다.");
        }
        try {
            s3FileStorageService.deleteFile(fileKey);
            return ResponseEntity.ok("파일 삭제 성공: " + fileKey);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("S3 삭제 서비스 오류: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("파일 삭제 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }
}