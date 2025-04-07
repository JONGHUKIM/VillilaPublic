package com.splusz.villigo.strategy;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class UpperCaseTableNamingStrategy extends PhysicalNamingStrategyStandardImpl{

	/**
	 * 
	 */
	private static final long serialVersionUID = -3148440905131512884L;

	@Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // 기본 변환을 먼저 적용한 후
        Identifier identifier = super.toPhysicalTableName(name, context);
        // 테이블 이름만 대문자로 변환
        return Identifier.toIdentifier(identifier.getText().toUpperCase());
	}	
}
