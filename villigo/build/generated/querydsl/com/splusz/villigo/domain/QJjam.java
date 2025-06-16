package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QJjam is a Querydsl query type for Jjam
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QJjam extends EntityPathBase<Jjam> {

    private static final long serialVersionUID = 471307250L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QJjam jjam = new QJjam("jjam");

    public final QUser buyer;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> quantity = createNumber("quantity", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> transactionTime = createDateTime("transactionTime", java.time.LocalDateTime.class);

    public final NumberPath<Integer> unitPrice = createNumber("unitPrice", Integer.class);

    public QJjam(String variable) {
        this(Jjam.class, forVariable(variable), INITS);
    }

    public QJjam(Path<? extends Jjam> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QJjam(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QJjam(PathMetadata metadata, PathInits inits) {
        this(Jjam.class, metadata, inits);
    }

    public QJjam(Class<? extends Jjam> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.buyer = inits.isInitialized("buyer") ? new QUser(forProperty("buyer"), inits.get("buyer")) : null;
    }

}

