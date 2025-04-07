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
    
    public boolean isProfileComplete() {
        return user != null;
    }
}
