package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTheme is a Querydsl query type for Theme
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTheme extends EntityPathBase<Theme> {

    private static final long serialVersionUID = 1734802435L;

    public static final QTheme theme1 = new QTheme("theme1");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath theme = createString("theme");

    public QTheme(String variable) {
        super(Theme.class, forVariable(variable));
    }

    public QTheme(Path<? extends Theme> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTheme(PathMetadata metadata) {
        super(Theme.class, metadata);
    }

}

