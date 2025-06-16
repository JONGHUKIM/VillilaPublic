package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QChatMessageReadById is a Querydsl query type for ChatMessageReadById
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QChatMessageReadById extends BeanPath<ChatMessageReadById> {

    private static final long serialVersionUID = 335412721L;

    public static final QChatMessageReadById chatMessageReadById = new QChatMessageReadById("chatMessageReadById");

    public final NumberPath<Long> chatMessageId = createNumber("chatMessageId", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QChatMessageReadById(String variable) {
        super(ChatMessageReadById.class, forVariable(variable));
    }

    public QChatMessageReadById(Path<? extends ChatMessageReadById> path) {
        super(path.getType(), path.getMetadata());
    }

    public QChatMessageReadById(PathMetadata metadata) {
        super(ChatMessageReadById.class, metadata);
    }

}

