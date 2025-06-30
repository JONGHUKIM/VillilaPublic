package com.splusz.villigo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/member")
public class LegalController {

    @GetMapping("/terms")
    public String showTerms() {
        return "member/terms"; // templates/terms.html을 찾아 렌더링
    }

    @GetMapping("/privacy")
    public String showPrivacy() {
        return "member/privacy"; // templates/privacy.html을 찾아 렌더링
    }
}
