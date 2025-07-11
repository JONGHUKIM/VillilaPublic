package com.splusz.villigo.config;

import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.splusz.villigo.domain.User;

import lombok.ToString;

@ToString
public class CustomOAuth2User implements OAuth2User {
	
	private final OAuth2User delegate; // 기존 OAuth2User 정보
	private final User user; // OAuth2로 로그인한 사용자의 필수정보 입력 여부(T/F)
	
	public CustomOAuth2User(OAuth2User delegate, User user) {
		this.delegate = delegate;
		this.user = user;
	}
	
	public User getUser() {
		return user;
	}
	
	// delegate 필드에 접근하기 위한 getter 메서드를 추가
    public OAuth2User getDelegate() {
        return delegate;
    }
    
	public Long getId() {
    	return user != null ? user.getId() : null;
    }
	
    @Override
    public Map<String, Object> getAttributes() {
        return delegate.getAttributes(); // 기본 속성 유지
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return delegate.getAuthorities();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }
    
    // 실제 사용자의 닉네임을 반환
    public String getDisplayUsername() {
        if (user != null && user.getNickname() != null && !user.getNickname().isEmpty()) {
            return user.getNickname(); // 닉네임이 있다면 닉네임 반환
        }
        if (user != null && user.getUsername() != null && !user.getUsername().isEmpty()) {
            return user.getUsername(); // 닉네임이 없다면 username 반환
        }
        return getName(); // 둘 다 없으면 OAuth2 기본 ID 반환 (폴백)
    }
    
	public boolean isProfileComplete() {
		// User 객체가 null이 아니고, 해당 User 객체의 isProfileComplete()가 true
		return user != null && user.isProfileComplete(); 
	}
}
