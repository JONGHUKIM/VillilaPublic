package com.splusz.villigo.web;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.splusz.villigo.domain.Address;
import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.AddressCreateDto;
import com.splusz.villigo.dto.AddressUpdateDto;
import com.splusz.villigo.dto.BagCreateDto;
import com.splusz.villigo.dto.BagUpdateDto;
import com.splusz.villigo.dto.RentalImageCreateDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.service.AddressService;
import com.splusz.villigo.service.BagService;
import com.splusz.villigo.service.ProductService;
import com.splusz.villigo.service.RentalImageService;
import com.splusz.villigo.service.ReservationService;
import com.splusz.villigo.service.UserService;
import com.splusz.villigo.storage.FileStorageException;
import com.splusz.villigo.util.SecurityUserUtil;

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
    public ResponseEntity<?> create(@ModelAttribute BagCreateDto bagDto,
                         @ModelAttribute RentalImageCreateDto imgDto,
                         @ModelAttribute AddressCreateDto addDto) throws Exception {

        log.info("bag postCreate()");
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        if (user == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        if (user.getId() == null || user.getUsername() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("사용자 정보가 누락되었습니다.");
        }

        String username = user.getUsername();

        log.info("bagDto={}", bagDto.getProductName());
        log.info("imgDto={}", imgDto);
        log.info("addDto={}", addDto);

        if (bagDto.getBrandId() == 0) {
            Brand brand = prodServ.createBrand(rentalCategoryNumber, bagDto.getCustomBrand());
            bagDto.setBrandId(brand.getId());
        }

        Product product = bagServ.create(bagDto, user, rentalCategoryNumber);

        if (imgDto.getImages() != null && !imgDto.getImages().isEmpty()) {
            rentalImgServ.create(product.getId(), imgDto);
        } else {
            log.info("이미지 첨부 X");
        }

        addServ.create(product, addDto);
        
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/post/details/bag")
                .queryParam("id", product.getId())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping("/details/bag")
    public String detail(@RequestParam("id") Long productId, Model model) {

        log.info("bag detail(productId={})", productId);
        
        // Product 정보 조회 (중복 선언 제거)
        Product product = prodServ.readById(productId); // ProductService.getProductById 대신 readById 사용 (기존에 섞여 있던 부분 통일)
        if (product == null) {
            log.warn("상품을 찾을 수 없습니다: productId={}", productId);
            return "error/404"; // 상품이 없는 경우 404 페이지로 처리
        }
        log.info("bag detail(product={})", product);
        
        // 상품 소유자 User 엔티티 조회 (기존 User user = userServ.read() 제거)
        User productOwnerEntity = product.getUser(); // Product 엔티티에 User 정보가 포함되어 있음
        if (productOwnerEntity == null) {
            log.error("상품에 연결된 사용자 정보를 찾을 수 없습니다: productId={}", productId);
            return "error/500"; // 심각한 오류로 500 페이지 처리
        }

        // User 엔티티를 UserProfileDto로 변환하여 아바타 URL 포함
        UserProfileDto productOwnerProfile = null;
        try {
            productOwnerProfile = userServ.getUserProfileDto(productOwnerEntity); // <--- User 엔티티를 DTO로 변환
        } catch (FileStorageException e) {
            log.error("상품 소유자의 아바타 URL 생성 실패 (FileStorageException): {}", e.getMessage(), e);
            // 오류 발생 시 기본값으로 DTO 생성
            productOwnerProfile = UserProfileDto.builder()
                .id(productOwnerEntity.getId())
                .nickname(productOwnerEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png") // 오류 시 기본 이미지 경로
                .build();
        } catch (Exception e) {
            log.error("상품 소유자 프로필 조회 중 알 수 없는 오류: {}", e.getMessage(), e);
            // 다른 예외 처리 시에도 기본값으로 DTO 생성
            productOwnerProfile = UserProfileDto.builder()
                .id(productOwnerEntity.getId())
                .nickname(productOwnerEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png") // 오류 시 기본 이미지 경로
                .build();
        }
        
        // 나머지 정보들 조회
        Address address = addServ.readByProductId(productId);
        log.info("bag detail(address={})", address);
        List<RentalImage> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("bag detail(rentalImages={})", rentalImages);

        // 모델에 추가
        model.addAttribute("product", product);
        model.addAttribute("user", productOwnerProfile); // <--- UserProfileDto를 "user" 이름으로 모델에 추가!
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
        
        log.info("bag detail(userProfileDto)={}", productOwnerProfile); // 로깅 메시지 명확화
        
        return "post/details/bag";
    }

    @DeleteMapping("/delete/bag")
    public ResponseEntity<String> delete(@RequestParam(name = "id") Long productId) {
        List<Integer> excludedStatuses = Arrays.asList(4, 5, 7);
        List<Reservation> activeReservations = reservServ.readAllExceptStatuses(productId, excludedStatuses);
        log.info("activeReservations={}", activeReservations);

        if (!activeReservations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("해당 제품에 있는 예약을 처리 후 삭제가 가능합니다.");
        } else {
            log.info("bag delete(productId={})", productId);
            rentalImgServ.deleteByProductId(productId);
            prodServ.deleteProduct(productId);
            return ResponseEntity.ok("삭제 완료");
        }
    }

    @GetMapping("/modify/bag")
    public void modify(@RequestParam(name = "id") Long productId, Model model) {
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
    public ResponseEntity<?> update(@RequestParam(name = "id") Long productId,
                                     @RequestParam(name = "existingImageIds", required = false) List<Long> existingImageIds,
                                     @ModelAttribute BagUpdateDto bagDto,
                                     @ModelAttribute RentalImageCreateDto imgDto,
                                     @ModelAttribute AddressUpdateDto addDto) throws IOException {

        log.info("bag update(productId={})", productId); // 로그 메시지 bag으로 변경
        log.info("bag update(existingImageIds={})", existingImageIds);
        log.info("bag update(bagUpdateDto={})", bagDto);
        log.info("bag update(RentalImageCreateDto={})", imgDto);
        log.info("bag update(AddressUpdateDto={})", addDto);

        List<Long> safeExistingImageIds = existingImageIds != null ? existingImageIds : new ArrayList<>();

        List<RentalImage> imagesBeforeUpdate = rentalImgServ.readByProductId(productId);
        List<Long> imageIdsBeforeUpdate = imagesBeforeUpdate.stream()
                .map(RentalImage::getId)
                .collect(Collectors.toList());

        List<Long> imageIdsForDelete = imageIdsBeforeUpdate.stream()
                .filter(imageId -> !safeExistingImageIds.contains(imageId))
                .collect(Collectors.toList());

        Product updatedProduct = bagServ.update(productId, bagDto); // carServ.update 대신 bagServ.update
        Address updatedAddress = addServ.update(productId, addDto);

        if (!imageIdsForDelete.isEmpty()) {
            rentalImgServ.deleteBeforeUpdate(imageIdsForDelete);
        }

        if (imgDto.getImages() != null && !imgDto.getImages().isEmpty()) {
            rentalImgServ.create(productId, imgDto);
        }

        // 상세 페이지로 리다이렉트
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/post/details/bag")
                .queryParam("id", productId)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}
