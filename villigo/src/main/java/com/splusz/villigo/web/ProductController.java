package com.splusz.villigo.web;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.splusz.villigo.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class ProductController {
	
	private final ProductService productService;
	
	@GetMapping("/api/productfee")
	public ResponseEntity<Integer> getProductFee(@RequestParam(name = "id") Long id) {
		log.info("getProductFee(id={})", id);
		
		int fee = productService.readFeeById(id);
		log.info("대여 요금 조회 결과: ", fee);
		
		return ResponseEntity.ok(fee);
	}
}
