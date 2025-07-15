package com.splusz.villigo.storage;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorageService {
    // 서버에서 직접 업로드하는 경우
    String uploadFile(InputStream inputStream, String originalFilename, String contentType) throws FileStorageException;

    // 클라이언트 직접 다운로드를 위한 Pre-signed URL 생성
    String generateDownloadPresignedUrl(String fileKey, Duration expiresIn) throws FileStorageException;

    // 클라이언트 직접 업로드를 위한 Pre-signed PUT URL 생성
    String generateUploadPresignedUrl(String originalFilename, String contentType, Duration expiresIn) throws FileStorageException;

    // 서버에서 파일 삭제
    void deleteFile(String fileKey) throws FileStorageException;
}