package com.splusz.villigo.web;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.AddressCreateDto;
import com.splusz.villigo.dto.AddressUpdateDto;
import com.splusz.villigo.dto.BagCreateDto;
import com.splusz.villigo.dto.BagUpdateDto;
import com.splusz.villigo.dto.RentalImageDto;
import com.splusz.villigo.dto.ReservationDto;
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
                                   @RequestParam(name = "imageS3Keys", required = false) List<String> imageS3Keys,
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
        log.info("imageS3Keys={}", imageS3Keys);
        log.info("addDto={}", addDto);

        if (bagDto.getBrandId() == 0) {
            Brand brand = prodServ.createBrand(rentalCategoryNumber, bagDto.getCustomBrand());
            bagDto.setBrandId(brand.getId());
        }

        Product product = bagServ.create(bagDto, user, rentalCategoryNumber);

        if (imageS3Keys != null && !imageS3Keys.isEmpty()) {
            rentalImgServ.create(product.getId(), imageS3Keys);
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
        
        Product product = prodServ.readById(productId);
        if (product == null) {
            log.warn("상품을 찾을 수 없습니다: productId={}", productId);
            return "error/404";
        }
        log.info("bag detail(product={})", product);
        
        User productOwnerEntity = product.getUser();
        if (productOwnerEntity == null) {
            log.error("상품에 연결된 사용자 정보를 찾을 수 없습니다: productId={}", productId);
            return "error/500";
        }

        UserProfileDto productOwnerProfile = null;
        try {
            productOwnerProfile = userServ.getUserProfileDto(productOwnerEntity);
        } catch (FileStorageException e) {
            log.error("상품 소유자의 아바타 URL 생성 실패 (FileStorageException): {}", e.getMessage(), e);
            productOwnerProfile = UserProfileDto.builder()
                .id(productOwnerEntity.getId())
                .nickname(productOwnerEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png")
                .build();
        } catch (Exception e) {
            log.error("상품 소유자 프로필 조회 중 알 수 없는 오류: {}", e.getMessage(), e);
            productOwnerProfile = UserProfileDto.builder()
                .id(productOwnerEntity.getId())
                .nickname(productOwnerEntity.getNickname())
                .avatarImageUrl("/images/default-avatar.png")
                .build();
        }
        
        Address address = addServ.readByProductId(productId);
        log.info("bag detail(address={})", address);
        List<RentalImageDto> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("bag detail(rentalImages={})", rentalImages);

        model.addAttribute("product", product);
        model.addAttribute("user", productOwnerProfile);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
        
        log.info("bag detail(userProfileDto)={}", productOwnerProfile);
        
        return "post/details/bag";
    }

    @DeleteMapping("/delete/bag")
    public ResponseEntity<String> delete(@RequestParam(name = "id") Long productId) {
        List<Integer> excludedStatuses = Arrays.asList(4, 5, 7);
        // ReservationDto를 반환받도록 변경
        List<ReservationDto> activeReservationsDto = reservServ.readAllExceptStatuses(productId, excludedStatuses); // <--- 수정
        log.info("activeReservations={}", activeReservationsDto); // DTO 리스트 로그

        // DTO 리스트를 사용하여 비어있는지 확인
        if (!activeReservationsDto.isEmpty()) { // <--- DTO 리스트 사용
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
        List<RentalImageDto> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("bag rentalImages(productId={})", rentalImages);

        model.addAttribute("product", product);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
    }

    @PostMapping(path = "/update/bag", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@RequestParam(name = "id") Long productId,
                                   @RequestParam(name = "imageS3Keys", required = false) List<String> imageS3Keys,
                                   @RequestParam(name = "deletedImageIds", required = false) List<Long> deletedImageIds,
                                   @ModelAttribute BagUpdateDto bagDto,
                                   @ModelAttribute AddressUpdateDto addDto) {
        log.info("bag update(productId={})", productId);
        log.info("bag update(imageS3Keys={})", imageS3Keys);
        log.info("bag update(deletedImageIds={})", deletedImageIds);
        log.info("bag update(bagUpdateDto={})", bagDto);
        log.info("bag update(AddressUpdateDto={})", addDto);

        List<Long> safeDeletedImageIds = deletedImageIds != null ? deletedImageIds : new ArrayList<>();
        List<String> safeImageS3Keys = imageS3Keys != null ? imageS3Keys : new ArrayList<>();

        Product updatedProduct = bagServ.update(productId, bagDto);
        Address updatedAddress = addServ.update(productId, addDto);

        if (!safeDeletedImageIds.isEmpty()) {
            rentalImgServ.deleteBeforeUpdate(safeDeletedImageIds);
        }

        if (!safeImageS3Keys.isEmpty()) {
            rentalImgServ.create(productId, safeImageS3Keys);
        }

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
