package com.splusz.villigo.storage;

import java.io.InputStream;
import java.time.Duration;
import java.util.Map; // Map 임포트

public interface FileStorageService {
    // 서버에서 직접 업로드하는 경우: S3에 저장될 최종 키(경로 포함)를 직접 받습니다.
    String uploadFile(InputStream inputStream, String targetS3Key, String contentType) throws FileStorageException;

    // 클라이언트 직접 다운로드를 위한 Pre-signed URL 생성
    String generateDownloadPresignedUrl(String fileKey, Duration expiresIn) throws FileStorageException;

    // 클라이언트 직접 업로드를 위한 Pre-signed PUT URL 생성: S3에 저장될 최종 키(경로 포함)를 직접 받습니다.
    Map<String, String> generateUploadPresignedUrl(String targetS3Key, String contentType, Duration expiresIn) throws FileStorageException; // <--- originalFilename -> targetS3Key로 변경

    // 서버에서 파일 삭제
    void deleteFile(String fileKey) throws FileStorageException;
}