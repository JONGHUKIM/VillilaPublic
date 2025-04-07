package com.splusz.villigo.strategy;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class CustomNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
		// 기본 카멜케이스 -> 스네이크케이스 변환 적용
		Identifier identifier = super.toPhysicalTableName(name, context);
		// 테이블 이름만 대문자로 변환
		return Identifier.toIdentifier(identifier.getText().toUpperCase());
	}
	
    // 컬럼 이름은 상속받은 CamelCaseToUnderscoresNamingStrategy의 동작을 그대로 사용
    // (카멜케이스 -> 스네이크케이스 변환)
}
