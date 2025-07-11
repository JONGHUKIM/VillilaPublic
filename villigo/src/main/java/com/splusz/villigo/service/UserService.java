package com.splusz.villigo.service;

import com.splusz.villigo.domain.*;
import com.splusz.villigo.dto.ReviewDto;
import com.splusz.villigo.repository.*;
import com.splusz.villigo.util.SecurityUserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.config.CustomAuthenticationSuccessHandler;
import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.dto.SocialUserSignUpDto;
import com.splusz.villigo.dto.UpdateAvatarRequestDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.dto.UserSignUpDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepo;
    private final ThemeRepository themeRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final UserJjamRepository userJjamRepo;

    private static final String UPLOAD_DIR = "/home/ubuntu/images/avatar";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername(username = {})", username);
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(username + "과(와) 일치하는 사용자 정보가 없습니다."));
    }

    public Boolean checkPhone(String phone) {
        log.info("checkPhone(phone={})", phone);
        return userRepo.findByPhone(phone.replaceAll("-", "")).isPresent();
    }

    @Transactional
    public User create(@Valid UserSignUpDto dto) {
        log.info("create(dto={})", dto);
        dto.validatePasswordMatch();

        if (checkUsername(dto.getUsername())) throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        if (checkNickname(dto.getNickname())) throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        if (checkEmail(dto.getEmail())) throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        if (checkPhone(dto.getPhone())) throw new IllegalArgumentException("이미 사용 중인 전화번호입니다.");

        Theme theme = themeRepo.findById(dto.getThemeId()).orElseThrow(() -> new IllegalArgumentException("테마를 찾을 수 없습니다."));

        User entity = dto.toEntity(passwordEncoder, theme).addRole(UserRole.USER);
        entity.setPhone(entity.getPhone().replaceAll("-", ""));
        entity.setMarketingConsent(dto.isMarketingConsent());
        return userRepo.save(entity);
    }

    @Transactional
    public User create(SocialUserSignUpDto dto, String nickname, String email) {
        log.info("create(dto={}, nickname={}, email={})", dto, nickname, email);

        if (checkNickname(nickname)) throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        if (checkPhone(dto.getPhone())) throw new IllegalArgumentException("이미 사용 중인 번호입니다.");

        Theme theme = themeRepo.findById(dto.getThemeId()).orElseThrow(() -> new IllegalArgumentException("테마를 찾을 수 없습니다."));

        User entity = dto.toEntity(theme).addRole(UserRole.USER);
        entity.setUsername(email.split("@")[0]);
        entity.setNickname(nickname);
        entity.setEmail(email);
        entity.setSnsLogin(true);
        entity.setPhone(dto.getPhone().replaceAll("-", ""));
        entity.setMarketingConsent(dto.isMarketingConsent());
        entity = userRepo.save(entity);

        Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuth instanceof OAuth2AuthenticationToken) {
            String clientRegistrationId = ((OAuth2AuthenticationToken) currentAuth).getAuthorizedClientRegistrationId();
            // registrationId에 따라 SocialType 설정 (예: "google", "kakao" 등)
            if ("google".equalsIgnoreCase(clientRegistrationId)) {
                entity.setSocialType(SocialType.GOOGLE); // SocialType enum에 GOOGLE이 있다고 가정
            }
            // else if ("kakao".equalsIgnoreCase(clientRegistrationId)) {
            //     entity.setSocialType(SocialType.KAKAO);
            // }
            // ... 기타 소셜 로그인 서비스에 따라 SocialType 추가
            
        } else {
            // 소셜 로그인이 아닌 경우 (혹은 예상치 못한 경우)
            entity.setSocialType(null); 
        }

        entity = userRepo.save(entity);

        // SecurityContext 업데이트 로직은 기존대로 유지
        if (currentAuth instanceof OAuth2AuthenticationToken) {
            CustomOAuth2User oldUser = (CustomOAuth2User) currentAuth.getPrincipal();
            CustomOAuth2User newUser = new CustomOAuth2User(oldUser.getDelegate(), entity);
            OAuth2AuthenticationToken newAuth = new OAuth2AuthenticationToken(newUser, currentAuth.getAuthorities(), ((OAuth2AuthenticationToken) currentAuth).getAuthorizedClientRegistrationId());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
            log.info("SecurityContext CustomOAuth2User updated. ID: {}", entity.getId());
        }

        return entity;
    }

    public User read(Long id) {
        log.info("read(id={})", id);
        return userRepo.findById(id).orElseThrow();
    }

    public String checkSnsLogin(Authentication authentication) {
        log.info("checkSnsLogin(CustomAuthenticationSuccessHandler={})", successHandler);
        boolean completed = successHandler.customizeOAuth2User(authentication);
        return completed ? "redirect:/" : "/member/signup-social";
    }

    public Boolean checkUsername(String username) {
        return userRepo.findByUsername(username).isPresent();
    }

    public Boolean checkNickname(String nickname) {
        return userRepo.findByNickname(nickname).isPresent();
    }

    public Boolean checkEmail(String email) {
        return userRepo.findByEmail(email).isPresent();
    }

    public Optional<User> findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public void setUserOnline(Long userId, boolean isOnline) {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        user.setOnline(isOnline);
        userRepo.save(user);
    }

    public boolean isUserOnline(Long userId) {
        log.info("isUserOnline(userId={})", userId);
        return userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId)).isOnline();
    }

    public int getMannerScore(Long userId) {
        return userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId)).getMannerScore();
    }

    public void updateMannerScore(Long userId, int scoreDelta) {
        User user = userRepo.findById(userId).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        user.setMannerScore(Math.max(user.getMannerScore() + scoreDelta, 0));
        userRepo.save(user);
    }

    @Transactional
    public User updateUserProfile(String nickname, String password, String phone, String region, Long themeId, MultipartFile avatarFile) throws IOException {
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");

        if (nickname != null && !nickname.isEmpty()) user.setNickname(nickname);
        if (password != null && !password.isEmpty()) user.setPassword(passwordEncoder.encode(password));
        if (phone != null && !phone.isEmpty()) user.setPhone(phone);
        if (region != null && !region.isEmpty()) user.setRegion(region);
        if (themeId != null) user.setTheme(themeRepo.findById(themeId).orElse(null));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            String ext = Optional.ofNullable(avatarFile.getOriginalFilename())
                    .map(name -> name.substring(name.lastIndexOf(".")))
                    .orElse("");
            String newFileName = UUID.randomUUID() + ext;
            File destFile = new File(UPLOAD_DIR + File.separator + newFileName);
            avatarFile.transferTo(destFile);
            user.setAvatar(newFileName);
        }

        return userRepo.save(user);
    }

    public static String normalizePath(String path) {
        return path != null && path.startsWith("/") ? path.substring(1) : path;
    }

    @Transactional
    public User updateAvatar(UpdateAvatarRequestDto requestDto) throws IOException {
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");

        MultipartFile avatarFile = requestDto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) directory.mkdirs();

            String ext = Optional.ofNullable(avatarFile.getOriginalFilename())
                    .map(name -> name.substring(name.lastIndexOf(".")))
                    .orElse("");
            String newFileName = UUID.randomUUID() + ext;
            File destFile = new File(UPLOAD_DIR + File.separator + newFileName);
            avatarFile.transferTo(destFile);
            user.setAvatar(normalizePath(newFileName));
            userRepo.save(user);
        }
        return user;
    }

    @Transactional
    public User getCurrentUserProfileAsUser() {
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");
        log.info("getCurrentUserProfileAsUser: user ID={}", user.getId());
        return user;
    }

    @Transactional // 이 메서드 자체는 @Transactional이므로, 트랜잭션 범위 내에서 동작합니다.
    public UserProfileDto getCurrentUserProfile() {
        User user = SecurityUserUtil.getCurrentLoggedInUser(); // SecurityUserUtil 사용
        if (user == null || user.getId() == null) {
            log.error("getCurrentUserProfile: 인증된 사용자를 찾을 수 없습니다.");
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }

        log.info("getCurrentUserProfile: user ID={}", user.getId());

        // LazyInitializationException을 해결하기 위해 현재 트랜잭션 내에서 User 엔티티를 다시 로드
        // 로드된 managedUser는 현재 트랜잭션의 영속성 컨텍스트에 의해 관리
        User managedUser = userRepo.findById(user.getId())
                                   .orElseThrow(() -> new IllegalArgumentException("User not found by ID: " + user.getId()));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(managedUser.getId()); // 관리되는 User 엔티티(managedUser) 사용
        dto.setUsername(managedUser.getUsername());
        dto.setNickname(managedUser.getNickname());
        dto.setPhone(managedUser.getPhone());
        dto.setRegion(managedUser.getRegion());
        dto.setAvatar(managedUser.getAvatar());
        dto.setMannerScore(managedUser.getMannerScore());

        // Theme 정보에 안전하게 접근 (이제 트랜잭션 내에서 초기화됨)
        if (managedUser.getTheme() != null) {
            dto.setThemeId(managedUser.getTheme().getId());
            dto.setTheme(managedUser.getTheme().getTheme()); // Theme가 이제 트랜잭션 내에서 초기화
        } else {
            dto.setThemeId(null);
            dto.setTheme(null); // 기본값 설정
        }
        
        dto.setJjamPoints(calculateUserJjamPoints(managedUser)); // 관리되는 User 엔티티(managedUser) 사용
        dto.setSocialType(managedUser.getSocialType() != null ? managedUser.getSocialType().name() : null);
        
        return dto;
    }

    public int calculateUserJjamPoints(User user) {
        return userJjamRepo.findByUser(user).stream()
                .mapToInt(UserJjam::getTransactionAmount)
                .sum();
    }

    public User findById(Long id) {
        return userRepo.findById(id).orElse(null);
    }
