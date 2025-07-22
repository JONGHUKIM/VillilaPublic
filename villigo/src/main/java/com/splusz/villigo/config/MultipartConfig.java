package com.splusz.villigo.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class MultipartConfig {

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 파일 1개당 최대 크기 (application.properties와 동일하게 설정)
        factory.setMaxFileSize(DataSize.ofMegabytes(50L));
        
        // 요청 전체의 최대 크기 (application.properties와 동일하게 설정)
        factory.setMaxRequestSize(DataSize.ofMegabytes(50L));
        
        return factory.createMultipartConfig();
    }
}