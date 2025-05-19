package com.splusz.villigo.dto;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Data;

@Data
public class RentalImageCreateDto {

    private Long productId;
    @JsonIgnore
    private List<MultipartFile> images;
}
