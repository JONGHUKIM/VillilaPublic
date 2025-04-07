package com.splusz.villigo.web;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.splusz.villigo.service.AddressService;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/address")
public class AddressController {
    
    private final AddressService addServ;

    @GetMapping("/latlng")
    public ResponseEntity<Map<String, Double>> getLatLng(@RequestParam(name = "addr") String address) {
        
        log.info("getLatLng(address={})", address);
        
        try {
            Map<String, Double> latlng = addServ.geocoding(address);
            log.info("latlng={}", latlng);
            return ResponseEntity.ok(latlng);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.ok(null);
        }
        
    }
}
