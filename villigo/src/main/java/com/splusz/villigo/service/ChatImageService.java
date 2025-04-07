package com.splusz.villigo.service;

import com.splusz.villigo.domain.ChatImage;
import com.splusz.villigo.domain.ChatRoom;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.repository.ChatImageRepository;
import com.splusz.villigo.repository.ChatRoomRepository;
import com.splusz.villigo.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ChatImageService {

    private final ChatImageRepository chatImageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    
    @Value("${file.upload-dir}")
    private String uploadDir;

    public String saveImage(MultipartFile file, Long roomId, Long senderId) {
        try {
            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            Path filePath = uploadPath.resolve(fileName);
            file.transferTo(filePath.toFile());

            ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new IllegalArgumentException("채팅방이 존재하지 않습니다."));

            User sender = userRepository.findById(senderId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자가 존재하지 않습니다."));
            
            ChatImage chatImage = new ChatImage();
            chatImage.setChatRoom(chatRoom);
            chatImage.setSender(sender);
            chatImage.setFilePath(fileName);

            chatImageRepository.save(chatImage);

            return fileName;
        } catch (IOException e) {
            throw new RuntimeException("파일 저장 중 오류 발생", e);
        }
    }
}
