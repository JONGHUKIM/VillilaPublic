package com.splusz.villigo.web;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.ResponseBody;
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
import com.splusz.villigo.service.S3FileStorageService;
import com.splusz.villigo.service.ThemeService;
import com.splusz.villigo.service.UserService;
import com.splusz.villigo.storage.FileStorageException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final S3FileStorageService s3FileStorageService;
	
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
    
    @GetMapping("/social/checkphone")
    public ResponseEntity<Boolean> checkPhoneForSocial(@RequestParam(name = "phone") String phone) {
        log.info("소셜 가입용 checkPhone(phone={})", phone);
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
            CustomOAuth2User oauthUser = (CustomOAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            User user = userService.create(dto, nickname, email);
            log.info("소셜 회원가입 성공: userId={}, marketingConsent={}", user.getId(), user.isMarketingConsent());
            return "redirect:/member/signin";
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
    @ResponseBody
    public ResponseEntity<String> checkEmail(@RequestParam String email) {
        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent()) {
            if (userOpt.get().getSocialType() != null) {
                return ResponseEntity.ok("SNS_USER");
            } else { 
                return ResponseEntity.ok("DUPLICATE");
            }
        }
        return ResponseEntity.ok("AVAILABLE");
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

        User userEntity = null; // User 엔티티
        if (postId != null) {
            Product product = productService.getProductById(postId);
            if (product == null) {
                log.warn("Product not found for postId={}", postId);
                return "error/404";
            }
            userEntity = product.getUser();
        } else if (userId != null) {
            userEntity = userService.findById(userId); // User 엔티티 조회
            if (userEntity == null) {
                log.warn("User not found for userId={}", userId);
                return "error/404";
            }
        } else {
            log.warn("Neither postId nor userId provided.");
            return "error/404";
        }

        Long resolvedUserId = userEntity.getId();
        List<PostSummaryDto> products = productService.getUserProducts(resolvedUserId);
        List<ReviewDto> reviews = reviewService.getReviewsForUser(resolvedUserId);

        // UserProfileDto를 얻고, 그 정보를 UserDetailsDto에 매핑
        UserProfileDto userProfileDto;
        try {
            userProfileDto = userService.getUserProfileDto(userEntity); // <--- UserProfileDto로 변환 (avatarImageUrl 포함)
        } catch (FileStorageException e) {
            log.error("유저 상세 페이지 아바타 URL 생성 실패 (FileStorageException): {}", e.getMessage(), e);
            userProfileDto = UserProfileDto.builder() // 오류 시 기본값 설정
                .id(userEntity.getId())
                .nickname(userEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png")
                .build();
        } catch (Exception e) {
            log.error("유저 상세 페이지 프로필 조회 중 알 수 없는 오류: {}", e.getMessage(), e);
             userProfileDto = UserProfileDto.builder() // 다른 예외 시 기본값 설정
                .id(userEntity.getId())
                .nickname(userEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png")
                .build();
        }


        UserDetailsDto userDetailsDto = new UserDetailsDto();
        userDetailsDto.setId(userProfileDto.getId()); // UserProfileDto에서 ID 가져오기
        userDetailsDto.setNickname(userProfileDto.getNickname());
        userDetailsDto.setRegion(userProfileDto.getRegion());
        userDetailsDto.setAvatar(userProfileDto.getAvatar()); // S3 Key 설정
        userDetailsDto.setAvatarImageUrl(userProfileDto.getAvatarImageUrl()); // <--- UserProfileDto의 Pre-signed URL 설정
        userDetailsDto.setInterestCategory(userProfileDto.getTheme() != null ? userProfileDto.getTheme() : "자동차"); // Theme 필드 사용

        userDetailsDto.setPosts(products);
        userDetailsDto.setReviews(reviews);
        userDetailsDto.setMannerScore(userService.getMannerScore(userEntity.getId())); // User 엔티티 ID 사용

        model.addAttribute("user", userDetailsDto); // UserDetailsDto를 모델에 추가
        model.addAttribute("posts", products);
        model.addAttribute("reviews", reviews);
        return "member/details";
    }

    
    // 이미지 제공 엔드포인트
    @GetMapping("/images/{imageS3Key:.+}") // {image} 대신 {imageS3Key} 사용, 파일 경로에 .이 포함될 수 있으므로 :.+ 패턴 사용
    public ResponseEntity<Void> getImage(@PathVariable(name = "imageS3Key") String imageS3Key) { // Resource 대신 Void 반환 (리다이렉트만 할 것이므로)
        log.info("GET /member/images/{}", imageS3Key);
        try {
            // S3FileStorageService를 통해 Pre-signed URL 생성 (5분 유효)
            // 주의: 여기서 직접 DB에서 user.getAvatar() 값을 가져오지 않으므로,
            // imageS3Key가 유효한 사용자의 아바타인지 확인하는 로직을 추가하는 것이 보안상 좋음.
            // 예: UserDetailsService를 통해 S3Key를 사용자로 찾아 그 사용자가 이 파일의 소유자인지 확인
            String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(imageS3Key, Duration.ofMinutes(5));

            // 클라이언트를 Pre-signed URL로 리다이렉트
            return ResponseEntity.status(HttpStatus.FOUND) // 302 Found (임시 리다이렉트)
                                 .header(HttpHeaders.LOCATION, presignedUrl)
                                 .build();
        } catch (FileStorageException e) {
        	log.error("이미지 {}에 대한 Pre-signed URL 생성 오류: {}", imageS3Key, e.getMessage(), e);
            // 파일을 찾을 수 없거나 접근 권한이 없는 경우 (404 Not Found)
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
        	log.error("이미지 리다이렉션 중 {}에 대한 오류: {}", imageS3Key, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
    
    // 회원 탈퇴 엔드포인트
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAccount(HttpServletRequest request, HttpServletResponse response) {
<<<<<<< HEAD
        log.info("POST /member/withdraw");

=======
        log.info("POST /member/withdraw"); // 요청 로그
>>>>>>> 05dfe10 (회원 탈퇴 후 로그아웃 -> 홈으로 리다이렉트 코드 추가)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
<<<<<<< HEAD
            userService.withdrawCurrentUser(request, response);
<<<<<<< HEAD
            return ResponseEntity.ok("탈퇴 성공"); // 리다이렉트 없음
=======
            userService.withdrawCurrentUser(request, response); // 사용자 탈퇴 서비스 호출
            return ResponseEntity.ok("Account withdrawn successfully"); // 성공 응답
>>>>>>> 05dfe10 (회원 탈퇴 후 로그아웃 -> 홈으로 리다이렉트 코드 추가)
=======
            return ResponseEntity.ok("탈퇴 성공");
        } catch (FileStorageException e) { // S3 삭제 실패 예외 추가
        	log.error("계정 탈퇴 중 오류 발생 (S3 삭제 실패): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("탈퇴 중 S3 파일 삭제 오류 발생");
>>>>>>> 496eb16 (로컬 -> S3로 변경 user avatar 이미지 경로 변경)
        } catch (Exception e) {
        	log.error("계정 탈퇴 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("탈퇴 중 오류 발생");
        }
    }
    
>>>>>>> 49abed9 (서버에서 JSON응답처리, JS에서 리다이렉트 처리)
=======
    
 // 회원 탈퇴 엔드포인트
    @PostMapping("/withdraw")
    public ResponseEntity<String> withdrawAccount() {
        log.info("POST /member/withdraw"); // 요청 로그
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // 인증되지 않은 사용자는 탈퇴 불가
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            log.warn("User not authenticated, cannot withdraw"); // 비인증 사용자 로그
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        try {
            userService.withdrawCurrentUser(); // 사용자 탈퇴 서비스 호출
            SecurityContextHolder.clearContext(); // 로그아웃 처리
            return ResponseEntity.ok("Account withdrawn successfully"); // 성공 응답
        } catch (Exception e) {
            log.error("Error during account withdrawal: {}", e.getMessage(), e); // 에러 로그
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error withdrawing account");
        }
    }
    
>>>>>>> fe7f247 (게시물, 리뷰, 채팅만 남겨놓음 탈퇴코드 controller, service에 추가)
}
