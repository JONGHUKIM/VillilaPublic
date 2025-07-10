package com.splusz.villigo.config;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class UserProfileCompletionFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
			FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		log.info("Filtering request: {}", path);
	    
		// 허용된 경로들 (SecurityConfig의 permitAll()과 일치)
	    String[] permitAllPaths = {
	            "/login", "/error", "/search", "/api/search", "/api/brand", "/member/signin", 
	            "/member/signup", "/logout", "/member/signup-social", "/member/checkusername",
	            "/member/social/checkphone",
	            "/member/checkemail", "/member/checknickname", "/api/faqs", "/css/", "/js/", 
	            "/images/", "/ws/", "/sockjs/", "/fonts/", "/queue/", "/bot/"
	        };
	    
	    // 허용된 경로인지 확인
	    boolean isPermittedPath = false;
	    for (String permitPath : permitAllPaths) {
	        if (path.startsWith(permitPath)) {
	            isPermittedPath = true;
	            log.info("Permitted path: {}", permitPath);
	            break;
	        }
	    }
	    
	    // 허용된 경로라면 필터 건너뛰기
//	    if (isPermittedPath) {
//	        filterChain.doFilter(request, response);
//	        return;
//	    }
	    
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.info("Authentication: {}", authentication);
		
		// 리다이렉션 횟수 제한을 위한 로직
		String redirectCount = (String) request.getSession().getAttribute("redirectCount");
		int count = 0;
		if (redirectCount != null) {
			count = Integer.parseInt(redirectCount);
			if (count > 3) {
				log.warn("*** Too many redirects detected. Skipping profile completion check. ***");
				filterChain.doFilter(request, response);
				return;
			}
		}
		
		// (1) 인증된 사용자 ==> authentication.isAuthenticated() == true
		// (2) OAuth 인증을 받은 사용자	==> authentication instanceof OAuth2AuthenticationToken == true
		// 위 조건들을 모두 만족하는 경우에만 필터 작동
		if (authentication != null && authentication instanceof OAuth2AuthenticationToken 
				&& authentication.isAuthenticated()) {
			log.info("OAuth2 authenticated user detected");
			boolean isProfileComplete = checkUserProfile(authentication);
			log.info("필수정보 입력 여부: {}", isProfileComplete);
			
			// 필수정보 입력이 필요하고, 요청이 필수정보 입력 페이지가 아닐 경우 리다이렉트
			if (!isProfileComplete && !isPermittedPath) {
				log.info("프로필 미완성으로 /member/signup-social 페이지로 리다이렉트");
				response.sendRedirect("/member/signup-social");
				return;
			} else {
                log.info("No redirect: complete={}, permitted={}", isProfileComplete, isPermittedPath);
            }
		}
		
		log.info("필터 체인 진행 중");
		filterChain.doFilter(request, response);
	}

	private boolean checkUserProfile(Authentication authentication) {
		log.info("checkUserProfile(auth={})", authentication);
		if (authentication.getPrincipal() instanceof CustomOAuth2User) {
			CustomOAuth2User customUser = (CustomOAuth2User) authentication.getPrincipal();
			boolean complete = customUser.isProfileComplete();
			log.info("User profile complete: {}", complete);
			return complete;
		}
		return false;
	}

}
