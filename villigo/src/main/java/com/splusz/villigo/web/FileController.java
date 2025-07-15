package com.splusz.villigo.web;

import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.service.S3FileStorageService; // S3FileStorageService 주입
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; // 파일 업로드를 위한 MultipartFile 임포트

import java.io.IOException; // IOException 임포트
import java.time.Duration;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final S3FileStorageService s3FileStorageService; // S3FileStorageService 주입

    public FileController(S3FileStorageService s3FileStorageService) {
        this.s3FileStorageService = s3FileStorageService;
    }

    // 파일 업로드 API (서버 직접 업로드 방식) 
    // 클라이언트에서 파일을 받아서 서버가 S3로 직접 업로드
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드할 파일이 없습니다.");
        }
        try {
            // S3 업로드 서비스 호출
            String fileKey = s3FileStorageService.uploadFile(
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType()
            );
            // 여기에 파일 메타데이터를 DB에 저장하는 로직 추가
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
    // 클라이언트가 이 URL을 받아서 S3에 직접 다운로드 요청
    @GetMapping("/download-url")
    public ResponseEntity<String> getDownloadUrl(@RequestParam("fileKey") String fileKey) {
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("파일 키는 필수입니다.");
        }
        try {
            // 5분 유효한 Duration 객체 생성
            String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(fileKey, Duration.ofMinutes(5)); // <--- 호출 방식 변경
            return ResponseEntity.ok(presignedUrl);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Pre-signed 다운로드 URL 생성 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("다운로드 URL 생성 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    // 업로드 Pre-signed URL 발급 API (선택 사항)
    // 클라이언트가 이 URL을 받아서 S3에 직접 업로드 요청
    @PostMapping("/upload-url")
    public ResponseEntity<String> getUploadUrl(@RequestParam("filename") String filename,
                                               @RequestParam("contentType") String contentType) {
        if (filename == null || filename.trim().isEmpty() || contentType == null || contentType.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("파일 이름과 Content-Type은 필수입니다.");
        }
        try {
            // 5분 유효한 Duration 객체 생성
            String presignedUrl = s3FileStorageService.generateUploadPresignedUrl(filename, contentType, Duration.ofMinutes(5));
            return ResponseEntity.ok(presignedUrl);
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Pre-signed 업로드 URL 생성 실패: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("업로드 URL 생성 중 알 수 없는 오류 발생: " + e.getMessage());
        }
    }

    // 파일 삭제 API (서버 직접 삭제)
    // 클라이언트가 서버에 삭제 요청, 서버가 S3에서 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteFile(@RequestParam("fileKey") String fileKey) {
        // TODO: fileKey에 대한 보안/인가 로직 추가 (현재 로그인한 사용자가 삭제할 권한이 있는지 확인)
        if (fileKey == null || fileKey.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("파일 키는 필수입니다.");
        }
        try {
            s3FileStorageService.deleteFile(fileKey);
            // 여기에 파일 메타데이터를 DB에서 삭제하는 로직 추가
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