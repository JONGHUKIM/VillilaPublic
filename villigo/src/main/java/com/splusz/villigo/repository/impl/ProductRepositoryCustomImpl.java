package com.splusz.villigo.repository.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.splusz.villigo.domain.Product;
import com.splusz.villigo.domain.QProduct;
import com.splusz.villigo.repository.ProductRepositoryCustom;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Repository
public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Product> searchedProduct(Map<String, List<String>> f) {
        QProduct product = QProduct.product;
        BooleanBuilder builder = new BooleanBuilder();

        System.out.println(f.toString());
        if(f.containsKey("price")) {
            BooleanBuilder priceBuilder = new BooleanBuilder();
            String[] parts = f.get("price").get(0).split(",");
            Integer minPrice = Integer.parseInt(parts[0]);
            Integer maxPrice = Integer.parseInt(parts[1]);

            if(minPrice != null && !minPrice.equals("")) {
                priceBuilder.and(product.fee.goe(minPrice));
            }

            if(maxPrice != null && !maxPrice.equals("")) {
                priceBuilder.and(product.fee.loe(maxPrice));
            }
            
            if(minPrice != null && !minPrice.equals("") && maxPrice != null && !maxPrice.equals("")) {
                priceBuilder.and(product.fee.between(minPrice, maxPrice));
            }
            builder.and(priceBuilder);
        }

        if(!f.get("keyword").get(0).isEmpty()) {
            String keyword = f.get("keyword").get(0);
            BooleanBuilder keywordBuilder = new BooleanBuilder();
            keywordBuilder.or(product.brand.name.containsIgnoreCase(keyword))
                .or(product.postName.containsIgnoreCase(keyword))
                .or(product.productName.containsIgnoreCase(keyword))
                .or(product.user.nickname.containsIgnoreCase(keyword));
            builder.and(keywordBuilder);
        }

        if(f.containsKey("rentalCategory")) {
            BooleanBuilder rentCateBuilder = new BooleanBuilder();
            for(String rentalCategoryId : f.get("rentalCategory")) {
                rentCateBuilder.or(product.rentalCategory.id.eq(Long.parseLong(rentalCategoryId)));
            }
            builder.and(rentCateBuilder);
        }

        if(f.containsKey("brand")) {
            BooleanBuilder brandBuilder = new BooleanBuilder();
            for(String brandId : f.get("brand")) {
                brandBuilder.or(product.brand.id.eq(Long.parseLong(brandId)));
            }
            builder.and(brandBuilder);
        }

        if(f.containsKey("color")) {
            BooleanBuilder colorBuilder = new BooleanBuilder();
            for(String colorNumber : f.get("color")) {
                colorBuilder.or(product.color.colorNumber.eq(colorNumber));
            }
            builder.and(colorBuilder);
        }
        
        List<Product> result = queryFactory
            .selectFrom(product)
            .where(builder)
            .orderBy(product.createdTime.desc())
            .fetch();

        return result;
    }
    
    @Override
    public List<Product> recentProducts() {
    	QProduct product = QProduct.product;
    	
    	List<Product> result = queryFactory
    		.selectFrom(product)
    		.orderBy(product.createdTime.desc())
    		.limit(20)
    		.fetch();
    	return result;
    }
    
    @Override
    public List<Product> themeProducts(Long rentalCategoryId) {
    	QProduct product = QProduct.product;
    	List<Product> result = queryFactory
        		.selectFrom(product)
        		.where(product.rentalCategory.id.eq(rentalCategoryId))
        		.orderBy(Expressions.numberTemplate(Double.class, "RAND()").asc())
        		.limit(20)
        		.fetch();
    	return result;
    }
    
    
}
