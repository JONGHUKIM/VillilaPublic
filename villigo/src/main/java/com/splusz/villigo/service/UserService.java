package com.splusz.villigo.service;

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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.config.CustomAuthenticationSuccessHandler;
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

import jakarta.transaction.Transactional;
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
	
	// 프로필 사진이 저장될 경로
	private static final String UPLOAD_DIR = "C:\\images\\avatar";

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("loadUserByUsername(username = {})", username);
        
        Optional<User> entity = userRepo.findByUsername(username);
        if (entity.isPresent()) {
            return entity.get();
        } else {
            throw new UsernameNotFoundException(username + "과(와) 일치하는 사용자 정보가 없습니다.");
        }
    }

    // 회원가입 서비스
    public User create(UserSignUpDto dto) {
        log.info("create(dto={})", dto);
        
        Theme theme = themeRepo.findById(dto.getThemeId()).orElseThrow();
        User entity = dto.toEntity(passwordEncoder, theme).addRole(UserRole.USER);
        entity = userRepo.save(entity);

        return entity;
    }

    // 소셜 계정 연결 회원가입 서비스
    public User create(SocialUserSignUpDto dto, String nickname, String realname, String email) {
        log.info("create(dto={}, nickname={}, realname={}, email={})", 
                dto, nickname, realname, email);
        
        Theme theme = themeRepo.findById(dto.getThemeId()).orElseThrow();
        User entity = dto.toEntity(theme).addRole(UserRole.USER);
        // 사용자 아이디, 닉네임, 이름, 이메일, 소셜 회원가입 여부(snsLogin)를 엔터티에 추가
        entity.setUsername(email.split("@")[0]); // 이메일에서 아이디 부분만 추출
        entity.setNickname(nickname);
        entity.setRealname(realname); 
        entity.setEmail(email);
        entity.setSnsLogin(true);
        entity = userRepo.save(entity);

        return entity;
    }

    // Id로 유저를 검색
    public User read(Long id) {
        log.info("read(id={})", id);
        
        User user = userRepo.findById(id).orElseThrow();
        return user;
    }

    // 소셜 로그인 사용자의 정보가 데이터베이스에 저장되어있는지 확인
    public String checkSnsLogin(Authentication authentication) {
        log.info("checkSnsLogin(CustomAuthenticationSuccessHandler={})", successHandler);
        
        boolean isProfileCompleted = successHandler.customizeOAuth2User(authentication);
        
        // 필수 정보가 데이터베이스에 입력이 되어있으면 홈페이지로 리다이렉트
        if (isProfileCompleted) {
            return "redirect:/";
        }
                
        return "/member/signup-social";
    }

    // 아이디(username) 중복 체크
    public Boolean checkUsername(String username) {
        log.info("checkUsername(username={})", username);
        
        Optional<User> entity = userRepo.findByUsername(username);
        if (entity.isPresent()) return true;
        
        return false;
    }

    // 닉네임 중복체크
    public Boolean checkNickname(String nickname) {
        log.info("checkNickname(nickname={})", nickname);
        
        Optional<User> entity = userRepo.findByNickname(nickname);
        if (entity.isPresent()) return true;
        
        return false;
    }

    // 이메일 중복 체크
    public Boolean checkEmail(String email) {
        log.info("checkEmail(email={})", email);
        
        Optional<User> entity = userRepo.findByEmail(email);
        if (entity.isPresent()) return true;
        
        return false;
    }

    // 채팅에서 사용자가 온라인/오프라인 구분 메서드 추가
    public void setUserOnline(Long userId, boolean isOnline) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        user.setOnline(isOnline);
        userRepo.save(user);
    }

    public boolean isUserOnline(Long userId) {
        log.info("isUserOnline(userId={})", userId);
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return user.isOnline();
    }

    // 매너 점수 조회 
    public int getMannerScore(Long userId) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        return user.getMannerScore(); // 매너 점수 반환
    }

    // 매너 점수 업데이트
    public void updateMannerScore(Long userId, int scoreDelta) {
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        int currentScore = user.getMannerScore();  // 현재 매너 점수
        int newScore = currentScore + scoreDelta;  // 점수 변경 (양수나 음수로 추가하거나 차감)

        // 점수는 0점 이상으로 유지하도록 설정
        user.setMannerScore(Math.max(newScore, 0));  // 최소값 0 설정
        
        userRepo.save(user);  // 업데이트된 매너 점수 저장
    }
	// 유저 프로필 업데이트
	@Transactional
    public User updateUserProfile(String nickname, String password, String phone, String region, Long themeId, MultipartFile avatarFile) throws IOException {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> userOptional = userRepo.findByUsername(username);

        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with username: " + username);
        }

        User user = userOptional.get();

        // 닉네임 업데이트 (중복 체크는 프론트엔드에서 이미 처리됨)
        if (nickname != null && !nickname.isEmpty()) {
            user.setNickname(nickname);
        }

        // 비밀번호 업데이트
        if (password != null && !password.isEmpty()) {
            user.setPassword(passwordEncoder.encode(password));
        }

        // 전화번호 업데이트
        if (phone != null && !phone.isEmpty()) {
            user.setPhone(phone);
        }

        // 지역 업데이트
        if (region != null && !region.isEmpty()) {
            user.setRegion(region);
        }

        // 관심 상품 업데이트
        if (themeId != null) {
            user.setTheme(themeRepo.findById(themeId).orElse(null));
        }

        // 아바타 이미지 업데이트
        if (avatarFile != null && !avatarFile.isEmpty()) {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFilename = avatarFile.getOriginalFilename();
            String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
            String newFileName = UUID.randomUUID() + fileExtension;
            String filePath = UPLOAD_DIR + File.separator + newFileName;

            File destFile = new File(filePath);
            avatarFile.transferTo(destFile);

            user.setAvatar(newFileName);
        }

        return userRepo.save(user);
    }
	
	public static String normalizePath(String path) {
	    if (path != null && path.startsWith("/")) {
	        return path.substring(1);
	    }
	    return path;
	}

    // 사진 업데이트
	@Transactional
	public User updateAvatar(UpdateAvatarRequestDto requestDto) throws IOException {
	    String username = SecurityContextHolder.getContext().getAuthentication().getName();
	    Optional<User> userOptional = userRepo.findByUsername(username);
	    if (userOptional.isEmpty()) {
	        throw new IllegalArgumentException("User not found with username: " + username);
	    }
	    User user = userOptional.get();
	    MultipartFile avatarFile = requestDto.getAvatarFile();
	    if (avatarFile != null && !avatarFile.isEmpty()) {
	        File directory = new File(UPLOAD_DIR);
	        if (!directory.exists()) {
	            directory.mkdirs();
	        }
	        String originalFilename = avatarFile.getOriginalFilename();
	        String fileExtension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : "";
	        String newFileName = UUID.randomUUID() + fileExtension;
	        String filePath = UPLOAD_DIR + File.separator + newFileName;
	        File destFile = new File(filePath);
	        avatarFile.transferTo(destFile);
	        user.setAvatar(normalizePath(newFileName)); // 정규화 적용
	        userRepo.save(user);
	    }
	    return user;
	}
	
	@Transactional
	public User getCurrentUserProfileAsUser() {
	    String username = SecurityContextHolder.getContext().getAuthentication().getName();
	    log.info("getCurrentUserProfileAsUser: username={}", username);
	    return userRepo.findByUsernameWithTheme(username)
	            .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));
	}
    
    // 현재 사용자 프로필 가져오기
	@Transactional
	public UserProfileDto getCurrentUserProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.error("No authenticated user found for getCurrentUserProfile");
            throw new IllegalStateException("No authenticated user found");
        }

	    String username = auth.getName();
	    log.info("getCurrentUserProfile: username={}", username);
        if (username == null || username.isEmpty()) {
            log.error("Username is null or empty in getCurrentUserProfile");
            throw new IllegalStateException("Username is null or empty");
        }

	    User user = userRepo.findByUsername(username)
	            .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

	    UserProfileDto dto = new UserProfileDto();
	    dto.setId(user.getId());
	    dto.setUsername(user.getUsername());
	    dto.setNickname(user.getNickname());
	    dto.setPhone(user.getPhone());
	    dto.setRegion(user.getRegion());
	    dto.setAvatar(user.getAvatar());
	    dto.setMannerScore(user.getMannerScore());
	    if (user.getTheme() != null) {
	        dto.setThemeId(user.getTheme().getId());
	        dto.setTheme(user.getTheme().getTheme());
	    }
	    dto.setJjamPoints(calculateUserJjamPoints(user)); // 잼 포인트 계산 후 dto에 설정

	    return dto;
	}
   
	// 사용자의 JJAM 포인트 총합 계산 메서드 추가
    public int calculateUserJjamPoints(User user) {
        List<UserJjam> userJjams = userJjamRepo.findByUser(user);
        return userJjams.stream()
                .mapToInt(UserJjam::getTransactionAmount)
                .sum();
    }
    
    public User findById(Long id) {
        return userRepo.findById(id).orElse(null);
    }

}
