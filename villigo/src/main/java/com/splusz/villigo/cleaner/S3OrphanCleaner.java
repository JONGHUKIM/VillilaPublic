package com.splusz.villigo.cleaner; // 혹은 com.splusz.villigo.component

import com.splusz.villigo.repository.UserRepository;
import com.splusz.villigo.repository.RentalImageRepository;
import com.splusz.villigo.repository.ChatMessageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class S3OrphanCleaner {
    private static final Logger log = LoggerFactory.getLogger(S3OrphanCleaner.class);

    private final S3CleanupService s3CleanupService;
    private final UserRepository userRepository;
    private final RentalImageRepository rentalImageRepository;
    private final ChatMessageRepository chatMessageRepository;

    private final String bucketName;
    private final Region s3Region; // S3Client에서 region 정보 가져옴

    private static final String EXCLUDE_KEY = "prod.env";

    private static final List<String> TARGET_FOLDERS = List.of(
        "avatars/",
        "product_images/",
        "chat_images/"
    );

    private static final Pattern S3_KEY_PATTERN = Pattern.compile("chat_images/[^\"'\\]]*");

    public S3OrphanCleaner(
        S3CleanupService s3CleanupService,
        UserRepository userRepository,
        RentalImageRepository rentalImageRepository,
        ChatMessageRepository chatMessageRepository,
        @Value("${aws.s3.bucket}") String bucketName,
        S3Client s3Client // S3Client에서 Region 정보를 가져오기 위해 주입
    ) {
        this.s3CleanupService = s3CleanupService;
        this.userRepository = userRepository;
        this.rentalImageRepository = rentalImageRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.bucketName = bucketName;
        this.s3Region = s3Client.serviceClientConfiguration().region(); // S3Client에서 Region 가져옴
    }

    // DB에 저장된 S3 키가 완전한 URL 형태일 경우 객체 키로 변환하는 헬퍼 메서드
    private String convertToS3Key(String urlOrKey) {
        if (urlOrKey == null || urlOrKey.isEmpty()) {
            return null;
        }
        String standardUrlPrefix = String.format("https://%s.s3.%s.amazonaws.com/", bucketName, s3Region.id());
        if (urlOrKey.startsWith(standardUrlPrefix)) {
            return urlOrKey.substring(standardUrlPrefix.length());
        }
        String virtualHostedPrefix = String.format("https://%s.s3.amazonaws.com/", bucketName);
        if (urlOrKey.startsWith(virtualHostedPrefix)) {
            return urlOrKey.substring(virtualHostedPrefix.length());
        }
        return urlOrKey; // 이미 키 형태인 경우
    }


    public void cleanupAllFolders(boolean dryRun) {
        log.info("S3 고아 이미지 정리 작업 시작 (Dry Run: {})", dryRun);

        Set<String> allS3Keys = new HashSet<>();
        // 모든 대상 폴더의 S3 객체 목록을 먼저 가져옴
        for (String prefix : TARGET_FOLDERS) {
            Set<String> keysInPrefix = s3CleanupService.listAllKeys(prefix);
            allS3Keys.addAll(keysInPrefix);
        }
        log.info("S3 버킷에서 총 {}개의 잠재적 이미지 객체 키를 가져왔습니다.", allS3Keys.size());


        Set<String> dbReferencedKeys = new HashSet<>();

        // 1. User 아바타 이미지
        userRepository.findAllAvatarKeys().stream()
            .map(this::convertToS3Key)
            .filter(Objects::nonNull)
            .forEach(dbReferencedKeys::add);
        log.debug("DB 참조 아바타 이미지 키 {}개.", userRepository.findAllAvatarKeys().size());


        // 2. Rental 이미지
        rentalImageRepository.findAllRentalImageKeys().stream()
            .map(this::convertToS3Key)
            .filter(Objects::nonNull)
            .forEach(dbReferencedKeys::add);
        log.debug("DB 참조 렌탈 이미지 키 {}개.", rentalImageRepository.findAllRentalImageKeys().size());


        // 3. Chat 메시지 이미지
        chatMessageRepository.findAllChatImageContents().stream()
            .forEach(content -> {
                // content가 JSON 배열 형태일 수 있으므로 파싱
                // 예: ["chat_images/path1.png", "chat_images/path2.png"]
                // 또는 단일 이미지 경로: "chat_images/path1.png"
                Matcher matcher = S3_KEY_PATTERN.matcher(content);
                while (matcher.find()) {
                    String key = matcher.group();
                    dbReferencedKeys.add(convertToS3Key(key)); // 혹시 URL 형태일까봐 변환
                }

            });
        
        log.debug("DB 참조 채팅 이미지 키 {}개.", chatMessageRepository.findAllChatImageContents().size());


        log.info("데이터베이스에서 총 {}개의 참조된 이미지 경로를 가져왔습니다.", dbReferencedKeys.size());

        // 고아 이미지 식별 (S3에만 있고 DB에는 없는 이미지)
        Set<String> orphanKeys = new HashSet<>(allS3Keys);
        orphanKeys.removeAll(dbReferencedKeys);

        // prod.env는 S3CleanupService에서 이미 걸렀으므로 여기서는 다시 걸 필요는 없음.
        orphanKeys.remove(EXCLUDE_KEY); // 혹시 모를 상황 대비 재확인

        if (orphanKeys.isEmpty()) {
            log.info("정리할 고아 이미지가 없습니다.");
            return;
        }

        List<String> sortedOrphanKeys = orphanKeys.stream().sorted().collect(Collectors.toList());

        log.warn("총 {}개의 고아 이미지가 발견되었습니다.", sortedOrphanKeys.size());
        if (!dryRun) {
            log.info("고아 이미지 삭제를 시작합니다...");
            s3CleanupService.deleteKeysBatch(sortedOrphanKeys);
            log.info("고아 이미지 삭제가 완료되었습니다.");
        } else {
            log.info("Dry Run 모드이므로 실제로 삭제하지 않습니다. 삭제될 고아 이미지 목록:");
            sortedOrphanKeys.forEach(key -> log.warn("Dry Run - Orphan: {}", key));
        }
    }
}