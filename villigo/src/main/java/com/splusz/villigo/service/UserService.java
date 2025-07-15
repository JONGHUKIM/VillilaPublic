package com.splusz.villigo.service;

import java.io.IOException;
import java.time.Duration;
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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.config.CustomAuthenticationSuccessHandler;
import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.SocialType;
import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.domain.UserJjam;
import com.splusz.villigo.domain.UserRole;
import com.splusz.villigo.dto.SocialUserSignUpDto;
import com.splusz.villigo.dto.UpdateAvatarRequestDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.dto.UserSignUpDto;
import com.splusz.villigo.repository.ThemeRepository;
import com.splusz.villigo.repository.UserJjamRepository;
import com.splusz.villigo.repository.UserRepository;
import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.storage.FileStorageService;
import com.splusz.villigo.util.SecurityUserUtil;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService implements UserDetailsService {
    private final UserRepository userRepo;
    private final ThemeRepository themeRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationSuccessHandler successHandler;
    private final UserJjamRepository userJjamRepo;
    private final S3FileStorageService s3FileStorageService;
    private final FileStorageService fileStorageService;

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
    public User updateUserProfile(String nickname, String password, String phone, String region, Long themeId, MultipartFile avatarFile) throws FileStorageException, IOException { // 예외 추가
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");

        if (nickname != null && !nickname.isEmpty()) user.setNickname(nickname);
        if (password != null && !password.isEmpty()) user.setPassword(passwordEncoder.encode(password));
        if (phone != null && !phone.isEmpty()) user.setPhone(phone);
        if (region != null && !region.isEmpty()) user.setRegion(region);
        if (themeId != null) user.setTheme(themeRepo.findById(themeId).orElse(null));

        if (avatarFile != null && !avatarFile.isEmpty()) {
            String oldAvatarS3Key = user.getAvatar(); // 기존 아바타 S3 키 가져오기

            // S3에 저장될 최종 키(경로 + 파일명) 생성: avatars/{userId}/UUID.확장자
            String uniqueFileNamePart = S3FileStorageService.createUniqueFileName(avatarFile.getOriginalFilename()); // S3FileStorageService의 static 헬퍼 사용
            String newAvatarS3Key = "avatars/" + user.getId() + "/" + uniqueFileNamePart;

            // S3에 새 아바타 업로드 (uploadFile은 이제 targetS3Key를 직접 받음)
            s3FileStorageService.uploadFile(
                avatarFile.getInputStream(),
                newAvatarS3Key, // <--- 최종 S3 키 전달
                avatarFile.getContentType()
            );
            user.setAvatar(newAvatarS3Key); // User 엔티티의 avatar 컬럼 업데이트

            // 기존 아바타 파일 S3에서 삭제 (선택 사항이지만 비용 절감 위해 권장)
            if (StringUtils.hasText(oldAvatarS3Key)) { // 기존 아바타가 있었다면
                // TODO: oldAvatarS3Key가 이 사용자의 것인지 추가 검증 (보안 강화)
                try {
                    s3FileStorageService.deleteFile(oldAvatarS3Key);
                    log.info("Old avatar deleted from S3: {}", oldAvatarS3Key);
                } catch (FileStorageException e) {
                    log.warn("Failed to delete old avatar from S3: {}. Error: {}", oldAvatarS3Key, e.getMessage());
                    // 삭제 실패해도 프로필 업데이트는 계속 진행 (치명적 오류 아님)
                }
            }
        }

        return userRepo.save(user); // 변경된 User 엔티티 저장 (아바타 S3 키 포함)
    }

    @Transactional
    public User updateAvatar(UpdateAvatarRequestDto requestDto) throws FileStorageException, IOException { // 예외 추가
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) throw new IllegalArgumentException("로그인된 사용자 정보를 찾을 수 없습니다.");

        MultipartFile avatarFile = requestDto.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String oldAvatarS3Key = user.getAvatar(); // 기존 아바타 S3 키 가져오기

            String uniqueFileNamePart = S3FileStorageService.createUniqueFileName(avatarFile.getOriginalFilename()); // static 메서드 호출
            String newAvatarS3Key = "avatars/" + user.getId() + "/" + uniqueFileNamePart;

            s3FileStorageService.uploadFile(
                avatarFile.getInputStream(),
                newAvatarS3Key, // <--- 최종 S3 키 전달
                avatarFile.getContentType()
            );
            user.setAvatar(newAvatarS3Key); // User 엔티티의 avatar 컬럼 업데이트

            // 기존 아바타 파일 S3에서 삭제
            if (StringUtils.hasText(oldAvatarS3Key)) {
                // TODO: oldAvatarS3Key가 이 사용자의 것인지 추가 검증 (보안 강화)
                try {
                    s3FileStorageService.deleteFile(oldAvatarS3Key);
                    log.info("Old avatar deleted from S3 (updateAvatar): {}", oldAvatarS3Key);
                } catch (FileStorageException e) {
                    log.warn("Failed to delete old avatar from S3 (updateAvatar): {}. Error: {}", oldAvatarS3Key, e.getMessage());
                }
            }
            userRepo.save(user); // 변경된 User 엔티티 저장
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

    @Transactional
    public UserProfileDto getCurrentUserProfile() throws FileStorageException { // FileStorageException 예외 추가
        User user = SecurityUserUtil.getCurrentLoggedInUser();
        if (user == null || user.getId() == null) {
            log.error("getCurrentUserProfile: 인증된 사용자를 찾을 수 없습니다.");
            throw new IllegalStateException("인증된 사용자를 찾을 수 없습니다.");
        }

        User managedUser = userRepo.findById(user.getId())
                                   .orElseThrow(() -> new IllegalArgumentException("User not found by ID: " + user.getId()));

        UserProfileDto dto = new UserProfileDto();
        dto.setId(managedUser.getId());
        dto.setUsername(managedUser.getUsername());
        dto.setNickname(managedUser.getNickname());
        dto.setPhone(managedUser.getPhone());
        dto.setRegion(managedUser.getRegion());
        dto.setAvatar(managedUser.getAvatar()); // S3 Key는 그대로 avatar 필드에 설정

        if (StringUtils.hasText(managedUser.getAvatar())) { // 아바타 S3 Key가 있다면
            try {
                // 5분 유효한 Pre-signed URL 생성
                String presignedUrl = s3FileStorageService.generateDownloadPresignedUrl(managedUser.getAvatar(), Duration.ofMinutes(5));
                dto.setAvatarImageUrl(presignedUrl); // DTO의 avatarImageUrl 필드에 설정
            } catch (FileStorageException e) {
                log.error("Error generating presigned URL for avatar {}: {}", managedUser.getAvatar(), e.getMessage());
                // 오류 발생 시 기본 이미지 URL 또는 null로 설정
                dto.setAvatarImageUrl("/images/default-avatar.png"); // TODO: 실제 기본 이미지 URL로 변경
            }
        } else {
            // 아바타 S3 Key가 없는 경우 (아바타 미설정)
            dto.setAvatarImageUrl("/images/default-avatar.png"); // TODO: 실제 기본 이미지 URL로 변경
        }

        // Theme 정보에 안전하게 접근 (이제 트랜잭션 내에서 초기화됨)
        if (managedUser.getTheme() != null) {
            dto.setThemeId(managedUser.getTheme().getId());
            dto.setTheme(managedUser.getTheme().getTheme());
        } else {
            dto.setThemeId(null);
            dto.setTheme(null);
        }

        dto.setJjamPoints(calculateUserJjamPoints(managedUser));
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
        
        if (StringUtils.hasText(user.getAvatar())) { // 아바타가 존재했다면
            try {
                s3FileStorageService.deleteFile(user.getAvatar()); // <--- S3에서 아바타 파일 삭제
                log.info("회원 탈퇴 중 S3에서 아바타 삭제됨: {}", user.getAvatar());
            } catch (FileStorageException e) {
            	log.warn("회원 탈퇴 중 S3에서 아바타 삭제 실패: {}. 오류: {}", user.getAvatar(), e.getMessage());
            }
            user.setAvatar(null); // DB에서 아바타 컬럼을 null로 설정
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
<<<<<<< HEAD
>>>>>>> a103d41 (구글 로그인 시 챗봇 사용불가 홈페이지 깨짐 현상 수정, 구글 로그인 시 홈제외 모든 페이지 깨짐 현상)
=======
    
    public UserProfileDto getUserProfileDto(User user) throws FileStorageException {
        if (user == null) {
            return UserProfileDto.builder()
                        .id(0L)
                        .username("unknown")
                        .nickname("알 수 없음")
                        .avatarImageUrl("/images/default-avatar.png")
                        .jjamPoints(0)
                        .mannerScore(36)
                        .build();
        }

        String avatarImageUrl;
        try {
            if (StringUtils.hasText(user.getAvatar())) { // user.getAvatar()가 S3 키를 가지고 있다면
                // FileStorageService를 통해 public URL 또는 presigned URL 생성
                // 여기서는 download presigned URL을 사용하고, 유효 기간은 5분으로 설정합니다.
                avatarImageUrl = fileStorageService.generateDownloadPresignedUrl(user.getAvatar(), Duration.ofMinutes(5));
            } else {
                avatarImageUrl = "/images/default-avatar.png"; // 아바타가 없으면 기본 이미지
            }
        } catch (FileStorageException e) {
            log.error("사용자 아바타 URL 생성 중 오류 발생 (User ID: {}): {}", user.getId(), e.getMessage());
            avatarImageUrl = "/images/default-avatar.png";
        }

        // Theme 객체에서 getName() 대신 getTheme()을 호출합니다.
        // UserProfileDto의 'theme' 필드는 테마 이름을 나타내는 String 타입이므로,
        // Theme 엔티티의 'theme' 필드(아마도 String 타입)를 사용합니다.
        String themeName = null;
        if (user.getTheme() != null) {
            // Theme 엔티티에 테마 이름을 저장하는 필드가 'theme'라고 가정
            // 만약 'name'이나 'title'이라면 user.getTheme().getName() 또는 user.getTheme().getTitle() 사용
            themeName = user.getTheme().getTheme(); // Theme 엔티티에 getName()이 없으므로 getTheme() 사용
        }

        return UserProfileDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .region(user.getRegion())
                .avatar(user.getAvatar())
                .avatarImageUrl(avatarImageUrl)
                .themeId(user.getTheme() != null ? user.getTheme().getId() : null)
                .theme(themeName) // 수정된 themeName 변수 사용
                .jjamPoints(calculateUserJjamPoints(user)) // score 대신 calculateUserJjamPoints() 호출
                .mannerScore(user.getMannerScore())
                .socialType(user.getSocialType() != null ? user.getSocialType().name() : null)
                .build();
    }
<<<<<<< HEAD
>>>>>>> 87dc779 (채팅 S3로 전환, 유저 상세보기 수정)
=======
    
    // --- 임시: 특정 사용자에게 ADMIN 역할 부여 메서드 ---
    @Transactional
    public void addAdminRoleToUser(Long userId) {
        // User user = SecurityUserUtil.getCurrentLoggedInUser(); // <--- 이 줄을 제거
        User user = userRepo.findById(userId) // <--- userId로 직접 사용자 조회
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        if (!user.getRoles().contains(UserRole.ADMIN)) {
            user.addRole(UserRole.ADMIN);
            userRepo.save(user);
            log.warn("🚨 사용자 {} (ID: {})에게 ADMIN 역할이 임시로 부여되었습니다. 마이그레이션 후 원래대로 되돌려주세요!", user.getUsername(), user.getId());
        } else {
            log.info("사용자 {} (ID: {})는 이미 ADMIN 역할을 가지고 있습니다.", user.getUsername(), user.getId());
        }
    }

    // --- 임시: 특정 사용자에서 ADMIN 역할 제거 메서드 ---
    @Transactional
    public void removeAdminRoleFromUser(Long userId) {
        // User user = SecurityUserUtil.getCurrentLoggedInUser(); // <--- 이 줄을 제거
        User user = userRepo.findById(userId) // <--- userId로 직접 사용자 조회
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        if (user.getRoles().contains(UserRole.ADMIN)) {
            user.removeRole(UserRole.ADMIN);
            userRepo.save(user);
            log.warn("✅ 사용자 {} (ID: {})에게서 ADMIN 역할이 제거되었습니다.", user.getUsername(), user.getId());
        } else {
            log.info("사용자 {} (ID: {})는 ADMIN 역할을 가지고 있지 않습니다.", user.getUsername(), user.getId());
        }
    }
>>>>>>> 30a18af (데이터 레벨을 위한 마이그레이션 (UserService, Controller에 있는 임시 코드는 삭제예정))
}
