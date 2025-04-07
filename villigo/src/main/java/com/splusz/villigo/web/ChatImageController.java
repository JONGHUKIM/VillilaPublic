package com.splusz.villigo.web;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.service.ChatImageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatImageController {

    private final ChatImageService chatImageService;

    @Value("${file.upload-dir}") // 설정 파일에서 이미지 저장 경로 가져오기
    private String uploadDir;

    // 이미지 업로드 API
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
    	    @RequestParam(name = "file", required = true) MultipartFile file,
    	    @RequestParam(name = "roomId", required = true) Long roomId,
    	    @RequestParam(name = "senderId", required = true) Long senderId) {

        // 이미지 저장 및 파일명 반환
        String filename = chatImageService.saveImage(file, roomId, senderId);

        // 클라이언트가 접근할 수 있는 이미지 URL 반환
        String imageUrl = "/api/chat/images/" + filename;
        return ResponseEntity.ok(imageUrl);
    }
    
    @PostMapping("/upload/multiple")
    public ResponseEntity<List<String>> uploadMultipleImages(
            @RequestParam(name = "files") List<MultipartFile> files,
            @RequestParam(name = "roomId") Long roomId,
            @RequestParam(name = "senderId") Long senderId) {

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            String filename = chatImageService.saveImage(file, roomId, senderId);
            urls.add("/api/chat/images/" + filename);
        }
        return ResponseEntity.ok(urls);
    }


    // 이미지 가져오기 API
    @GetMapping("/images/{filename}")
    public ResponseEntity<Resource> getImage(@PathVariable(name = "filename") String filename) {
        try {
            // URL 디코딩
            String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
            Path filePath = Paths.get(uploadDir).resolve(decodedFilename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            // 파일 존재 여부 확인
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            // MIME 타입 설정
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
