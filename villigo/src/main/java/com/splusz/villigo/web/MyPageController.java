package com.splusz.villigo.web;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.service.MyPageService;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageServ;

    @GetMapping("/myproduct")
    public ResponseEntity<Page<ProductImageMergeDto>> myProductPaging(@RequestParam("p") Integer pageNum) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityUserUtil을 사용하여 User 객체 가져오기
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        // 사용자 정보가 없거나 ID가 null인 경우 인증되지 않은 응답 반환
        if (user == null || user.getId() == null) {
            log.warn("myProductPaging: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = user.getId(); // User 객체의 id 반환
        log.info("myProductPaging(pageNum={}, userId={})", pageNum, userId);

        Page<ProductImageMergeDto> myProductPage = myPageServ.readMyProductPaging(userId, pageNum);
        log.info("myProductPage={}", myProductPage);

        return ResponseEntity.ok(myProductPage);
    }

    @GetMapping("/likeproduct")
    public ResponseEntity<Page<ProductImageMergeDto>> likeProductPaging(@RequestParam("p") Integer pageNum) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityUserUtil을 사용하여 User 객체 가져오기
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        // 사용자 정보가 없거나 ID가 null인 경우 인증되지 않은 응답 반환
        if (user == null || user.getId() == null) {
            log.warn("likeProductPaging: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = user.getId(); // User 객체의 id 반환
        log.info("likeProductPaging(pageNum={}, userId={})", pageNum, userId);

        Page<ProductImageMergeDto> likeProductPage = myPageServ.readLikeProductPaging(userId, pageNum);
        log.info("likeProductPage={}", likeProductPage);

        return ResponseEntity.ok(likeProductPage);
    }
}
