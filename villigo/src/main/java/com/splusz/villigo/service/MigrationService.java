package com.splusz.villigo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatMessageType;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.ChatMessageRepository;
import com.splusz.villigo.repository.UserRepository;
import com.splusz.villigo.storage.FileStorageException; // FileStorageException 임포트
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // StringUtils 임포트

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional; // Optional 임포트
import java.util.UUID; // UUID 임포트

@Service
@RequiredArgsConstructor
@Slf4j
public class MigrationService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final S3FileStorageService s3FileStorageService; // S3 서비스 주입
    private final ObjectMapper objectMapper; // JSON 처리를 위해 주입

    @Value("${file.upload-dir}") // application.properties에서 로컬 업로드 경로 가져오기
    private String baseUploadDir;

    // --- 1. 아바타 이미지 마이그레이션 ---
    @Transactional // DB 업데이트가 있으므로 트랜잭션 필요
    public int migrateUserAvatarsToS3() {
        log.info("--- 아바타 이미지 S3 마이그레이션 시작 ---");
        int migratedCount = 0;

        List<User> usersWithAvatars = userRepository.findAll(); // 모든 사용자 조회 (또는 avatar가 null이 아닌 사용자만 조회하는 레포지토리 메서드 사용)

        for (User user : usersWithAvatars) {
            String currentAvatarS3Key = user.getAvatar(); // 현재 DB에 저장된 아바타 경로

            // 이미 S3 키로 보이는 경우 (예: "avatars/...")는 건너뜁니다.
            if (StringUtils.hasText(currentAvatarS3Key) && currentAvatarS3Key.startsWith("avatars/")) {
                log.info("사용자 {} (ID: {})의 아바타 '{}'는 이미 S3 키로 보입니다. 건너뜁니다.", user.getUsername(), user.getId(), currentAvatarS3Key);
                continue;
            }

            // DB에 로컬 경로가 저장되어 있다고 가정
            String localFileName = currentAvatarS3Key; // DB의 avatar 컬럼에 로컬 파일명만 저장되어 있었다고 가정

            if (!StringUtils.hasText(localFileName)) {
                log.info("사용자 {} (ID: {})에게 아바타가 설정되어 있지 않습니다. 건너뜁니다.", user.getUsername(), user.getId());
                continue;
            }

            Path localFilePath = Paths.get(baseUploadDir, "avatar", localFileName); // 로컬 아바타 경로 재구성
            File localFile = localFilePath.toFile();

            if (!localFile.exists() || !localFile.isFile()) {
                log.warn("사용자 {} (ID: {})의 로컬 아바타 파일 '{}'을 찾을 수 없습니다. DB 업데이트 스킵.", user.getUsername(), user.getId(), localFilePath);
                continue;
            }

            try (InputStream inputStream = new FileInputStream(localFile)) {
                String originalFileNameWithExt = localFile.getName(); // 확장자를 포함한 원본 파일명
                String newS3Key = "avatars/" + user.getId() + "/" + S3FileStorageService.createUniqueFileName(originalFileNameWithExt); // S3 키 생성
                String contentType = Files.probeContentType(localFilePath); // 파일 MIME 타입 감지

                if (contentType == null) {
                    contentType = "application/octet-stream"; // 기본 MIME 타입
                    log.warn("파일 '{}'의 MIME 타입을 감지할 수 없습니다. 기본값 '{}' 사용.", localFilePath, contentType);
                }

                // S3에 업로드
                String uploadedS3Key = s3FileStorageService.uploadFile(inputStream, newS3Key, contentType);

                if (uploadedS3Key != null) {
                    // DB 업데이트: avatar 컬럼을 새로운 S3 키로 변경
                    user.setAvatar(uploadedS3Key);
                    userRepository.save(user);
                    log.info("✅ 아바타 마이그레이션 성공: 사용자 {} (ID: {}), '{}' -> '{}'. DB 업데이트 완료.", user.getUsername(), user.getId(), localFilePath, uploadedS3Key);
                    migratedCount++;

                    // 선택 사항: 로컬 파일 삭제 (마이그레이션 후 안전하다면)
                    // Files.delete(localFilePath);
                    // log.info("로컬 아바타 파일 삭제됨: {}", localFilePath);
                } else {
                    log.error("❌ 아바타 S3 업로드 실패: 사용자 {} (ID: {}), '{}'. DB 업데이트 건너뜠습니다.", user.getUsername(), user.getId(), localFilePath);
                }

            } catch (IOException e) {
                log.error("아바타 파일 읽기/MIME 타입 감지 중 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
            } catch (FileStorageException e) {
                log.error("아바타 S3 서비스 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
            } catch (Exception e) {
                log.error("아바타 마이그레이션 중 알 수 없는 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
            }
        }
        log.info("--- 아바타 이미지 S3 마이그레이션 완료. 총 {}개 마이그레이션됨 ---", migratedCount);
        return migratedCount;
    }

    // --- 2. 채팅 이미지 마이그레이션 ---
    @Transactional // DB 업데이트가 있으므로 트랜잭션 필요
    public int migrateChatImagesToS3() {
        log.info("--- 채팅 이미지 S3 마이그레이션 시작 ---");
        int migratedCount = 0;

        List<ChatMessage> imageMessages = chatMessageRepository.findByMessageTypeIn(List.of(ChatMessageType.IMAGE, ChatMessageType.IMAGE_GROUP));

        for (ChatMessage message : imageMessages) {
            String currentContent = message.getContent();
            
            // 이미 S3 키로 보이는 경우 건너뛰는 로직
            if (message.getMessageType() == ChatMessageType.IMAGE_GROUP) {
                try {
                    List<String> urlsInContent = objectMapper.readValue(currentContent, List.class);
                    if (!urlsInContent.isEmpty() && urlsInContent.get(0).startsWith("chat_images/")) {
                         log.info("메시지 {} (채팅방 ID: {})의 이미지 그룹은 이미 S3 키로 보입니다. 건너뜠습니다.", message.getId(), message.getChatRoomId());
                         continue;
                    }
                } catch (JsonProcessingException e) { // JSON 파싱 오류 처리
                    log.warn("메시지 {}의 IMAGE_GROUP content가 유효한 JSON이 아닙니다: {}. 건너뜁니다.", message.getId(), currentContent);
                    continue;
                }
            } else if (message.getMessageType() == ChatMessageType.IMAGE) {
                if (StringUtils.hasText(currentContent) && currentContent.startsWith("chat_images/")) {
                    log.info("메시지 {} (채팅방 ID: {})의 이미지는 이미 S3 키로 보입니다. 건너뜠습니다.", message.getId(), message.getChatRoomId());
                    continue;
                }
            }

            List<String> localFileNamesToProcess = new ArrayList<>();
            if (message.getMessageType() == ChatMessageType.IMAGE) {
                localFileNamesToProcess.add(currentContent);
            } else if (message.getMessageType() == ChatMessageType.IMAGE_GROUP) {
                try {
                    localFileNamesToProcess.addAll(objectMapper.readValue(currentContent, List.class));
                } catch (JsonProcessingException e) { // JSON 파싱 오류 처리
                    log.error("메시지 {} (채팅방 ID: {})의 IMAGE_GROUP content JSON 파싱 실패: {}. 오류: {}", message.getId(), message.getChatRoomId(), e.getMessage(), e);
                    continue;
                }
            }

            List<String> uploadedS3Keys = new ArrayList<>();
            for (String localFileName : localFileNamesToProcess) {
                Path localFilePath = Paths.get(baseUploadDir, "chat", localFileName);
                File localFile = localFilePath.toFile();

                if (!localFile.exists() || !localFile.isFile()) {
                    log.warn("메시지 {} (채팅방 ID: {})의 로컬 채팅 이미지 '{}'을 찾을 수 없습니다. 스킵.", message.getId(), message.getChatRoomId(), localFilePath);
                    continue;
                }

                try (InputStream inputStream = new FileInputStream(localFile)) {
                    String originalFileNameWithExt = localFile.getName();
                    String newS3Key = "chat_images/" + message.getChatRoomId() + "/" + S3FileStorageService.createUniqueFileName(originalFileNameWithExt);
                    String contentType = Files.probeContentType(localFilePath);

                    if (contentType == null) {
                        contentType = "application/octet-stream";
                        log.warn("파일 '{}'의 MIME 타입을 감지할 수 없습니다. 기본값 '{}' 사용.", localFilePath, contentType);
                    }

                    String uploadedKey = s3FileStorageService.uploadFile(inputStream, newS3Key, contentType);

                    if (uploadedKey != null) {
                        uploadedS3Keys.add(uploadedKey);
                        // 선택 사항: 로컬 파일 삭제 (마이그레이션 후 안전하다면)
                        // Files.delete(localFilePath);
                        // log.info("로컬 채팅 이미지 파일 삭제됨: {}", localFilePath);
                    } else {
                        log.error("❌ 채팅 이미지 S3 업로드 실패: 메시지 {} (채팅방 ID: {}), '{}'.", message.getId(), message.getChatRoomId(), localFilePath);
                    }
                } catch (IOException e) {
                    log.error("채팅 이미지 파일 읽기/MIME 타입 감지 중 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
                } catch (FileStorageException e) {
                    log.error("채팅 이미지 S3 서비스 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
                } catch (Exception e) { // catch IOException and other unexpected exceptions
                    log.error("채팅 이미지 마이그레이션 중 알 수 없는 오류: {}. 오류: {}", localFilePath, e.getMessage(), e);
                }
            }

            if (!uploadedS3Keys.isEmpty()) {
                String newContentForDb;
                try { // <--- 이 try-catch 블록 추가
                    if (message.getMessageType() == ChatMessageType.IMAGE_GROUP) {
                        newContentForDb = objectMapper.writeValueAsString(uploadedS3Keys); // JSON 배열로 저장
                    } else { // IMAGE
                        newContentForDb = uploadedS3Keys.get(0); // 단일 이미지이므로 첫 번째 키
                    }
                    message.setContent(newContentForDb);
                    chatMessageRepository.save(message); // DB 업데이트
                    log.info("✅ 채팅 메시지 마이그레이션 성공: 메시지 {} (채팅방 ID: {}), content 업데이트됨.", message.getId(), message.getChatRoomId());
                    migratedCount++;
                } catch (JsonProcessingException e) { // <--- JsonProcessingException 처리
                    log.error("❌ 마이그레이션된 S3 키를 JSON으로 변환 중 오류: 메시지 {} (채팅방 ID: {}). 오류: {}", message.getId(), message.getChatRoomId(), e.getMessage(), e);
                    // 여기서 DB 업데이트는 실패하므로, 이 메시지는 건너뛰고 다음 메시지로 진행
                }
            } else {
                log.warn("메시지 {} (채팅방 ID: {})의 이미지 업로드된 S3 키가 없어 DB 업데이트 건너뜁니다.", message.getId(), message.getChatRoomId());
            }
        }
        log.info("--- 채팅 이미지 S3 마이그레이션 완료. 총 {}개 마이그레이션됨 ---", migratedCount);
        return migratedCount;
    }
}