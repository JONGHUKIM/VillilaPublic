package com.splusz.villigo.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.PostSummaryDto;
import com.splusz.villigo.dto.ReviewDto;
import com.splusz.villigo.dto.SocialUserSignUpDto;
import com.splusz.villigo.dto.UserDetailsDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.dto.UserSignUpDto;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.ReviewService;
import com.splusz.villigo.service.ThemeService;
import com.splusz.villigo.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/member")
public class UserController {
	
	private final UserService userService;
	private final ThemeService themeService;
	private final ProductService productService;
    private final ReviewService reviewService;
	
    @GetMapping("/signin")
    public String signIn() {
        log.info("GET signin()");
        // 이미 인증된 사용자는 홈으로 리디렉션
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() 
        		&& !(auth instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }
        
        return "/member/signin";
    }
    
    @GetMapping("/signup")
    public String signUp(Model model) { // void -> String으로 변경
        log.info("GET signUp()");
        List<Theme> themes = themeService.read();
        model.addAttribute("themes", themes);
        model.addAttribute("userSignUpDto", new UserSignUpDto()); // DTO 객체 추가
        return "/member/signup"; // 템플릿 경로 반환
    }
    
    @PostMapping("/signup")
    public String signUp(@Valid UserSignUpDto dto, BindingResult bindingResult, Model model) {
        log.info("POST signUp(dto={})", dto);

        if (bindingResult.hasErrors()) {
            log.warn("유효성 검사 오류: {}", bindingResult.getAllErrors());
            List<Theme> themes = themeService.read();
            model.addAttribute("themes", themes);
            model.addAttribute("errors", bindingResult.getAllErrors());
            model.addAttribute("userSignUpDto", dto); // 오류 발생 시 DTO 유지
            return "/member/signup";
        }

        try {
            User user = userService.create(dto);
            log.info("회원가입 성공: {}", user);
            return "redirect:/member/signin";
        } catch (IllegalArgumentException e) {
            log.warn("회원가입 실패: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            List<Theme> themes = themeService.read();
            model.addAttribute("themes", themes);
            model.addAttribute("userSignUpDto", dto); // 오류 발생 시 DTO 유지
            return "/member/signup";
        }
    }
    
    @GetMapping("/checkphone")
    public ResponseEntity<Boolean> checkPhone(@RequestParam(name = "phone") String phone) {
        log.info("checkPhone(phone={})", phone);
        return ResponseEntity.ok(userService.checkPhone(phone));
    }
    
    @GetMapping("/signup-social")
    public void signUpSocial(Model model) {
        log.info("GET signUpSocial()");

        List<Theme> themes = themeService.read(); 
        model.addAttribute("themes", themes);
    }
    
    
    @PostMapping("/signup-social")
    public String signUpSocial(Authentication authentication, SocialUserSignUpDto dto) {
    	log.info("POST signUpSocial(dto={})", dto); 
    	String realname = null;
    	String email = null;
    	log.info("authentication: {}", authentication);
    	if (authentication.getPrincipal() instanceof CustomOAuth2User) {
    		CustomOAuth2User oauth2user = (CustomOAuth2User) authentication.getPrincipal();
    		// 구글에서 받은 인증정보에서 이름과 이메일 데이터를 가져옴.
    		realname = oauth2user.getAttribute("name");
    		email = oauth2user.getAttribute("email");
    		log.info("realname: {}, email: {}", realname, email);
    	}
    	
    	User user = userService.create(dto, dto.getNickname(), realname, email);
    	log.info("SNS 회원가입 완료: {}", user);
    	String uri = userService.checkSnsLogin(authentication);
    	log.info("uri: {}", uri);
    	return "redirect:/";
    }
    
    @GetMapping("/checkusername")
    public ResponseEntity<Boolean> checkUsername(@RequestParam(name = "username") String username) {
    	log.info("checkUsername(username={})", username);
    	return ResponseEntity.ok(userService.checkUsername(username));
    }
    
    @GetMapping("/checknickname")
    public ResponseEntity<Boolean> checkNickname(@RequestParam(name = "nickname") String nickname) {
    	log.info("checkNickname(nickname={})", nickname);
    	return ResponseEntity.ok(userService.checkNickname(nickname));
    }
    
    @GetMapping("/checkemail")
    public ResponseEntity<Boolean> checkEmail(@RequestParam(name = "email") String email) {
    	log.info("checkemail(email={})", email);
    	return ResponseEntity.ok(userService.checkEmail(email));
    }
    
    @GetMapping("/modify")
    public String modify(Model model) {
        log.info("GET /member/modify");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("User not authenticated, redirecting to signin");
            return "redirect:/member/signin";
        }

        try {
            UserProfileDto user = userService.getCurrentUserProfile();
            if (user == null) {
                log.error("UserProfileDto is null for authenticated user");
                return "redirect:/member/signin";
            }
            List<Theme> themes = themeService.read();
            model.addAttribute("user", user);
            model.addAttribute("themes", themes);
        } catch (Exception e) {
            log.error("Error fetching user profile for modify: {}", e.getMessage(), e);
            return "redirect:/member/signin";
        }
        return "/member/modify";
    }

