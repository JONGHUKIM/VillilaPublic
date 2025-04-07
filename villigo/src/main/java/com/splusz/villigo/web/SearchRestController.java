package com.splusz.villigo.web;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.dto.SearchedProductDto;
import com.splusz.villigo.dto.SelectBrandsByCategoryDto;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.SearchService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api")
@RestController
public class SearchRestController {

    private final SearchService searServ;
    private final ProductService prodServ;

    @PostMapping("/search")
    public ResponseEntity<Page<SearchedProductDto>> searching (@RequestBody Map<String, List<String>> filters) {

        log.info("filters={}", filters);
        Page<SearchedProductDto> searchedProducts = searServ.searchProduct(filters);

        return ResponseEntity.ok(searchedProducts);
    };
    
    @PostMapping("/brand")
    public ResponseEntity<List<SelectBrandsByCategoryDto>> selectBrandsByCategory(@RequestBody Map<String, Long> body) {
    	log.info("selectBrandsByCategory(body={})", body);

    	List<SelectBrandsByCategoryDto> selectedBrands = prodServ.readSelectBrandsByCategory(body.get("rentalCategoryId").longValue());
    	log.info("selectedBrands={}", selectedBrands);
    	
    	return ResponseEntity.ok(selectedBrands);
    }
}
