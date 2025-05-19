package com.splusz.villigo.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.dto.RentalImageCreateDto;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RentalImageService {

    private final RentalImageRepository rentalImgRepo;
    private final ProductRepository prodRepo;
    private final String rentalImagePath = "/home/ubuntu/images/rentals";

    public Integer create(Long productId, RentalImageCreateDto dto) throws IOException {
        for(MultipartFile image : dto.getImages())
            {
                if (image.isEmpty()) {
                    log.info("빈 파일 건너뜁니다.");
                    continue;
                }
                
                if (image.getSize() > 10 * 1024 * 1024) {
                    throw new IllegalArgumentException("파일 용량이 너무 큽니다.");
                }
                // 저장업로드 시간 숫자형식으로만 이미지에 파일명 추가용임!
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
                String formattedDateTime = now.format(formatter);

                String FileName = image.getOriginalFilename();
                String extension = FileName.substring(FileName.lastIndexOf("."), FileName.length());
                String uuid = UUID.randomUUID().toString();
                String changedFileName = uuid + "-" + formattedDateTime + extension;
                Path uploadPath = Paths.get(rentalImagePath);
                if(!Files.exists(uploadPath)) {
                    Files.createDirectories(uploadPath);
                }

                Path targetPath = uploadPath.resolve(changedFileName).normalize();

                image.transferTo(targetPath);

                Product product = prodRepo.findById(productId).orElseThrow();
                
                RentalImage entity = RentalImage.builder()
                    .product(product)
                    .filePath(changedFileName)
                    .build();

                rentalImgRepo.save(entity);
                
            }
        return 0;
    }

    public List<RentalImage> readByProductId(Long productId) {
        List<RentalImage> rentalImages = rentalImgRepo.findByProductId(productId);
        return rentalImages;
    }

    @Transactional
    public void deleteBeforeUpdate(List<Long> imageIdsForDelete) {

        List<RentalImage> images = rentalImgRepo.findAllById(imageIdsForDelete);
        
        for(RentalImage image : images) {
        	log.info("image = {}", image.toString());
        }

        for (RentalImage image : images) {
            String filename = image.getFilePath();
            File file = new File(rentalImagePath, filename);
    
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.info("파일 삭제 성공: {}", file.getAbsolutePath());
                } else {
                    log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
                }
            } else {
                log.warn("파일이 존재하지 않음: {}", file.getAbsolutePath());
            }
        }
        
        rentalImgRepo.deleteAllByIdInBatch(imageIdsForDelete);
    }
    
    @Transactional
    public void deleteByProductId(Long productId) {
    	
    	List<Long> imageIds = rentalImgRepo.findIdByProductId(productId);
    	List<RentalImage> images = rentalImgRepo.findAllById(imageIds);
    	
    	for (RentalImage image : images) {
    		String filename = image.getFilePath();
    		File file = new File(rentalImagePath, filename);
    		
    		if (file.exists()) {
    			boolean deleted = file.delete();
    			if (deleted) {
    				log.info("파일 삭제 성공: {}", file.getAbsolutePath());
    			} else {
    				log.warn("파일 삭제 실패: {}", file.getAbsolutePath());
    			}
    		} else {
    			log.warn("파일이 존재하지 않음: {}", file.getAbsolutePath());
    		}
    	}
    	
        rentalImgRepo.deleteAllByIdInBatch(imageIds);
    }


}
