package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatReadReceipt is a Querydsl query type for ChatReadReceipt
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatReadReceipt extends EntityPathBase<ChatReadReceipt> {

    private static final long serialVersionUID = -60395772L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatReadReceipt chatReadReceipt = new QChatReadReceipt("chatReadReceipt");

    public final QChatMessage chatMessage;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> readTime = createDateTime("readTime", java.time.LocalDateTime.class);

    public final QUser user;

    public QChatReadReceipt(String variable) {
        this(ChatReadReceipt.class, forVariable(variable), INITS);
    }

    public QChatReadReceipt(Path<? extends ChatReadReceipt> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatReadReceipt(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatReadReceipt(PathMetadata metadata, PathInits inits) {
        this(ChatReadReceipt.class, metadata, inits);
    }

    public QChatReadReceipt(Class<? extends ChatReadReceipt> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatMessage = inits.isInitialized("chatMessage") ? new QChatMessage(forProperty("chatMessage"), inits.get("chatMessage")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

