package com.splusz.villigo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
		        .allowedOrigins(
		                "http://localhost:3000",
		                "http://localhost:8080",
		                "https://villila.store"
		            )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 대여 이미지
        registry.addResourceHandler("/images/rentals/**")
                .addResourceLocations("file:/home/ubuntu/images/rentals/");

        // 프로필 이미지
        registry.addResourceHandler("/images/avatar/**")
                .addResourceLocations("file:/home/ubuntu/images/avatar/");
    }

}