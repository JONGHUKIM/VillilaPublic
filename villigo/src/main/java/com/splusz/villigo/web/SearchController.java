package com.splusz.villigo.web;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.splusz.villigo.domain.Brand;
import com.splusz.villigo.domain.Color;
import com.splusz.villigo.domain.RentalCategory;
import com.splusz.villigo.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Controller
public class SearchController {

    private final ProductService prodServ;

    
    @GetMapping("/search")
    public void search(Model model) {
        log.info("search()");
        List<RentalCategory> rentalCategories = prodServ.readRentalCategories();
        List<Color> colors = prodServ.readAllColors();

        model.addAttribute("rentalCategories", rentalCategories);
        model.addAttribute("colors", colors);
    }

}
