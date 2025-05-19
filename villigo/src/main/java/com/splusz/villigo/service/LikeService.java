package com.splusz.villigo.service;

import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Like;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.LikeRepository;
import com.splusz.villigo.repository.ProductRepository;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class LikeService {

    private final LikeRepository likeRepo;
    private final ProductRepository prodRepo;
    private final UserRepository userRepo;

    public void create(Long userId, Long productId) {
        log.info("create(userId={}, productId={})", userId, productId);
        try {
            Product product = prodRepo.findById(productId).orElseThrow();
            User user = userRepo.findById(userId).orElseThrow();
            Like entity = Like.builder()
                .product(product)
                .user(user)
                .build();
            likeRepo.save(entity);
        } catch (Exception e) {
            log.info("좋아요 중복 불가!");
        }
    }

    public void delete(Long userId, Long productId) {
        log.info("delete(userId={}, productId={})", userId, productId);
        likeRepo.deleteByUser_IdAndProduct_Id(userId, productId);
    }
    
    // 찜 여부 체크용
    public boolean isLiked(Long userId, Long productId) {
        return likeRepo.existsByUser_IdAndProduct_Id(userId, productId);
    }

}
