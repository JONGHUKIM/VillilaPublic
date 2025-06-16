package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 471643729L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final QBaseTimeEntity _super = new QBaseTimeEntity(this);

    public final StringPath avatar = createString("avatar");

    public final ListPath<ChatRoomParticipant, QChatRoomParticipant> chatRoomParticipants = this.<ChatRoomParticipant, QChatRoomParticipant>createList("chatRoomParticipants", ChatRoomParticipant.class, QChatRoomParticipant.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdTime = _super.createdTime;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isOnline = createBoolean("isOnline");

    public final NumberPath<Integer> mannerScore = createNumber("mannerScore", Integer.class);

    public final BooleanPath marketingConsent = createBoolean("marketingConsent");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> modifiedTime = _super.modifiedTime;

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final StringPath realname = createString("realname");

    public final StringPath region = createString("region");

    public final SetPath<UserRole, EnumPath<UserRole>> roles = this.<UserRole, EnumPath<UserRole>>createSet("roles", UserRole.class, EnumPath.class, PathInits.DIRECT2);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public final BooleanPath snsLogin = createBoolean("snsLogin");

    public final QTheme theme;

    public final StringPath username = createString("username");

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.theme = inits.isInitialized("theme") ? new QTheme(forProperty("theme")) : null;
    }

}

