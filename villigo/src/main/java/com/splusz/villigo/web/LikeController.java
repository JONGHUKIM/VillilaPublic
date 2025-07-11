package com.splusz.villigo.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.service.LikeService;
import com.splusz.villigo.service.UserService;
import com.splusz.villigo.util.SecurityUserUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/like")
public class LikeController {

    private final LikeService likeServ;
    private final UserService userServ;

    @GetMapping("/yes")
    public ResponseEntity<String> yesLike(@RequestParam(name = "id") Long productId) {
        log.info("yesLike(productId={})", productId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityUserUtil을 사용하여 User 객체 가져오기
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        if (user == null || user.getId() == null) { // User 객체가 없거나 ID가 null인 경우
            log.warn("yesLike: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다."); // 401 Unauthorized
        }

        likeServ.create(user.getId(), productId);

        return ResponseEntity.ok("liked");
    }

    @GetMapping("/no")
    public ResponseEntity<String> noLike(@RequestParam(name = "id") Long productId) {
        log.info("noLike(productId={})", productId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityUserUtil을 사용하여 User 객체 가져오기
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        if (user == null || user.getId() == null) { // User 객체가 없거나 ID가 null인 경우
            log.warn("noLike: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다."); // 401 Unauthorized
        }

        likeServ.delete(user.getId(), productId);

        return ResponseEntity.ok("unliked");
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkLike(@RequestParam(name = "id") Long productId) {
        log.info("checkLike(productId={})", productId);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // SecurityUserUtil을 사용하여 User 객체 가져오기
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        if (user == null || user.getId() == null) { // User 객체가 없거나 ID가 null인 경우
            log.warn("checkLike: 사용자 정보가 없거나 ID가 null입니다. 401 Unauthorized 반환.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false); // 401 Unauthorized
        }

        boolean liked = likeServ.isLiked(user.getId(), productId);
        return ResponseEntity.ok(liked);
    }
}
