package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatParty is a Querydsl query type for ChatParty
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatParty extends EntityPathBase<ChatParty> {

    private static final long serialVersionUID = -2105096280L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatParty chatParty = new QChatParty("chatParty");

    public final QChatRoom chatRoom;

    public final QChatPartyId id;

    public final QUser participant;

    public QChatParty(String variable) {
        this(ChatParty.class, forVariable(variable), INITS);
    }

    public QChatParty(Path<? extends ChatParty> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatParty(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatParty(PathMetadata metadata, PathInits inits) {
        this(ChatParty.class, metadata, inits);
    }

    public QChatParty(Class<? extends ChatParty> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatRoom = inits.isInitialized("chatRoom") ? new QChatRoom(forProperty("chatRoom")) : null;
        this.id = inits.isInitialized("id") ? new QChatPartyId(forProperty("id")) : null;
        this.participant = inits.isInitialized("participant") ? new QUser(forProperty("participant"), inits.get("participant")) : null;
    }

}

