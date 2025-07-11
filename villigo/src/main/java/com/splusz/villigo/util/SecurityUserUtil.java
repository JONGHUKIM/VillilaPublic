package com.splusz.villigo.util;

import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUserUtil {

    /**
     * 현재 인증된 Principal 객체에서 User 도메인 객체를 추출
     * OAuth2User (CustomOAuth2User) 또는 일반 로그인 User (UserDetails 구현)를 처리
     * 
     * @param principal Spring Security의 Authentication.getPrincipal()에서 반환된 객체
     * @return 추출된 User 도메인 객체 또는 인증되지 않았거나 예상치 못한 타입일 경우 null
     */
    public static User getUserFromPrincipal(Object principal) {
        if (principal == null || principal instanceof AnonymousAuthenticationToken) {
            return null; // 인증되지 않은 사용자 또는 익명 사용자
        }

        if (principal instanceof CustomOAuth2User) {
            CustomOAuth2User oauthUser = (CustomOAuth2User) principal;
            // 필요하다면 여기서 isProfileComplete()를 확인하고, 미완성이라면 null을 반환하거나 예외처리
            // 하지만 현재 컨트롤러들이 이 필터 역할을 이미 하고 있으므로 여기서는 단순히 User 객체를 반환
            return oauthUser.getUser();
        } else if (principal instanceof User) { // 일반 로그인 사용자
            return (User) principal;
        }
        return null;
    }

    /**
     * 현재 SecurityContext에서 로그인한 User 도메인 객체를 가져오는 편의 메서드
     *
     * @return 로그인한 User 객체 로그인하지 않았거나 정보를 가져올 수 없는 경우 null
     */
    public static User getCurrentLoggedInUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return getUserFromPrincipal(authentication.getPrincipal());
    }
}