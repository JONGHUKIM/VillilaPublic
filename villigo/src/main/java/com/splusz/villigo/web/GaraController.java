package com.splusz.villigo.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class GaraController {
   
   // html과 css를 확인하기 위한 컨트롤러 임니다. 
   // 가라 컨트롤러 임니다
   // 맵핑이 완료되시면 지우면 알려주세용! 
   
   @GetMapping("/post/category")
   public void  postCategory() {

   }
   
   @GetMapping("/jjam/shop")
   public void jjamShop() {
      
   }
   
   @GetMapping("/review")
   public void review() {
	   
   }
}