<<<<<<< HEAD
<<<<<<< HEAD
<<<<<<< HEAD
=======
    
    @Transactional
    public void withdrawCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userRepo.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with username: " + username);
        }
        User user = userOptional.get();
<<<<<<< HEAD

        // 고유한 UUID 생성
=======
        
        // 유저네임과 닉네임을 "탈퇴회원_UUID" 형식으로 변경
>>>>>>> 7e55b2a (탈퇴회원_UUID로 변경)
=======

    @Transactional
    public void withdrawCurrentUser(HttpServletRequest request, HttpServletResponse response) {
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");

>>>>>>> a103d41 (구글 로그인 시 챗봇 사용불가 홈페이지 깨짐 현상 수정, 구글 로그인 시 홈제외 모든 페이지 깨짐 현상)
        String uuid = UUID.randomUUID().toString();
<<<<<<< HEAD

        user.setUsername("탈퇴회원_" + uuid);
        user.setNickname("탈퇴회원_" + uuid);
        user.setEmail("deleted_" + uuid + "@example.com");
        user.setPassword(null);
        user.setPhone(null);
        user.setRegion(null);
        if (user.getAvatar() != null) {
            File file = new File(UPLOAD_DIR + File.separator + user.getAvatar());
            if (file.exists()) file.delete();
            user.setAvatar(null);
        }
        user.setTheme(null);
        user.setMarketingConsent(false);
        user.setMannerScore(0);
        user.setOnline(false);
        user.setSnsLogin(false);

<<<<<<< HEAD
        // 사용자 정보 저장
=======
        user.setUsername("탈퇴회원_" + uuid); // 유저네임 변경
        user.setNickname("탈퇴회원_" + uuid); // 닉네임 변경
=======
    
    // 현재 사용자 계정 탈퇴 처리: 개인 정보 삭제, 유저네임/닉네임 변경
    @Transactional
    public void withdrawCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userRepo.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with username: " + username); // 사용자 없음 예외
        }
        User user = userOptional.get();
        
        // 유저네임과 닉네임을 "탈퇴한 회원_UUID" 형식으로 변경
        String uuid = UUID.randomUUID().toString();
        user.setUsername("탈퇴회원_" + uuid);
        user.setNickname("탈퇴회원_" + uuid);
