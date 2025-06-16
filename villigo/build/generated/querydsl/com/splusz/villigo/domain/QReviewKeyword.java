package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QReviewKeyword is a Querydsl query type for ReviewKeyword
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewKeyword extends EntityPathBase<ReviewKeyword> {

    private static final long serialVersionUID = 1438709675L;

    public static final QReviewKeyword reviewKeyword = new QReviewKeyword("reviewKeyword");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath keyword = createString("keyword");

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public QReviewKeyword(String variable) {
        super(ReviewKeyword.class, forVariable(variable));
    }

    public QReviewKeyword(Path<? extends ReviewKeyword> path) {
        super(path.getType(), path.getMetadata());
    }

    public QReviewKeyword(PathMetadata metadata) {
        super(ReviewKeyword.class, metadata);
    }

}

