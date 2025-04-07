package com.splusz.villigo.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Like;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.repository.LikeRepository;
import com.splusz.villigo.repository.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class MyPageService {

    private final ProductRepository prodRepo;
    private final LikeRepository likeRepo;

    private final ProductService prodServ;
    
    public Page<ProductImageMergeDto> readMyProductPaging(Long userId, Integer pageNum) {
        
        Pageable pageable = PageRequest.of(pageNum, 9);
        List<Product> myProductList = prodRepo.findByUser_IdOrderByCreatedTimeDesc(userId);
        List<ProductImageMergeDto> myDtoList = prodServ.addRandomImageInProduct(myProductList);
        Page<ProductImageMergeDto> myDtoPage = prodServ.listToPage(myDtoList, pageable);
        log.info("myDtoList={}", myDtoList);

        return myDtoPage;
    }

    public Page<ProductImageMergeDto> readLikeProductPaging(Long userId, Integer pageNum) {
        Pageable pageable = PageRequest.of(pageNum, 9);

        List<Like> likes = likeRepo.findByUser_IdOrderByCreatedTime(userId);

        List<Long> likeIds = likes.stream()
        .map(like -> like.getProduct().getId())
        .collect(Collectors.toList());

        List<Product> likeProductList = prodRepo.findAllByIdIn(likeIds);
        List<ProductImageMergeDto> likeDtoList = prodServ.addRandomImageInProduct(likeProductList);
        Page<ProductImageMergeDto> likeDtoPage = prodServ.listToPage(likeDtoList, pageable);
        
        return likeDtoPage;
    }
}
