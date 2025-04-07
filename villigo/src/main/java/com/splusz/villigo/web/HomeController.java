package com.splusz.villigo.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splusz.villigo.config.CurrentUser;
import com.splusz.villigo.domain.Brand;
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
	
	@GetMapping("/")
	public String home(@CurrentUser User user, Model model) throws JsonProcessingException {
		log.info("home()");
		Map<String, List<ProductImageMergeDto>> homeProducts = new HashMap<>();
		
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		log.info("authentication: ", authentication);
		Boolean a = authentication.isAuthenticated();
		log.info("authentication.isAuthenticated(): ", a.toString());

        if (user != null) {
            Object principal = authentication.getPrincipal();            
  
            log.info("user: ", user);
            user = (User) principal; // 캐스팅
            log.info("user: ", user);
            String username = user.getUsername(); // User 객체의 id 반환
            Long themeId = user.getTheme().getId();
            String region = user.getRegion();
            
            log.info(username, themeId, region);
            log.info(themeId.toString());
            log.info(region);

			homeProducts = prodService.readHomeProducts(themeId, region);
        } else {
        	homeProducts = prodService.readHomeProducts(1L, "서울");
        	log.info("로그인 안한 경우");
        }
        
        log.info("result = {}", homeProducts.toString());
		List<BrandReadDto> bagBrands = prodService.readBrandDto(1L);
		List<BrandReadDto> carBrands = prodService.readBrandDto(2L);
        
	    ObjectMapper mapper = new ObjectMapper();
	    String bagBrandsJson = mapper.writeValueAsString(bagBrands);
	    String carBrandsJson = mapper.writeValueAsString(carBrands);
	    String homeProductsJson = mapper.writeValueAsString(homeProducts);
	    
        
		model.addAttribute("bagBrandsJson", bagBrandsJson);
		model.addAttribute("carBrandsJson", carBrandsJson);
		model.addAttribute("homeProductsJson", homeProductsJson);
        return "index";
	}
	
    @GetMapping("/home")
    public String home(@AuthenticationPrincipal OAuth2User principal, Model model) {
       if (principal != null) log.info("OAuth2User: {}", principal);
//            model.addAttribute("name", principal.getAttribute("name"));
//            model.addAttribute("email", email);
        
        return "redirect:/";
    }
}
