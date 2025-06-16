package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QColor is a Querydsl query type for Color
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QColor extends EntityPathBase<Color> {

    private static final long serialVersionUID = 1719317917L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QColor color = new QColor("color");

    public final StringPath colorNumber = createString("colorNumber");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final QRentalCategory rentalCategory;

    public QColor(String variable) {
        this(Color.class, forVariable(variable), INITS);
    }

    public QColor(Path<? extends Color> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QColor(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QColor(PathMetadata metadata, PathInits inits) {
        this(Color.class, metadata, inits);
    }

    public QColor(Class<? extends Color> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.rentalCategory = inits.isInitialized("rentalCategory") ? new QRentalCategory(forProperty("rentalCategory")) : null;
    }

}

