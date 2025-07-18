package com.splusz.villigo.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
}
