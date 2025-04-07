package com.splusz.villigo.repository;

import java.util.List;
import java.util.Map;

import com.splusz.villigo.domain.Product;

public interface ProductRepositoryCustom {

    List<Product> searchedProduct(Map<String, List<String>> filters);
    
    List<Product> recentProducts();
    
    List<Product> themeProducts(Long rentalCategoryId);
}
