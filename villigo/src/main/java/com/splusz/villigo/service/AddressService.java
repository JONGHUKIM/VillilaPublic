package com.splusz.villigo.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.dto.AddressCreateDto;
import com.splusz.villigo.dto.AddressUpdateDto;
import com.splusz.villigo.repository.AddressRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {
    
    private final AddressRepository addRepo;
    private final ObjectMapper objectMapper;

   // @Value("${naver.api.client-id}")
   // private String clientId;
   // @Value("${naver.api.client-secret}")
   // private String clientSecret;
    @Value("${kakao.restapi}")
    private String kakaoRestApi;

    public Address create(Product product, AddressCreateDto dto) throws Exception {
        Map<String, Double> location = geocoding(dto.getFullAddress());
        Address entity = Address.builder()
            .product(product)
            .sido(dto.getSido())
            .fullAddress(dto.getFullAddress())
            .latitude(location.get("latitude"))
            .longitude(location.get("longitude"))
            .sigungu(dto.getSigungu())
            .zonecode(dto.getZonecode())
            .bname(dto.getBname())
            .build();

        addRepo.save(entity);

        return entity;
    }
    
    public Map<String, Double> geocoding(String address) throws Exception {
        String url = "https://dapi.kakao.com/v2/local/search/address.json?query=" + address;

        // 헤더에 인증 정보와 Accept 설정 추가
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoRestApi);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        
        RestTemplate restTemplate = new RestTemplate();

        // API 호출: GET 요청
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        
        // 응답 JSON 파싱
        JsonNode rootNode = objectMapper.readTree(response.getBody());
        log.info("rootNode = {}", rootNode);
        JsonNode documentsNode = rootNode.path("documents");
        log.info("documentsNode = {}", documentsNode);

        Map<String, Double> result = new HashMap<>();
        if (documentsNode.isArray() && documentsNode.size() > 0) {
            JsonNode firstAddress = documentsNode.get(0).path("address");
            result.put("longitude", firstAddress.path("x").asDouble());
            result.put("latitude", firstAddress.path("y").asDouble());
        }
        log.info("result={}", result);
        return result;
    }
    
//    위도 경도로 주소 얻어오기
//    public Map<String, Double> reverseGeocoding(Double lat, Double lng) throws Exception {
//        String apiURL = "https://dapi.kakao.com/v2/local/geo/coord2regioncode.json"
//        			+ "?x=" + lng + "&y=" + lng;
//
//        // 헤더에 인증 정보와 Accept 설정 추가
//        URL url = new URL(apiURL);
//        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//        conn.setRequestMethod("GET");
//        // 헤더에 카카오 REST API 키를 포함
//        conn.setRequestProperty("Authorization", "KakaoAK " + url);
//        
//        int responseCode = conn.getResponseCode();
//        BufferedReader br;
//        if (responseCode == 200) {  // 정상 호출
//            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//        } else {  // 에러 발생 시
//            br = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
//        }
//        
//        String inputLine;
//        StringBuilder response = new StringBuilder();
//        while ((inputLine = br.readLine()) != null) {
//            response.append(inputLine);
//        }
//        br.close();
//        
//        // 결과 출력 (JSON 형식)
//        System.out.println("Response: " + response.toString());
//    }

    public Address readByProductId(Long productId) {
        Address address = addRepo.findByProductId(productId);
        return address;
    }

    public void deleteByProductId(Long productId) {
        addRepo.deleteById(productId);
    }

    @Transactional
    public Address update(Long productId, AddressUpdateDto addDto) {
        Address entity = addRepo.findByProductId(productId);
        log.info("entity={}", entity);
        entity.update(addDto);
        log.info("updated entity={}", entity);

        return entity;
    }
}
