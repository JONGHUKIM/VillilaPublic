package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatMessageReadBy is a Querydsl query type for ChatMessageReadBy
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatMessageReadBy extends EntityPathBase<ChatMessageReadBy> {

    private static final long serialVersionUID = 1698671158L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatMessageReadBy chatMessageReadBy = new QChatMessageReadBy("chatMessageReadBy");

    public final QChatMessage chatMessage;

    public final QChatMessageReadById id;

    public final BooleanPath isRead = createBoolean("isRead");

    public final DateTimePath<java.time.LocalDateTime> readTime = createDateTime("readTime", java.time.LocalDateTime.class);

    public final QUser user;

    public QChatMessageReadBy(String variable) {
        this(ChatMessageReadBy.class, forVariable(variable), INITS);
    }

    public QChatMessageReadBy(Path<? extends ChatMessageReadBy> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatMessageReadBy(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatMessageReadBy(PathMetadata metadata, PathInits inits) {
        this(ChatMessageReadBy.class, metadata, inits);
    }

    public QChatMessageReadBy(Class<? extends ChatMessageReadBy> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatMessage = inits.isInitialized("chatMessage") ? new QChatMessage(forProperty("chatMessage"), inits.get("chatMessage")) : null;
        this.id = inits.isInitialized("id") ? new QChatMessageReadById(forProperty("id")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

