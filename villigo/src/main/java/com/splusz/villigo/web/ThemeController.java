package com.splusz.villigo.web;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.domain.Theme;
import com.splusz.villigo.repository.ThemeRepository;

@RestController
@RequestMapping("/api")
public class ThemeController {

    @Autowired
    private ThemeRepository themeRepo;

    @GetMapping("/themes")
    public List<Theme> getAllThemes() {
        return themeRepo.findAll();
    }
}