>>>>>>> fe7f247 (게시물, 리뷰, 채팅만 남겨놓음 탈퇴코드 controller, service에 추가)
        
        // 개인 정보 삭제
        user.setPassword(null); // 비밀번호 삭제
        user.setPhone(null); // 전화번호 삭제
        user.setRegion(null); // 지역 삭제
<<<<<<< HEAD
        // 프로필 이미지 삭제 (파일 시스템 및 데이터베이스)
        if (user.getAvatar() != null) {
            File file = new File(UPLOAD_DIR + File.separator + user.getAvatar());
            if (file.exists()) {
                file.delete(); // 파일 시스템에서 프로필 이미지 삭제
            }
            user.setAvatar(null); // 데이터베이스에서 avatar 필드 null로 설정
        }
=======
        user.setAvatar(null); // 프로필 이미지 삭제
>>>>>>> fe7f247 (게시물, 리뷰, 채팅만 남겨놓음 탈퇴코드 controller, service에 추가)
        user.setEmail(null); // 이메일 삭제
        user.setRealname(null); // 실제 이름 삭제
        user.setTheme(null); // 관심 상품 삭제
        user.setMarketingConsent(false); // 마케팅 동의 해제
        user.setMannerScore(0); // 매너 점수 초기화
        user.setOnline(false); // 온라인 상태 해제
        user.setSnsLogin(false); // 소셜 로그인 여부 해제
        
        // 사용자 정보 저장 (Product, Review, Chat은 유지)
<<<<<<< HEAD
>>>>>>> bededd5 (탈퇴한 회원 -> 탈퇴회원으로 변경)
=======
>>>>>>> a103d41 (구글 로그인 시 챗봇 사용불가 홈페이지 깨짐 현상 수정, 구글 로그인 시 홈제외 모든 페이지 깨짐 현상)
        userRepo.save(user);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
<<<<<<< HEAD
        if (auth != null) {
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
<<<<<<< HEAD
=======

        // 탈퇴 후 홈으로 리다이렉트
        try {
			response.sendRedirect(request.getContextPath() + "/");
		} catch (IOException e) {
			e.printStackTrace();
		}
>>>>>>> 05dfe10 (회원 탈퇴 후 로그아웃 -> 홈으로 리다이렉트 코드 추가)
    }
>>>>>>> 49abed9 (서버에서 JSON응답처리, JS에서 리다이렉트 처리)
=======
        userRepo.save(user);
    }
>>>>>>> fe7f247 (게시물, 리뷰, 채팅만 남겨놓음 탈퇴코드 controller, service에 추가)

=======
        if (auth != null) new SecurityContextLogoutHandler().logout(request, response, auth);
    }
>>>>>>> a103d41 (구글 로그인 시 챗봇 사용불가 홈페이지 깨짐 현상 수정, 구글 로그인 시 홈제외 모든 페이지 깨짐 현상)
}
