package com.splusz.villigo.web;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.dto.SearchedProductDto;
import com.splusz.villigo.dto.SelectBrandsByCategoryDto;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.SearchService;
import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.storage.FileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class SearchRestController {

    private final SearchService searServ;
    private final ProductService prodServ;
    private final FileStorageService fileStorageService; // S3 서비스 주입

    @PostMapping("/search")
    public ResponseEntity<Page<SearchedProductDto>> searching(@RequestBody Map<String, List<String>> filters) {
        log.info("filters={}", filters);
        Page<SearchedProductDto> searchedProducts = searServ.searchProduct(filters);

        // S3 pre-signed URL 생성
        searchedProducts.getContent().forEach(product -> {
            if (product.getFilePath() != null) {
            	try {
                    String presignedUrl = fileStorageService.generateDownloadPresignedUrl(product.getFilePath(), Duration.ofHours(1));
                    log.info("Generated presigned URL for productId {}: {}", product.getId(), presignedUrl);
                    product.setFilePath(presignedUrl);
                } catch (FileStorageException e) {
                    log.error("S3 pre-signed URL 생성 실패 for productId {}: {}", product.getId(), e.getMessage(), e);
                    product.setFilePath("/images/placeholder.jpg");
                }
            }
        });

        return ResponseEntity.ok(searchedProducts);
    }
    
    @PostMapping("/brand")
    public ResponseEntity<List<SelectBrandsByCategoryDto>> selectBrandsByCategory(@RequestBody Map<String, Long> body) {
        log.info("selectBrandsByCategory(body={})", body);

        List<SelectBrandsByCategoryDto> selectedBrands = prodServ.readSelectBrandsByCategory(body.get("rentalCategoryId").longValue());
        log.info("selectedBrands={}", selectedBrands);
        
        return ResponseEntity.ok(selectedBrands);
    }
}