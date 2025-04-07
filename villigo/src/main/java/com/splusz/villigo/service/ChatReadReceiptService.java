package com.splusz.villigo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.splusz.villigo.domain.ChatMessage;
import com.splusz.villigo.domain.ChatReadReceipt;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.ChatMessageRepository;
import com.splusz.villigo.repository.ChatReadReceiptRepository;
import com.splusz.villigo.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatReadReceiptService {

    private final ChatReadReceiptRepository readReceiptRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepo;

    @Transactional
    public void markMessageAsRead(Long messageId, Long userId) {
        if (hasBeenRead(messageId, userId)) {
            return;
        }

        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메시지입니다."));
        
        User user = userRepo.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        ChatReadReceipt receipt = new ChatReadReceipt();
        receipt.setChatMessage(chatMessage);
        receipt.setUser(user);
        readReceiptRepository.save(receipt);
    }

    public boolean hasBeenRead(Long messageId, Long userId) {
        return readReceiptRepository.existsByChatMessageIdAndUserId(messageId, userId);
    }
}
