package com.splusz.villigo.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Bag;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.AddressCreateDto;
import com.splusz.villigo.dto.AddressUpdateDto;
import com.splusz.villigo.dto.BagCreateDto;
import com.splusz.villigo.dto.BagUpdateDto;
import com.splusz.villigo.dto.ProductUpdateDto;
import com.splusz.villigo.dto.RentalImageCreateDto;
import com.splusz.villigo.service.AddressService;
import com.splusz.villigo.service.BagService;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.RentalImageService;
import com.splusz.villigo.service.ReservationService;
import com.splusz.villigo.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class BagController {

    private final ProductService prodServ;
    private final RentalImageService rentalImgServ;
    private final BagService bagServ;
    private final AddressService addServ;
    private final UserService userServ;
    private final ReservationService reservServ;
    private final Long rentalCategoryNumber = 1L;

    @GetMapping("/create/bag")
    public void create(Model model) {
        log.info("bag getCreate()");
        model.addAttribute("brands", prodServ.readBrands(rentalCategoryNumber));
        model.addAttribute("colors", prodServ.readColors(rentalCategoryNumber));
    }

    @PostMapping(path = "/create/bag", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String create(@ModelAttribute BagCreateDto bagDto, 
    	@ModelAttribute RentalImageCreateDto imgDto,
        @ModelAttribute AddressCreateDto addDto) throws Exception {
        
        log.info("bag postCreate()");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();

            if (principal instanceof User) { // principal이 User 객체인지 확인
                User user = (User) principal; // 캐스팅
                String username = user.getUsername(); // User 객체의 id 반환

                log.info("bagDto={}", bagDto.getProductName());
                log.info("imgDto={}", imgDto);
                log.info("addDto={}", addDto);

                if(bagDto.getBrandId() == 0) {
                    Brand brand = prodServ.createBrand(rentalCategoryNumber, bagDto.getCustomBrand());
                    bagDto.setBrandId(brand.getId());
                }
                Product product = bagServ.create(bagDto, username, rentalCategoryNumber);
                
                if(imgDto.getImages() != null && !imgDto.getImages().isEmpty()) {
                    rentalImgServ.create(product.getId(), imgDto);
                } else {
                    log.info("이미지 첨부 X");
                }
        
                addServ.create(product, addDto);
                return "redirect:/post/details/bag?id=" + product.getId();
            } 
        }
        return "redirect:/";
    }

    @GetMapping("/details/bag")
    public void detail(@RequestParam(name="id") Long productId, Model model) {

        log.info("bag detail(productId={})", productId);
        Product product = prodServ.readById(productId);
        log.info("bag detail(product={})", product);
        User user = userServ.read(product.getUser().getId());
        log.info("bag detail(user={})", user);
        Address address = addServ.readByProductId(productId);
        log.info("bag detail(address={})", address);
        List<RentalImage> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("bag detail(rentalImages={})", rentalImages);

        model.addAttribute("product", product);
        model.addAttribute("user", user);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
    }
    
    @DeleteMapping("/delete/bag")
    public ResponseEntity<String> delete(@RequestParam(name="id") Long productId) {
    	
    	List<Reservation> reservations = reservServ.readAll(productId);
    	log.info("reservations={}", reservations);
    	if(!reservations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("해당 제품에 있는 예약을 처리 후 삭제가 가능합니다.");
    	} else {
            log.info("bag delete(productId={})", productId);
            rentalImgServ.deleteByProductId(productId);
            prodServ.deleteById(productId);
        	return ResponseEntity.ok("삭제 완료");
    	}
    }

    @GetMapping("/modify/bag")
    public void modify(@RequestParam(name="id") Long productId, Model model) {
        log.info("bag modify(productId={})", productId);
        Product product = prodServ.readById(productId);
        log.info("bag modify(bag={})", product);
        Address address = addServ.readByProductId(productId);
        log.info("bag modify(address={})", address);
        List<RentalImage> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("bag rentalImages(productId={})", rentalImages);

        model.addAttribute("product", product);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
    }

    @PostMapping(path = "/update/bag", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String update(@RequestParam(name="id") Long productId, @RequestParam(name="existingImageIds", required = false) List<Long> existingImageIds, 
        @ModelAttribute BagUpdateDto bagDto, @ModelAttribute RentalImageCreateDto imgDto, @ModelAttribute AddressUpdateDto addDto) throws IOException {
        log.info("bag update(productId={})", productId);
        log.info("bag update(existingImageIds={})", existingImageIds);
        log.info("bag update(productDto={})", bagDto);
        log.info("bag update(RentalImageCreateDto={})", imgDto);
        log.info("bag update(AddressUpdateDto={})", addDto);

        List<Long> safeExistingImageIds = existingImageIds != null ? existingImageIds : new ArrayList<>();
        
        List<RentalImage> imagesBeforeUpdate = rentalImgServ.readByProductId(productId);
        List<Long> imageIdsBeforeUpdate = imagesBeforeUpdate.stream()
        	    .map(RentalImage :: getId)
        	    .collect(Collectors.toList());
        
        List<Long> imageIdsForDelete = imageIdsBeforeUpdate.stream()
                .filter(imageId -> !safeExistingImageIds.contains(imageId))
                .collect(Collectors.toList());
        
        Set<Long> imageIdsBeforeUpdateSet = new HashSet<>(imageIdsBeforeUpdate);
        Set<Long> imageIdsForDeleteSet = new HashSet<>(imageIdsForDelete);
        
        Product updatedProduct = bagServ.update(productId, bagDto);
        Address updatedAddress = addServ.update(productId, addDto);

        if(!imageIdsBeforeUpdateSet.equals(imageIdsForDeleteSet) || 
        		(imageIdsBeforeUpdateSet.equals(imageIdsForDeleteSet) && !imageIdsForDeleteSet.isEmpty())) {
        	rentalImgServ.deleteBeforeUpdate(imageIdsForDelete);
        }
        if(!imgDto.getImages().isEmpty()) {
        	rentalImgServ.create(productId, imgDto);
        }

        return "redirect:/post/details/bag?id=" + productId;
    }
}
