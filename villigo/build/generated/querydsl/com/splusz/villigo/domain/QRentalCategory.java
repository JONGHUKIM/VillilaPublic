package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRentalCategory is a Querydsl query type for RentalCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRentalCategory extends EntityPathBase<RentalCategory> {

    private static final long serialVersionUID = 1046615368L;

    public static final QRentalCategory rentalCategory = new QRentalCategory("rentalCategory");

    public final StringPath category = createString("category");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QRentalCategory(String variable) {
        super(RentalCategory.class, forVariable(variable));
    }

    public QRentalCategory(Path<? extends RentalCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRentalCategory(PathMetadata metadata) {
        super(RentalCategory.class, metadata);
    }

}

