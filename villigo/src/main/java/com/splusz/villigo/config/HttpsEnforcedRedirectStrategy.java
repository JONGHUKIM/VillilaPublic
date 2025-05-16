package com.splusz.villigo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.stereotype.Component;

import java.io.IOException;

// 강제로 http 요청을 https로 변환
@Component
public class HttpsEnforcedRedirectStrategy implements RedirectStrategy {

    private final RedirectStrategy delegate = new DefaultRedirectStrategy();

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        if (url.startsWith("http://")) {
            url = url.replace("http://", "https://");
        }
        delegate.sendRedirect(request, response, url);
    }
}

