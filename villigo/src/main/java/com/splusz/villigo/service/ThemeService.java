package com.splusz.villigo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.repository.ThemeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ThemeService {
	
	private final ThemeRepository themeRepo;
	
	// DB에 저장된 전체 테마 목록을 리스트로 리턴하는 메서드
	public List<Theme> read() {
		log.info("read()");
		
		List<Theme> result = themeRepo.findAll();
		return result;
	}
	
	// DB에 저장된 테마를 ID로 검색해서 리턴하는 메서드
	public Theme read(Long themeId) {
		log.info("read(id)", themeId);
		
		Theme result = themeRepo.findById(themeId).orElseThrow();
		return result;
	}
}
