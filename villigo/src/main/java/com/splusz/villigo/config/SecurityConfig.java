package com.splusz.villigo.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
//-> 스프링 컨테이너에서 생성하고 관리하는 설정 컴포넌트
//-> 스프링 컨테이너에서 필요한 곳에 의존성 주입해 줄 수 있음

@EnableMethodSecurity
//-> 각각의 컨트롤러 메서드에서 인증(로그인), 권한 설정을 하기 위해서 
public class SecurityConfig {
	
	@Autowired
	private HttpsEnforcedRedirectStrategy redirectStrategy;

	
	// Spring Security 5 버전부터 비밀번호는 반드시 암호화(encoding)를 해야만 함 
	// 만약 비밀번호를 암호화하지 않으면 HTTP 403(access denied, 접근 거부) 또는
	// HTTP 500 (internal server error, 내부 서버 오류) 에러 발생
	// 비밀번호를 암호화하는 객체를 스프링 컨테이너가 빈으로 관리해야 함 
	@Bean //-> 스프링 컨테이너에서 관리하는 객체(빈)-> 의존성 주입
	PasswordEncoder passwordEncoder() {
		log.info("BCryptPasswordEncoder()생성");
		return new BCryptPasswordEncoder();
	}
	/*
	 *  UserDetailsService:
	 *  사용자 관리(로그인,로그아웃,회원가입 등)를 위한 서비스 인터페이스
	 *  스프링 부트 애플리케이션에서 스프링 시큐리티를 이용한 로그인/ 로그아웃 서비스를 구현하려면 
	 *  (1) UserDetailsService 인터페이스를 구현하는 서비스 클래스와
	 *  (2) UserDetails 인터페이스를 구현하는 엔터티 클래스가 있어야 함 
	 *  사용자(User) 엔터티와 리포지토리와 사용자 서비스를 구현하기 전에 테스트 용도로 사용할 코드
	 */

	/*
	 * SecurityFilterChain: 스프링 시큐리티 필터 체인 객체(bean)
	 * - 로그인/로그아웃, 인증 필터에서 필요한 설정들을 구성
	 * - 로그인 페이지(뷰), 로그아웃 페이지 설정 
	 * - 페이지 접근 권한(admin,user,...) 설정
	 * - 인증 설정(로그인 없이 접근 가능한 페이지 vs 로그인해야만 접근할 수 있는 페이지)
	 */
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, UserProfileCompletionFilter userProfileCompletionFilter,
			CustomAuthenticationSuccessHandler oauth2SuccessHandler) throws Exception {
		log.info("SecurityFilterChain()생성");
		
		// CSRF 기능 비활성화 
		http.csrf((csrf) -> csrf.disable());
		
		// CORS 설정
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		
	    // iframe 허용 설정
	    http.headers(headers -> headers
	        .frameOptions(frameOptions -> frameOptions.sameOrigin())
	    );

		// 커스텀 로그인 HTML 페이지를 사용
		http.formLogin((login) -> login
				.loginPage("/member/signin")
				.loginProcessingUrl("/member/signin")
				.defaultSuccessUrl("/home", true)
				.permitAll()
		);
		
	    http.exceptionHandling(exception -> 
        exception.authenticationEntryPoint((request, response, authException) -> {
            // redirectStrategy 사용
            redirectStrategy.sendRedirect(request, response, "/member/signin");
        })
    );
		
		// 구글 로그인 인증 설정
		http
			.authorizeHttpRequests(authorize -> authorize
					.requestMatchers("/", "/login", "/error", "/search", "/api/search", "/api/brand",
							"/member/signin", "/member/signup", "/member/signup-social", 
							"/member/agreement", // /member/agreement 허용
							"/member/checkusername", "/member/checkemail", "/member/checknickname",
							"/member/checkphone",
							"/api/faqs", "/css/**", "/js/**", "/images/**", "/ws/**", "/sockjs/**","/fonts/**",
							"/queue/**","/bot/**").permitAll()
					.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
					.loginPage("/member/signin")
					.defaultSuccessUrl("/home", true)
					.failureUrl("/member/signin?error=true")
					.successHandler(oauth2SuccessHandler)
			)
			.logout(logout -> logout
					.logoutSuccessUrl("/")
					.clearAuthentication(true)
					.invalidateHttpSession(true)
			)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			// 필터 추가
			.addFilterAfter(userProfileCompletionFilter, SecurityContextPersistenceFilter.class);

		return http.build(); // SecurityFilterChain을 생성해서 리턴
	}
	
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
	    CorsConfiguration config = new CorsConfiguration();
	    config.setAllowedOrigins(List.of(
	    	    "http://localhost:3000",
	    	    "http://localhost:8080",
	    	    "https://villila.store"
	    	));
	    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
	    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "x-requested-with"));
	    config.setExposedHeaders(List.of("Authorization"));
	    config.setAllowCredentials(true);
	    config.setMaxAge(3600L);

	    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	    source.registerCorsConfiguration("/**", config);
	    return source;
	}
	
	@Bean
	public FilterRegistrationBean<ForwardedHeaderFilter> forwardedHeaderFilter() {
	    FilterRegistrationBean<ForwardedHeaderFilter> filter = new FilterRegistrationBean<>();
	    filter.setFilter(new ForwardedHeaderFilter());
	    filter.setOrder(0); // 우선순위 높게
	    return filter;
	}
	
}
