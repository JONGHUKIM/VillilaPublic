package com.splusz.villigo.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
	@PersistenceContext
	private EntityManager entityManager;
	
	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException, ServletException {
		log.info("onAuthenticationSuccess()");
		boolean isProfileCompleted = customizeOAuth2User(authentication);
		log.info("Profile completed: {}", isProfileCompleted);	
		// 필수 정보가 데이터베이스에 입력이 되어있으면 홈페이지로 리다이렉트
		if (isProfileCompleted) {
			response.sendRedirect("/");
		} else {
			response.sendRedirect("/member/signup-social");
		}
	}
	
	// DB에 사용자 정보가 저장되어 있는지 확인하고, 저장되어있다면 OAuth2User 객체의 속성으로 추가하는 메서드.
	public boolean customizeOAuth2User(Authentication authentication) {
		log.info("customizeOAuth2User(authentication={})", authentication);
		if(!(authentication.getPrincipal() instanceof OAuth2User)) {
            return false;
        } 

		OAuth2User oauth2user = (OAuth2User) authentication.getPrincipal();
		// DB에 정보가 저장되어 있으면 User 객체 생성			
		String email = oauth2user.getAttribute("email");
		log.info("구글 메일 주소: {}", email);
		User entity = null;
	    List<User> users = entityManager.createQuery(
	        "SELECT u FROM User u WHERE u.email = :email", User.class)
	        .setParameter("email", email)
	        .getResultList();

	    if (!users.isEmpty()) {
	        entity = users.get(0);
	    }
		
//		Optional<User> userOptional = userRepo.findByEmail(email); // 사용자의 이메일이 DB에 있는지 확인
//		User entity = userOptional.orElse(null); // DB에 없으면 null을 리턴
//		System.out.println("DB에서 가져온 User 정보: " + entity);
		
		// CustomOAuth2User: 기존 OAuth2User + DB에서 가져온 User 객체 정보
		CustomOAuth2User customUser = new CustomOAuth2User(oauth2user, entity);
		log.info("교체된 OAuth2User 정보: {}", customUser);
		
		// 새로운 Authentication 객체 생성
		OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(
				customUser, 
				authentication.getAuthorities(),
				((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId()
		);
		log.info("새로운 authentication 생성 완료: {}", newAuth);
		
		// SecurityContext에 새로운 Authentication 설정
		SecurityContextHolder.getContext().setAuthentication(newAuth);
		log.info("SecurityContext에 새로운 Authentication 설정");
		
		return customUser.isProfileComplete();
	}
}
