package com.splusz.villigo.domain;

public enum UserRole {
	USER("ROLE_USER"),
	ADMIN("ROLE_ADMIN"),
	GUEST("ROLE_GUEST");
	
	private String authority;
	
	UserRole(String authority) {
		this.authority = authority;
	}
	
	public String getAuthority() {
		return this.authority;
	}
}
