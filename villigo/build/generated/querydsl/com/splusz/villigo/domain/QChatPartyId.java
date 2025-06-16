package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatPartyId is a Querydsl query type for ChatPartyId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QChatPartyId extends BeanPath<ChatPartyId> {

    private static final long serialVersionUID = -67926301L;

    public static final QChatPartyId chatPartyId = new QChatPartyId("chatPartyId");

    public final NumberPath<Long> chatRoomId = createNumber("chatRoomId", Long.class);

    public final NumberPath<Long> participantId = createNumber("participantId", Long.class);

    public QChatPartyId(String variable) {
        super(ChatPartyId.class, forVariable(variable));
    }

    public QChatPartyId(Path<? extends ChatPartyId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatPartyId(PathMetadata metadata) {
        super(ChatPartyId.class, metadata);
    }

}

