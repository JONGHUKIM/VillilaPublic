package com.splusz.villigo.service;

import com.splusz.villigo.storage.FileStorageException; // 예외 클래스 임포트
import com.splusz.villigo.storage.FileStorageService; // 인터페이스 임포트 (선택 사항이지만 좋은 설계)

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest; // 삭제용 임포트
import software.amazon.awssdk.services.s3.model.GetObjectRequest; // 다운로드용 임포트
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner; // Presigner 임포트
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest; // Presigned GET 요청 임포트
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest; // 생성된 Presigned GET 객체 임포트
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest; // Presigned PUT 요청 임포트
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest; // 생성된 Presigned PUT 객체 임포트

import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok으로 생성자 자동 생성
public class S3FileStorageService implements FileStorageService { // FileStorageService 인터페이스 구현

    private final S3Client s3Client; // S3ClientConfig에서 정의한 S3Client Bean 주입
    private final S3Presigner s3Presigner; // S3PresignConfig에서 정의한 S3Presigner Bean 주입

    @Value("${aws.s3.bucket}") // 버킷 이름 주입
    private String bucketName;

    // 고유 파일명 생성 헬퍼 메서드
    private String generateUniqueFileName(String originalFilename) {
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }

    // 파일 업로드 (서버 직접 업로드 방식)
    @Override
    public String uploadFile(InputStream inputStream, String originalFilename, String contentType) throws FileStorageException {
        try {
            String fileKey = generateUniqueFileName(originalFilename); // 고유 파일명 생성
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .build();
            // RequestBody.fromInputStream 사용 시 InputStream.available()이 IOException을 던질 수 있으므로
            // 정확한 길이를 알거나 또는 RequestBody.fromInputStream(inputStream, -1)을 사용하여 Stream을 명시적으로 소비하도록 해야 함
            // 또는 MultipartFile을 직접 받는 컨트롤러에서 file.getBytes()를 RequestBody.fromBytes()로 전달하는 것이 더 간단할 수 있음
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
            return fileKey;
        } catch (S3Exception e) {
            throw new FileStorageException("S3에 파일 업로드 실패: " + originalFilename, e);
        } catch (Exception e) { // InputStream.available() 등에서 발생할 수 있는 IOException 처리
            throw new FileStorageException("파일 업로드 중 알 수 없는 오류 발생: " + originalFilename, e);
        }
    }

    // 다운로드 Pre-signed URL 생성
    @Override
    public String generateDownloadPresignedUrl(String fileKey, Duration expiresIn) throws FileStorageException { 
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequest)
                    .signatureDuration(expiresIn) // <--- Duration 객체를 직접 사용
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();
        } catch (S3Exception e) {
            throw new FileStorageException("Pre-signed 다운로드 URL 생성 실패: " + fileKey, e);
        } catch (Exception e) {
            throw new FileStorageException("Pre-signed 다운로드 URL 생성 중 알 수 없는 오류 발생: " + fileKey, e);
        }
    }

    // 업로드 Pre-signed URL 생성 (클라이언트 직접 업로드용, 선택 사항)
    // Pre-signed PUT URL 예시
    @Override
    public String generateUploadPresignedUrl(String originalFilename, String contentType, Duration expiresIn) throws FileStorageException { // <--- 파라미터 타입 변경
        try {
            String fileKey = generateUniqueFileName(originalFilename);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiresIn) // <--- Duration 객체를 직접 사용
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            throw new FileStorageException("Pre-signed 업로드 URL 생성 실패: " + originalFilename, e);
        } catch (Exception e) {
            throw new FileStorageException("Pre-signed 업로드 URL 생성 중 알 수 없는 오류 발생: " + originalFilename, e);
        }
    }


    // 파일 삭제 (서버에서 직접 삭제)
    @Override
    public void deleteFile(String fileKey) throws FileStorageException {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
        } catch (S3Exception e) {
            throw new FileStorageException("S3에서 파일 삭제 실패: " + fileKey, e);
        } catch (Exception e) {
            throw new FileStorageException("파일 삭제 중 알 수 없는 오류 발생: " + fileKey, e);
        }
    }
}