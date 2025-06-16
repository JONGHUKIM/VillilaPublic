package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QChatRoomReservation is a Querydsl query type for ChatRoomReservation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QChatRoomReservation extends EntityPathBase<ChatRoomReservation> {

    private static final long serialVersionUID = -1029110221L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QChatRoomReservation chatRoomReservation = new QChatRoomReservation("chatRoomReservation");

    public final QChatRoom chatRoom;

    public final NumberPath<Long> chatRoomId = createNumber("chatRoomId", Long.class);

    public final QReservation reservation;

    public final NumberPath<Long> reservationId = createNumber("reservationId", Long.class);

    public QChatRoomReservation(String variable) {
        this(ChatRoomReservation.class, forVariable(variable), INITS);
    }

    public QChatRoomReservation(Path<? extends ChatRoomReservation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QChatRoomReservation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QChatRoomReservation(PathMetadata metadata, PathInits inits) {
        this(ChatRoomReservation.class, metadata, inits);
    }

    public QChatRoomReservation(Class<? extends ChatRoomReservation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.chatRoom = inits.isInitialized("chatRoom") ? new QChatRoom(forProperty("chatRoom")) : null;
        this.reservation = inits.isInitialized("reservation") ? new QReservation(forProperty("reservation"), inits.get("reservation")) : null;
    }

}

