package com.splusz.villigo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.dto.SearchedProductDto;
import com.splusz.villigo.repository.AddressRepository;
import com.splusz.villigo.repository.BrandRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.RentalImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final ProductRepository prodRepo;
    private final AddressRepository addrRepo;
    private final RentalImageRepository rentalImgRepo;
    private final ProductService prodServ;
    private final BrandRepository brandRepo;

    public Page<SearchedProductDto> searchProduct(Map<String, List<String>> filters) {
        log.info("searchProduct(filters={})", filters);

        List<SearchedProductDto> searchedProducts = new ArrayList<>();
        List<ProductImageMergeDto> imageMergedProducts = new ArrayList<>();

        List<String> locations = filters.remove("location");
        List<String> page = filters.remove("page");
        Integer pageNum = (page == null || page.isEmpty() || page.get(0) == null)
            ? 0 : Integer.parseInt(page.get(0));

        log.info("pageNum={}", pageNum);

        List<Product> products = prodRepo.searchedProduct(filters);
        List<Long> productIds = products.stream()
            .map(Product::getId)
            .collect(Collectors.toList());

        List<Address> addresses = addrRepo.findAllByProduct_IdIn(productIds);

        if (locations != null) {
        	List<Long> filteredProductIdByLocation = addresses.stream()
        		.filter(address -> locations.stream().anyMatch(loc -> address.getSido().startsWith(loc)))
                .map(address -> address.getProduct().getId()) // 여기 중요! getId() → getProduct().getId()
                .collect(Collectors.toList());

            List<Product> filteredProductsByLocation = products.stream()
                .filter(product -> filteredProductIdByLocation.contains(product.getId()))
                .collect(Collectors.toList());

            // ✅ 여기서 랜덤 → 첫 이미지 고정으로 변경
            imageMergedProducts = prodServ.addFirstImageInProduct(filteredProductsByLocation);
        } else {
            imageMergedProducts = prodServ.addFirstImageInProduct(products);
        }

        searchedProducts = prodServ.addAddressInProduct(imageMergedProducts);

        Pageable pageable = PageRequest.of(pageNum, 6);
        return prodServ.listToPage(searchedProducts, pageable);
    }
    
    public List<Brand> readBrandsByCategory(Long rentalCategoryId) {
        if (rentalCategoryId == null || rentalCategoryId == 99) {
            return brandRepo.findAll(); // 모든 브랜드 반환
        }
        return brandRepo.findByRentalCategoryId(rentalCategoryId);
    }


}
