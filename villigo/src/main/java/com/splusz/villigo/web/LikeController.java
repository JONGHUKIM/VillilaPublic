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

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal; 
                Long userId = user.getId();
                
                likeServ.create(userId, productId);
                
                return ResponseEntity.ok("liked");
            }
        }
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("like failed");
    }

    @GetMapping("/no")
    public ResponseEntity<String> noLike(@RequestParam(name = "id") Long productId) {
        log.info("noLike(productId={})", productId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal; 
                Long userId = user.getId();
                
                likeServ.delete(userId, productId);
                
                return ResponseEntity.ok("unliked");
            }
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("unliked failed");
    }
    
    @GetMapping("/check")
    public ResponseEntity<Boolean> checkLike(@RequestParam(name = "id") Long productId) {
        log.info("checkLike(productId={})", productId);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) {
                User user = (User) principal;
                Long userId = user.getId();

                boolean liked = likeServ.isLiked(userId, productId);
                return ResponseEntity.ok(liked);
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }

}
