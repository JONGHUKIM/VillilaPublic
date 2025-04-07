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
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.dto.SearchedProductDto;
import com.splusz.villigo.repository.AddressRepository;
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

    public Page<SearchedProductDto> searchProduct(Map<String, List<String>> filters) {
        
        log.info("searchProduct(filters={})", filters);
        List<SearchedProductDto> searchedProducts = new ArrayList<>();
        List<ProductImageMergeDto> ImageMergeProducts = new ArrayList<>();
        List<String> locations = filters.remove("location");
        List<String> page = filters.remove("page");
        Integer pageNum = (page.getFirst() == null)? 0 : Integer.parseInt(page.getFirst());
        log.info("pageNum={}", pageNum);

        List<Product> products = prodRepo.searchedProduct(filters);
        List<Long> productIds = products.stream()
            .map(product -> product.getId())
            .collect(Collectors.toList());
        
        List<Address> addresses = addrRepo.findAllByProduct_IdIn(productIds);

        if(locations != null) {
            List<Long> filteredProductIdByLocation = addresses.stream()
                .filter(address -> locations.contains(address.getSido()))
                .map(address -> address.getId())
                .collect(Collectors.toList());
        
            List<Product> fileterdProductsByLocation = products.stream()
                .filter(product -> filteredProductIdByLocation.contains(product.getId()))
                .collect(Collectors.toList());

            ImageMergeProducts = prodServ.addRandomImageInProduct(fileterdProductsByLocation);
            log.info("products={}", ImageMergeProducts);

        } else {

            ImageMergeProducts = prodServ.addRandomImageInProduct(products);
            log.info("products={}", ImageMergeProducts);
        }

        searchedProducts = prodServ.addAddressInProduct(ImageMergeProducts);

        Pageable pageable = PageRequest.of(pageNum, 6);
        Page<SearchedProductDto> searchedProductsPaging = prodServ.listToPage(searchedProducts, pageable);
        
        return searchedProductsPaging;
    }

}
