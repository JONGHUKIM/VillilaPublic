package com.splusz.villigo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.dto.RentalImageDto;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalImageRepository;
import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalImageService {

	private final RentalImageRepository rentalImgRepo;
	private final ProductRepository prodRepo;
	private final FileStorageService fileStorageService; // S3FileStorageService 주입
	
    @Value("${file.upload-dir-mig}") // <--- 변경: application.properties의 file.upload-dir-mig 값을 주입
    private String rentalImageLocalPath; // <--- 변수명도 소문자로 변경

	// S3 기반 이미지 생성
	@Transactional
	public void create(Long productId, List<String> imageS3Keys) {
	    Product product = prodRepo.findById(productId)
	            .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 제품 ID: " + productId));

	    List<RentalImage> entities = new ArrayList<>();
	    for (String s3Key : imageS3Keys) {
	        if (s3Key == null || s3Key.trim().isEmpty()) {
	            log.warn("빈 S3 키 건너뜁니다: {}", s3Key);
	            continue;
	        }
	        if (!s3Key.startsWith("product_images/")) {
	            log.warn("잘못된 S3 키 형식: {}", s3Key);
	            throw new IllegalArgumentException("잘못된 S3 키 형식: " + s3Key);
	        }

	        RentalImage entity = RentalImage.builder()
	                .product(product)
	                .filePath(s3Key)
	                .build();
	        entities.add(entity);
	    }

	    if (!entities.isEmpty()) {
	        rentalImgRepo.saveAll(entities);
	        log.info("S3 키 {}개 저장 완료: productId={}", entities.size(), productId);
	    } else {
	        log.warn("저장할 S3 키가 없음: productId={}", productId);
	    }
	}
	public String generatePresignedUrl(String filePath) throws FileStorageException {
        return fileStorageService.generateDownloadPresignedUrl(filePath, Duration.ofHours(1));
    }

    public List<RentalImageDto> readByProductId(Long productId) {
        List<RentalImage> images = rentalImgRepo.findByProductId(productId);
        return images.stream().map(image -> {
            String imageUrl = "/images/default-image.png"; // 기본값
            try {
                imageUrl = fileStorageService.generateDownloadPresignedUrl(image.getFilePath(), Duration.ofHours(1));
            } catch (FileStorageException e) {
                log.warn("Pre-signed URL 생성 실패: filePath={}, 오류: {}", image.getFilePath(), e.getMessage());
            }
            return RentalImageDto.builder()
                    .imageId(image.getId())
                    .filePath(image.getFilePath())
                    .imageUrl(imageUrl)
                    .build();
        }).collect(Collectors.toList());
    }

	@Transactional
	public void deleteBeforeUpdate(List<Long> imageIdsForDelete) {
		List<RentalImage> images = rentalImgRepo.findAllById(imageIdsForDelete);

		for (RentalImage image : images) {
			String s3Key = image.getFilePath();
			try {
				fileStorageService.deleteFile(s3Key); // S3에서 파일 삭제
				log.info("S3 파일 삭제 성공: {}", s3Key);
			} catch (FileStorageException e) {
				log.warn("S3 파일 삭제 실패: {}, 오류: {}", s3Key, e.getMessage());
			}
		}

		rentalImgRepo.deleteAllByIdInBatch(imageIdsForDelete);
	}

	@Transactional
	public void deleteByProductId(Long productId) {
	    List<RentalImage> images = rentalImgRepo.findByProductId(productId); // filePath 직접 조회
	    List<Long> imageIds = images.stream().map(RentalImage::getId).collect(Collectors.toList());
	    List<String> failedKeys = new ArrayList<>();

	    for (RentalImage image : images) {
	        String s3Key = image.getFilePath();
	        try {
	            fileStorageService.deleteFile(s3Key);
	            log.info("S3 파일 삭제 성공: {}", s3Key);
	        } catch (FileStorageException e) {
	            log.warn("S3 파일 삭제 실패: {}, 오류: {}", s3Key, e.getMessage());
	            failedKeys.add(s3Key);
	        }
	    }

	    if (!imageIds.isEmpty()) {
	        rentalImgRepo.deleteAllByIdInBatch(imageIds);
	        log.info("DB 이미지 삭제 완료: productId={}", productId);
	    }

	    if (!failedKeys.isEmpty()) {
	        log.error("S3 파일 삭제 실패 키: {}", failedKeys);
	    }
	}
	
	 @Transactional
	    public int migrateLocalImagesToS3() throws IOException, FileStorageException {
	        // 모든 RentalImage 엔티티를 조회
	        List<RentalImage> allRentalImages = rentalImgRepo.findAll();
	        int migratedCount = 0;

	        for (RentalImage image : allRentalImages) {
	            String currentFilePath = image.getFilePath();

	            if (currentFilePath.startsWith("product_images/") || currentFilePath.startsWith("avatars/") || currentFilePath.startsWith("chat_images/")) {
	                log.info("migrateLocalImagesToS3: 이미 S3 경로로 보이는 파일이므로 건너뜁니다: {}", currentFilePath);
	                continue; 
	            }

	            Path localFilePath = Paths.get(rentalImageLocalPath).resolve(currentFilePath).normalize();

	            if (Files.exists(localFilePath) && Files.isReadable(localFilePath)) {
	                log.info("migrateLocalImagesToS3: 로컬 파일 발견: {}", localFilePath);

	                // 파일의 MIME 타입 추정
	                String contentType = Files.probeContentType(localFilePath);
	                if (contentType == null) {
	                    contentType = "application/octet-stream"; // 기본값
	                    log.warn("migrateLocalImagesToS3: 파일 {}의 Content-Type을 추정할 수 없어 application/octet-stream으로 설정합니다.", localFilePath);
	                }

	                String newS3Key = "product_images/" + S3FileStorageService.createUniqueFileName(currentFilePath);

	                try (InputStream inputStream = new FileInputStream(localFilePath.toFile())) {
	                    // S3에 파일 업로드
	                    fileStorageService.uploadFile(inputStream, newS3Key, contentType); // FileStorageService 사용
	                    log.info("migrateLocalImagesToS3: S3 업로드 성공: {} -> {}", localFilePath, newS3Key);

	                    // DB 업데이트: filePath를 S3 Key로 변경
	                    image.setFilePath(newS3Key);
	                    rentalImgRepo.save(image); // 트랜잭션 내에서 변경사항 저장
	                    log.info("migrateLocalImagesToS3: DB filePath 업데이트 성공: {} -> {}", currentFilePath, newS3Key);

	                    // 로컬 파일 삭제 (마이그레이션 완료 후 안전을 위해)
	                    try {
	                        Files.delete(localFilePath);
	                        log.info("migrateLocalImagesToS3: 로컬 파일 삭제 성공: {}", localFilePath);
	                    } catch (IOException e) {
	                        log.warn("migrateLocalImagesToS3: 로컬 파일 삭제 실패: {} - {}", localFilePath, e.getMessage());
	                    }

	                    migratedCount++;

	                } catch (IOException e) {
	                    log.error("migrateLocalImagesToS3: 로컬 파일 읽기 중 오류: {} - {}", localFilePath, e.getMessage(), e);
	                } catch (FileStorageException e) {
	                    log.error("migrateLocalImagesToS3: S3 업로드 중 오류 (FileStorageException): {} -> {} - {}", localFilePath, newS3Key, e.getMessage(), e);
	                } catch (Exception e) {
	                    log.error("migrateLocalImagesToS3: 이미지 마이그레이션 중 알 수 없는 오류: {} - {}", localFilePath, e.getMessage(), e);
	                }
	            } else {
	                log.warn("migrateLocalImagesToS3: 로컬 파일을 찾을 수 없거나 읽을 수 없습니다. 건너뜁니다: {}", localFilePath);
	            }
	        }

	        log.info("migrateLocalImagesToS3: 총 {}개의 상품 이미지 S3 마이그레이션 완료.", migratedCount);
	        return migratedCount;
	    }
}
