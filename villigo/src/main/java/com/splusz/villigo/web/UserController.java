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
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

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
        
        return "member/signin";
    }
    
    @GetMapping("/signup")
    public String signUp(Model model) { // void -> String으로 변경
        log.info("GET signUp()");
        List<Theme> themes = themeService.read();
        model.addAttribute("themes", themes);
        model.addAttribute("userSignUpDto", new UserSignUpDto()); // DTO 객체 추가
        return "member/signup"; // 템플릿 경로 반환
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
            return "member/signup";
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
            return "member/signup";
        }
    }
    
    @GetMapping("/agreement") // 약관 동의 페이지 매핑 추가
    public String agreement() {
        log.info("GET agreement()"); //
        return "member/agreement"; // agreement.html 템플릿 반환
    }
    
    @GetMapping("/checkphone")
    public ResponseEntity<Boolean> checkPhone(@RequestParam(name = "phone") String phone) {
        log.info("checkPhone(phone={})", phone);
        return ResponseEntity.ok(userService.checkPhone(phone));
    }
    
    // 소셜 회원가입 폼 렌더링
    @GetMapping("/signup-social")
    public String showSocialSignUpForm(Model model) {
        model.addAttribute("socialUserSignUpDto", new SocialUserSignUpDto());
        List<Theme> themes = themeService.read();
        model.addAttribute("themes", themes);
        return "member/signup-social";
    }
    
    
    @PostMapping("/signup-social")
    public String processSocialSignUp(
            @Valid @ModelAttribute SocialUserSignUpDto dto,
            BindingResult result,
            Authentication authentication,
            Model model) {
        log.info("POST signup-social(dto={})", dto);
        log.info("Marketing consent: {}", dto.isMarketingConsent());

        if (result.hasErrors()) {
            log.warn("유효성 검사 오류: {}", result.getAllErrors());
            List<Theme> themes = themeService.read();
            model.addAttribute("themes", themes);
            model.addAttribute("errors", result.getAllErrors());
            model.addAttribute("socialUserSignUpDto", dto);
            return "member/signup-social";
        }

        try {
            String nickname = dto.getNickname();
            String realname = authentication.getName();
            String email = authentication.getPrincipal().toString();
            User user = userService.create(dto, nickname, realname, email);
            log.info("소셜 회원가입 성공: userId={}, marketingConsent={}", user.getId(), user.isMarketingConsent());
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            log.warn("소셜 회원가입 실패: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            List<Theme> themes = themeService.read();
            model.addAttribute("themes", themes);
            model.addAttribute("socialUserSignUpDto", dto);
            return "member/signup-social";
        }
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
        return "member/modify";
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

        return "mypage";
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
            Path filePath = Paths.get("/home/ubuntu/images/avatar").resolve(image).normalize();
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
<<<<<<< HEAD
=======
    
    // 회원 탈퇴 엔드포인트
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAccount(HttpServletRequest request, HttpServletResponse response) {
        log.info("POST /member/withdraw");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            userService.withdrawCurrentUser(request, response);
            return ResponseEntity.ok("탈퇴 성공"); // 리다이렉트 없음
        } catch (Exception e) {
            log.error("Error during account withdrawal: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("탈퇴 중 오류 발생");
        }
    }
    
>>>>>>> 49abed9 (서버에서 JSON응답처리, JS에서 리다이렉트 처리)
}
