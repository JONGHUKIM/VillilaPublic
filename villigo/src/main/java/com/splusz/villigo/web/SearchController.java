package com.splusz.villigo.web;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Color;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.SearchService;
import com.splusz.villigo.storage.FileStorageService;
import com.splusz.villigo.storage.FileStorageException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/search")
public class SearchController {

    private final ProductService prodServ;
    private final SearchService searchServ;
    private final FileStorageService fileStorageService; // S3 서비스 주입

    @GetMapping
    public String search(Model model) {
        log.info("search()");
        List<RentalCategory> rentalCategories = prodServ.readRentalCategories();
        List<Color> colors = prodServ.readAllColors();
        List<Brand> brands = prodServ.readAllBrands();

        model.addAttribute("rentalCategories", rentalCategories);
        model.addAttribute("colors", colors);
        model.addAttribute("brands", brands);
        return "search"; // 뷰 이름
    }

    @PostMapping("/api/search")
    public ResponseEntity<?> searchApi(@RequestBody Map<String, List<String>> filters) {
        log.info("searchApi(filters={})", filters);
        return ResponseEntity.ok(searchServ.searchProduct(filters));
    }
    
    @PostMapping("/api/brand")
    public ResponseEntity<List<Brand>> getBrandsByCategory(@RequestBody Map<String, Long> request) {
        Long rentalCategoryId = request.get("rentalCategoryId");
        log.info("getBrandsByCategory(rentalCategoryId={})", rentalCategoryId);
        List<Brand> brands = prodServ.readBrandsByCategory(rentalCategoryId);
        return ResponseEntity.ok(brands);
    }
}