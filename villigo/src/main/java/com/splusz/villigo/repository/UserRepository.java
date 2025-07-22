package com.splusz.villigo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.splusz.villigo.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
	
	@EntityGraph(attributePaths = "roles")
	Optional<User> findByUsername(String username);
	
	Optional<User> findByNickname(String nickname);
	
	Optional<User> findByEmail(String email);

	Optional<User> findByPhone(String phone);
	
	@Query("SELECT u FROM User u LEFT JOIN FETCH u.theme WHERE u.username = :username")
	Optional<User> findByUsernameWithTheme(String username);
	
    // S3 아바타 키를 가져오는 쿼리
    @Query("SELECT u.avatar FROM User u WHERE u.avatar IS NOT NULL")
    List<String> findAllAvatarKeys();
	
}
