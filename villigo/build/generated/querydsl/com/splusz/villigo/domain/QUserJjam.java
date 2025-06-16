package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserJjam is a Querydsl query type for UserJjam
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserJjam extends EntityPathBase<UserJjam> {

    private static final long serialVersionUID = -1217764515L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserJjam userJjam = new QUserJjam("userJjam");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> transactionAmount = createNumber("transactionAmount", Integer.class);

    public final DatePath<java.time.LocalDate> transactionDate = createDate("transactionDate", java.time.LocalDate.class);

    public final QUser user;

    public QUserJjam(String variable) {
        this(UserJjam.class, forVariable(variable), INITS);
    }

    public QUserJjam(Path<? extends UserJjam> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserJjam(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserJjam(PathMetadata metadata, PathInits inits) {
        this(UserJjam.class, metadata, inits);
    }

    public QUserJjam(Class<? extends UserJjam> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

