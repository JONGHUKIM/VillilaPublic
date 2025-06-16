package com.splusz.villigo.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.hibernate.annotations.NaturalId;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "USERS")
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // 🚀 Hibernate 프록시 제거
public class User extends BaseTimeEntity implements UserDetails {
    
    private static final long serialVersionUID = 2167210764923357271L;

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NaturalId
    @EqualsAndHashCode.Include
    private String username;
    
    @JsonIgnore
    private String password;
    
    @Basic(optional = false)
    private String email;
    
    private String realname;
    
    @Basic(optional = false)
    private String nickname;
    
    private String phone;
    
    private String region;
    
    private String avatar; // 유저 프로필 사진 파일 경로
    
    private Boolean snsLogin; // SNS계정(구글 등) 연결 여부(T/F)
    
    @ToString.Exclude 
    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "theme_id") 
    @Basic(optional = false)
    @JsonIgnore // 🚀 Lazy Loading 프록시 문제 방지
    private Theme theme;
    
    @Basic(optional = false)
    @Column(insertable = false)
    private int score;

    @Builder.Default
    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    private Set<UserRole> roles = new HashSet<UserRole>();
    
    @Column(name = "is_online", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isOnline;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @JsonIgnore
    private List<ChatRoomParticipant> chatRoomParticipants = new ArrayList<>();
    
    public List<ChatRoom> getChatRooms() {
        return chatRoomParticipants.stream()
                .map(ChatRoomParticipant::getChatRoom)
                .toList();
    }
    
    public User update(String realname, String phone, String region, Theme theme) {
        this.realname = realname;
        this.phone = phone;
        this.region = region;
        this.theme = theme;
        return this;
    }
    
    public User addRole(UserRole role) {
    	roles.add(role);
    	return this;
    }
    
    public User removeRole(UserRole role) {
    	roles.remove(role);
    	return this;
    }
    
    public User clearRoles() {
    	roles.clear();
    	return this;
    }
    
    // 매너점수 필드 추가
    @Column(name = "manner_score", nullable = false, columnDefinition = "INT DEFAULT 36", insertable = false)
    private int mannerScore; // 기본값 36점으로 설정

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		log.info("getAuthorities()");
		
		List<SimpleGrantedAuthority> authorities = roles.stream()
				.map(r -> new SimpleGrantedAuthority(r.getAuthority()))
				.toList();
		
		return authorities;
	}
	
    // Marketing Consent 필드 추가
    @Column(name = "marketing_consent", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean marketingConsent;
	
	// UserDetails 인터페이스 메서드 구현
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
