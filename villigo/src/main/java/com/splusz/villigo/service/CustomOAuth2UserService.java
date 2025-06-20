package com.splusz.villigo.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.splusz.villigo.config.CustomOAuth2User;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepo;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User delegate = new DefaultOAuth2UserService().loadUser(request);
        Map<String, Object> attributes = delegate.getAttributes();

        String email = (String) attributes.get("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("이메일 정보가 없습니다.");
        }

        Optional<User> optionalUser = userRepo.findByEmail(email);
        User user = optionalUser.orElse(null);

        return new CustomOAuth2User(delegate, user); // DB에 없으면 user == null
    }
}

