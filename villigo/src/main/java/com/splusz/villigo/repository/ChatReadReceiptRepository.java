package com.splusz.villigo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splusz.villigo.domain.ChatReadReceipt;

public interface ChatReadReceiptRepository extends JpaRepository<ChatReadReceipt, Long> {

	boolean existsByChatMessageId(Long messageId);

	boolean existsByChatMessageIdAndUserId(Long messageId, Long userId);

}
