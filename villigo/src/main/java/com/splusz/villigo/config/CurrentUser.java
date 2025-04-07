package com.splusz.villigo.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

// @Target: 애너테이션이 적용될 수 있는 요소를 제한합니다.
// ElementType.PARAMETER: 이 애너테이션은 메서드 매개변수에 적용될 수 있다는 것을 나타냅니다.
// ElementType.ANNOTATION_TYPE: 다른 애너테이션 위에 메타 애너테이션으로 사용할 수 있습니다.
@Target({ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
// @Retention: 애너테이션이 유지되는 기간을 설정합니다.
// RetentionPolicy.RUNTIME: 런타임 시에도 이 애너테이션의 정보가 유지되어 리플렉션을 통해 접근할 수 있습니다.
@Retention(RetentionPolicy.RUNTIME)
// @@AuthenticationPrincipal: Spring Security에서 인증된 사용자 정보를 주입받을 때 사용됩니다.
// expression: 특정 조건에 따라 값을 가져오도록 커스터마이즈합니다.
@AuthenticationPrincipal(expression = 
	"#this instanceof T(com.splusz.villigo.config.CustomOAuth2User) ? #this.getUser() : #this")
	//-> 현재 Principal 객체가 CustonOAuth2User의 인스턴스인지 확인 후
	//   참이면 객체의 getUser() 메서드를 호출하여 반환, 거짓이면 현재 객체를 그대로 반환합니다.
public @interface CurrentUser {
	// @AliasFor: 애너테이션의 속성을 재정의합니다.
	// annotation: 속성을 재정의할 애너테이션을 입력합니다.(이 코드에서는 @AuthenticationPrincipal)  
	@AliasFor(annotation = AuthenticationPrincipal.class)
	boolean errorOnInvalidType() default false;
}
