package com.splusz.villigo.domain;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Getter
@ToString
@EqualsAndHashCode
@MappedSuperclass
// 다른 엔터티 클래스의 상위 클래스로 사용됨. 분리된 테이블과 매핑되는 엔터티는 아님.
@EntityListeners(AuditingEntityListener.class)
// 엔터티 생성/수정 이벤트를 처리하는 리스너 설정.
// 엔터티 (최초) 생성 시간, 최종 수정 시간을 처리하는(자동 저장/수정) 리스너 설정.
public class BaseTimeEntity {

	@CreatedDate // 엔터티 최초 생성 시간을 저장하는 필드.
	private LocalDateTime createdTime;
	
	@LastModifiedDate // 엔터티 최종 수정 시간을 저장하는 필드.
	private LocalDateTime modifiedTime;
}