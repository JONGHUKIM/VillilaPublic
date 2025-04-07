package com.splusz.villigo.web;

import com.splusz.villigo.domain.Faq;
import com.splusz.villigo.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/faqs")
public class FaqController {

    private final FaqRepository faqRepository;

    @GetMapping
    public List<Faq> getFaqs() {
        return faqRepository.findAll();  // DB에서 모든 FAQ 목록 가져오기
    }
}
