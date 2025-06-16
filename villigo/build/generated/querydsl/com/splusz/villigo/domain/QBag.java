package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBag is a Querydsl query type for Bag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBag extends EntityPathBase<Bag> {

    private static final long serialVersionUID = -1231730494L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBag bag = new QBag("bag");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProduct product;

    public QBag(String variable) {
        this(Bag.class, forVariable(variable), INITS);
    }

    public QBag(Path<? extends Bag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBag(PathMetadata metadata, PathInits inits) {
        this(Bag.class, metadata, inits);
    }

    public QBag(Class<? extends Bag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.product = inits.isInitialized("product") ? new QProduct(forProperty("product"), inits.get("product")) : null;
    }

}

