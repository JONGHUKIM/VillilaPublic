package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRentalImage is a Querydsl query type for RentalImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRentalImage extends EntityPathBase<RentalImage> {

    private static final long serialVersionUID = -205437615L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRentalImage rentalImage = new QRentalImage("rentalImage");

    public final QBaseTimeEntity _super = new QBaseTimeEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdTime = _super.createdTime;

    public final StringPath filePath = createString("filePath");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedTime = _super.modifiedTime;

    public final QProduct product;

    public QRentalImage(String variable) {
        this(RentalImage.class, forVariable(variable), INITS);
    }

    public QRentalImage(Path<? extends RentalImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRentalImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRentalImage(PathMetadata metadata, PathInits inits) {
        this(RentalImage.class, metadata, inits);
    }

    public QRentalImage(Class<? extends RentalImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product"), inits.get("product")) : null;
    }

}

