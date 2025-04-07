package com.splusz.villigo.service;

import com.splusz.villigo.domain.Faq;
import com.splusz.villigo.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FaqService {
    private final FaqRepository faqRepository;

    public Optional<String> getAnswer(String userInput) {
        // 사용자가 입력한 질문을 단어별로 분리
        List<String> keywords = Arrays.asList(userInput.split("\\s+"));

        // 키워드 리스트를 하나씩 검사하여 FAQ 질문과 일치하는지 확인
        for (String keyword : keywords) {
            Optional<Faq> faq = faqRepository.findByQuestionContaining(keyword);
            if (faq.isPresent()) {
                return Optional.of(faq.get().getAnswer());
            }
        }

        return Optional.empty(); // FAQ 데이터에 없는 경우
    }
}
