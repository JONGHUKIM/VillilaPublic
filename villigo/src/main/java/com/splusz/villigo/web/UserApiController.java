package com.splusz.villigo.web;

import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.UpdateAvatarRequestDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserApiController {

    private final UserService userService;
    
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> updateAvatar(@ModelAttribute UpdateAvatarRequestDto requestDto) throws IOException {
        User updatedUser = userService.updateAvatar(requestDto);
        return ResponseEntity.ok(updatedUser);
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileDto> getUserProfile() { // 반환 타입을 UserProfileDto로 변경
        try {
            UserProfileDto userProfile = userService.getCurrentUserProfile();
            //int jjamPoints = userService.calculateUserJjamPoints(userService.getCurrentUserProfileAsUser());

            // userProfile.setJjamPoints(jjamPoints); // UserProfileDto에 jjamPoints를 추가하거나, 필요하다면 별도 처리

            return ResponseEntity.ok(userProfile); // UserProfileDto를 직접 반환
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null);
        }
    }
    
    // 닉네임 중복 체크 API 추가
    @GetMapping("/check-nickname") 
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Boolean> checkNickname(@RequestParam(name = "nickname") String nickname) {
        try {
            UserProfileDto currentUser = userService.getCurrentUserProfile();
            if (currentUser.getNickname().equals(nickname)) {
                return ResponseEntity.ok(true);
            }
            boolean isAvailable = !userService.checkNickname(nickname); // 중복이면 false, 사용 가능하면 true
            return ResponseEntity.ok(isAvailable);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(null); // 인증 실패 시 401 반환
        }
    }
}