    @PostMapping("/modify")
    public String updateProfile(
            @RequestParam(name = "nickname") String nickname,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "phone") String phone,
            @RequestParam(name = "region") String region,
            @RequestParam(name = "themeId") Long themeId,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage) throws IOException {
        log.info("POST /member/modify: nickname={}, phone={}, region={}, themeId={}", nickname, phone, region, themeId);

        userService.updateUserProfile(nickname, password, phone, region, themeId, profileImage);
        return "redirect:/member/mypage";
    }
    
    @GetMapping("/mypage")
    public String myPage(Model model) {
        log.info("GET /member/mypage");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("User not authenticated, redirecting to signin");
            return "redirect:/member/signin";
        }

        try {
            UserProfileDto user = userService.getCurrentUserProfile();
            if (user == null) {
                log.error("UserProfileDto is null for authenticated user");
                return "redirect:/member/signin";
            }
            log.info("UserProfileDto fetched: {}", user);
            model.addAttribute("user", user);
        } catch (Exception e) {
            log.error("Error fetching user profile for mypage: {}", e.getMessage(), e);
            return "redirect:/member/signin";
        }

        return "/mypage";
    }
    
    @GetMapping("/details")
    public String memberDetails(
        @RequestParam(value = "postId", required = false) Long postId,
        @RequestParam(value = "userId", required = false) Long userId,
        Model model
    ) {
        log.info("GET /member/details?postId={}, userId={}", postId, userId);

        User user = null;

        if (postId != null) {
            Product product = productService.getProductById(postId);
            if (product == null) {
                log.warn("Product not found for postId={}", postId);
                return "error/404";
            }
            user = product.getUser();
        } else if (userId != null) {
            user = userService.findById(userId);
            if (user == null) {
                log.warn("User not found for userId={}", userId);
                return "error/404";
            }
        } else {
            log.warn("Neither postId nor userId provided.");
            return "error/404";
        }

        Long resolvedUserId = user.getId();
        List<PostSummaryDto> products = productService.getUserProducts(resolvedUserId);
        List<ReviewDto> reviews = reviewService.getReviewsForUser(resolvedUserId);

        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setId(user.getId());
        userDetailsDto.setNickname(user.getNickname());
        userDetailsDto.setRegion(user.getRegion());
        userDetailsDto.setAvatar(user.getAvatar());
        userDetailsDto.setInterestCategory(user.getTheme() != null ? user.getTheme().getTheme() : "자동차");
        userDetailsDto.setPosts(products);
        userDetailsDto.setReviews(reviews);
        userDetailsDto.setMannerScore(userService.getMannerScore(user.getId()));

        model.addAttribute("user", userDetailsDto);
        model.addAttribute("posts", products);
        model.addAttribute("reviews", reviews);
        return "member/details";
    }

    
    // 이미지 제공 엔드포인트
    @GetMapping("/images/{image}")
    public ResponseEntity<Resource> getImage(@PathVariable(name = "image") String image) {
        log.info("GET /member/images/{}", image);
        try {
            // 이미지 파일 경로 (예: /uploads/ 디렉토리에 저장됨)
            Path filePath = Paths.get("C:/images/avatar/").resolve(image).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = "image/jpeg"; // 기본값
                if (image.endsWith(".png")) {
                    contentType = "image/png";
                } else if (image.endsWith(".gif")) {
                    contentType = "image/gif";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error loading image: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
