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
import com.splusz.villigo.domain.Car;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.RentalImage;
import com.splusz.villigo.domain.Reservation;
import com.splusz.villigo.domain.User;
import com.splusz.villigo.dto.AddressCreateDto;
import com.splusz.villigo.dto.AddressUpdateDto;
import com.splusz.villigo.dto.CarCreateDto;
import com.splusz.villigo.dto.CarUpdateDto;
import com.splusz.villigo.dto.RentalImageCreateDto;
import com.splusz.villigo.dto.RentalImageDto;
import com.splusz.villigo.dto.UserProfileDto;
import com.splusz.villigo.service.AddressService;
import com.splusz.villigo.service.CarService;
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
public class CarController {

    private final ProductService prodServ;
    private final RentalImageService rentalImgServ;
    private final CarService carServ;
    private final AddressService addServ;
    private final UserService userServ;
    private final ReservationService reservServ;
    private final Long rentalCategoryNumber = 2L;

    @GetMapping("/create/car")
    public void create(Model model) {
        log.info("car create()");
        model.addAttribute("brands", prodServ.readBrands(rentalCategoryNumber));
        model.addAttribute("colors", prodServ.readColors(rentalCategoryNumber));
    }

    @PostMapping(path = "/create/car", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(@ModelAttribute CarCreateDto carDto,
                                   @RequestParam(name = "imageS3Keys", required = false) List<String> imageS3Keys,
                                   @ModelAttribute AddressCreateDto addDto) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = SecurityUserUtil.getUserFromPrincipal(authentication.getPrincipal());

        if (user == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        if (user.getId() == null || user.getUsername() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("사용자 정보가 누락되었습니다.");
        }

        String username = user.getUsername();
        log.info("carDto={}", carDto);
        log.info("imageS3Keys={}", imageS3Keys);
        log.info("addDto={}", addDto);

        if (carDto.getBrandId() == 0) {
            Brand brand = prodServ.createBrand(rentalCategoryNumber, carDto.getCustomBrand());
            carDto.setBrandId(brand.getId());
        }

        Product product = carServ.create(carDto, user, rentalCategoryNumber);

        if (imageS3Keys != null && !imageS3Keys.isEmpty()) {
            rentalImgServ.create(product.getId(), imageS3Keys);
        } else {
            log.info("이미지 첨부 X");
        }

        addServ.create(product, addDto);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/post/details/car")
                .queryParam("id", product.getId())
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }

    @GetMapping("/details/car")
    public String detail(@RequestParam(name = "id") Long productId, Model model) {
        log.info("detail car(productId={})", productId);
        
        Product product = prodServ.readById(productId);
        if (product == null) {
            log.warn("상품을 찾을 수 없습니다: productId={}", productId);
            return "error/404";
        }
        log.info("product={}", product);
        
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

        Car car = carServ.readByProductId(productId);
        log.info("car={}", car);
        
        Address address = addServ.readByProductId(productId);
        log.info("address={}", address);
        List<RentalImageDto> rentalImages = rentalImgServ.readByProductId(productId);
        log.info("rentalImages={}", rentalImages);

        model.addAttribute("product", product);
        model.addAttribute("user", productOwnerProfile);
        model.addAttribute("car", car);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
        
        log.info("model user (UserProfileDto)={}", productOwnerProfile);
        
        return "post/details/car";
    }

    @DeleteMapping("/delete/car")
    public ResponseEntity<String> delete(@RequestParam(name = "id") Long productId) {
        List<Integer> excludedStatuses = Arrays.asList(4, 5, 7);
        List<Reservation> activeReservations = reservServ.readAllExceptStatuses(productId, excludedStatuses);
        log.info("activeReservations={}", activeReservations);
        if (!activeReservations.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("해당 제품에 있는 예약을 처리 후 삭제가 가능합니다.");
        } else {
            log.info("car delete(productId={})", productId);
            rentalImgServ.deleteByProductId(productId);
            prodServ.deleteProduct(productId);
            return ResponseEntity.ok("삭제 완료");
        }
    }

    @GetMapping("/modify/car")
    public void modify(@RequestParam(name = "id") Long productId, Model model) {
        log.info("car modify(productId={})", productId);
        Product product = prodServ.readById(productId);
        Car car = carServ.readByProductId(productId);
        Address address = addServ.readByProductId(productId);
        List<RentalImageDto> rentalImages = rentalImgServ.readByProductId(productId);

        model.addAttribute("product", product);
        model.addAttribute("car", car);
        model.addAttribute("address", address);
        model.addAttribute("rentalImages", rentalImages);
    }

    @PostMapping(path = "/update/car", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> update(@RequestParam(name = "id") Long productId,
                                   @RequestParam(name = "imageS3Keys", required = false) List<String> imageS3Keys,
                                   @RequestParam(name = "deletedImageIds", required = false) List<Long> deletedImageIds,
                                   @ModelAttribute CarUpdateDto carDto,
                                   @ModelAttribute AddressUpdateDto addDto) {
        log.info("car update(productId={})", productId);
        log.info("car update(imageS3Keys={})", imageS3Keys);
        log.info("car update(deletedImageIds={})", deletedImageIds);
        log.info("car update(carUpdateDto={})", carDto);
        log.info("car update(AddressUpdateDto={})", addDto);

        List<Long> safeDeletedImageIds = deletedImageIds != null ? deletedImageIds : new ArrayList<>();
        List<String> safeImageS3Keys = imageS3Keys != null ? imageS3Keys : new ArrayList<>();

        Product updatedProduct = carServ.update(productId, carDto);
        Address updatedAddress = addServ.update(productId, addDto);

        if (!safeDeletedImageIds.isEmpty()) {
            rentalImgServ.deleteBeforeUpdate(safeDeletedImageIds);
        }

        if (!safeImageS3Keys.isEmpty()) {
            rentalImgServ.create(productId, safeImageS3Keys);
        }

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/post/details/car")
                .queryParam("id", productId)
                .build()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(headers, HttpStatus.SEE_OTHER);
    }
}
