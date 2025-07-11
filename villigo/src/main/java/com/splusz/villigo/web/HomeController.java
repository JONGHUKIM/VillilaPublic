package com.splusz.villigo.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splusz.villigo.config.CurrentUser;
import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.BrandReadDto;
import com.splusz.villigo.dto.ProductImageMergeDto;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class HomeController {
	
	private final UserService userService;
	private final ProductService prodService;
	private final ObjectMapper objectMapper;
	
    @GetMapping({"/", "/home"}) // 두 경로를 하나의 메서드로 매핑
    public String home(Model model) throws JsonProcessingException {
    	log.info("/ 또는 /home 요청으로 home() 실행됨");

        Map<String, List<ProductImageMergeDto>> homeProducts = new HashMap<>();
        User currentUser = null; // 현재 로그인된 사용자 정보를 담을 변수

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.info("현재 인증 정보: {}", authentication);

        // 인증된 사용자인 경우 User 객체 가져오기
        // OAuth2Authenticated user 또는 일반 사용자 모두 처리
        if (authentication != null && authentication.isAuthenticated() &&
            !(authentication.getPrincipal() instanceof String)) { // anonymousUser String 제외
            
            Object principal = authentication.getPrincipal();
            
            if (principal instanceof CustomOAuth2User) {
                // 소셜 로그인 (OAuth2) 사용자
                CustomOAuth2User customPrincipal = (CustomOAuth2User) principal;
                currentUser = customPrincipal.getUser(); // CustomOAuth2User에서 User 객체를 가져옴
            } else if (principal instanceof User) {
                // 일반 로그인 사용자 (UserDetails를 User로 직접 구현했을 경우)
                currentUser = (User) principal;
            } else {
                // 그 외 예상치 못한 Principal 타입
            	log.warn("예상치 못한 principal 타입입니다: {}", principal.getClass().getName());
            }

            // 로그인한 사용자(currentUser)의 정보를 사용하여 homeProducts 조회
            if (currentUser != null && currentUser.isProfileComplete()) { // 프로필이 완성된 사용자만 맞춤형 데이터 제공
                Long themeId = currentUser.getTheme() != null ? currentUser.getTheme().getId() : null;
                String region = currentUser.getRegion();
                
                log.info("로그인 사용자 - 사용자 ID: {}, 테마 ID: {}, 지역: {}", 
                        currentUser.getId(), themeId, region);

                // themeId 또는 region이 null이거나 비어있을 경우 기본값 처리
                if (themeId != null && region != null && !region.trim().isEmpty()) {
                    homeProducts = prodService.readHomeProducts(themeId, region);
                } else {
                    // 필수 정보 (테마/지역)가 불완전한 경우 기본값 (예: 서울, 테마ID 1)
                    homeProducts = prodService.readHomeProducts(1L, "서울");
                    log.warn("사용자 프로필이 불완전합니다 (테마 또는 지역 누락). 기본값으로 홈 상품 조회.");
                }
            } else {
                // 로그인했지만 프로필이 아직 미완성인 경우 (필터에 의해 signup-social로 리다이렉트될 수 있지만, 만일을 대비)
                homeProducts = prodService.readHomeProducts(1L, "서울");
                log.info("로그인했지만 프로필이 완성되지 않았습니다. 기본 홈 상품을 보여줍니다.");
            }
        } else {
            // 로그인하지 않은 경우 (AnonymousAuthenticationToken 포함)
            homeProducts = prodService.readHomeProducts(1L, "서울");
            log.info("비로그인 사용자입니다. 기본 홈 상품을 보여줍니다.");
        }
        
        // homeProducts Map에 모든 예상되는 키가 있는지 확인하고, 없으면 빈 리스트로 초기화 (JS 오류 방지)
        homeProducts.putIfAbsent("recent", Collections.emptyList());
        homeProducts.putIfAbsent("theme", Collections.emptyList());
        homeProducts.putIfAbsent("region", Collections.emptyList());


        // 브랜드 데이터 로드
        List<BrandReadDto> bagBrands = prodService.readBrandDto(1L);
        List<BrandReadDto> carBrands = prodService.readBrandDto(2L);
        
        // null 체크 및 빈 리스트로 초기화
        bagBrands = bagBrands != null ? bagBrands : Collections.emptyList();
        carBrands = carBrands != null ? carBrands : Collections.emptyList();

        // JSON 직렬화
        String bagBrandsJson = objectMapper.writeValueAsString(bagBrands);
        String carBrandsJson = objectMapper.writeValueAsString(carBrands);
        String homeProductsJson = objectMapper.writeValueAsString(homeProducts);

        // 모델에 추가
        model.addAttribute("bagBrandsJson", bagBrandsJson);
        model.addAttribute("carBrandsJson", carBrandsJson);
        model.addAttribute("homeProductsJson", homeProductsJson);

        // 현재 로그인한 사용자 정보 (Thymeleaf에서 접근 가능하도록)
        if (currentUser != null) {
            model.addAttribute("currentUser", currentUser);
        }

        return "index"; // index.html 또는 home.html
    }
}
