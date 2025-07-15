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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor // Lombok으로 생성자 자동 생성
public class S3FileStorageService implements FileStorageService { // FileStorageService 인터페이스 구현

    private final S3Client s3Client; // S3ClientConfig에서 정의한 S3Client Bean 주입
    private final S3Presigner s3Presigner; // S3PresignConfig에서 정의한 S3Presigner Bean 주입

    @Value("${aws.s3.bucket}") // 버킷 이름 주입
    private String bucketName;

    // 고유 파일명 생성 헬퍼 메서드
    public static String createUniqueFileName(String originalFileNameWithExtension) { // 파라미터명 명확화
        String extension = "";
        int dotIndex = originalFileNameWithExtension.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFileNameWithExtension.length() - 1) {
            extension = originalFileNameWithExtension.substring(dotIndex);
        }
        return UUID.randomUUID().toString() + extension;
    }


    // 파일 업로드 (서버 직접 업로드 방식)
    @Override
    public String uploadFile(InputStream inputStream, String targetS3Key, String contentType) throws FileStorageException {
        try {
            // S3에 저장될 최종 키는 이제 파라미터로 직접 받음
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(targetS3Key) // <--- S3 키로 targetS3Key 직접 사용
                    .contentType(contentType)
                    .build();
            
            // InputStream.available()는 IOException을 던질 수 있으므로, 정확한 파일 크기를 아는 경우 (예: MultipartFile.getSize())
            // 이를 파라미터로 받아 RequestBody.fromInputStream(inputStream, fileSize)를 사용하거나,
            // RequestBody.fromInputStream(inputStream, -1)을 사용하여 스트림의 끝까지 읽도록 할 수 있음
            // 현재는 inputStream.available()를 유지합니다. (컨트롤러에서 getSize()를 전달받도록 시그니처를 확장하는게 더 좋음)
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, inputStream.available()));
            
            return targetS3Key; // <--- S3에 저장된 최종 키를 반환
        } catch (S3Exception e) {
            throw new FileStorageException("S3에 파일 업로드 실패: " + targetS3Key, e);
        } catch (Exception e) { // InputStream.available()에서 발생할 수 있는 IOException 처리
            throw new FileStorageException("파일 업로드 중 알 수 없는 오류 발생: " + targetS3Key, e);
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
    public Map<String, String> generateUploadPresignedUrl(String targetS3Key, String contentType, Duration expiresIn) throws FileStorageException { // <--- 파라미터 변경
        try {
            // S3에 저장될 최종 키(경로 포함)는 이제 파라미터로 직접 받습니다.
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(targetS3Key) // <--- S3 키로 targetS3Key 직접 사용
                    .contentType(contentType)
                    .build();

            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(expiresIn)
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
            
            Map<String, String> response = new HashMap<>();
            response.put("presignedUrl", presignedRequest.url().toString());
            response.put("s3Key", targetS3Key); // <--- S3 Key도 함께 반환
            
            return response;
        } catch (S3Exception e) {
            throw new FileStorageException("Pre-signed 업로드 URL 생성 실패: " + targetS3Key, e);
        } catch (Exception e) {
            throw new FileStorageException("Pre-signed 업로드 URL 생성 중 알 수 없는 오류 발생: " + targetS3Key, e);
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