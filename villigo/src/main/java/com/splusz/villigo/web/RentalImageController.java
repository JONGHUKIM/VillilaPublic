package com.splusz.villigo.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/images")
public class RentalImageController {

    private static final String RENTAL_IMAGE_PATH = "C:/images/rentals/";

    @GetMapping("/{imageName:.+}")
    public ResponseEntity<Resource> getRentalImage(@PathVariable String imageName) {
        log.info("GET /api/images/{}", imageName);
        try {
            Path filePath = Paths.get(RENTAL_IMAGE_PATH).resolve(imageName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.warn("Image not found or unreadable: {}", imageName);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Failed to load image: {}", imageName, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
