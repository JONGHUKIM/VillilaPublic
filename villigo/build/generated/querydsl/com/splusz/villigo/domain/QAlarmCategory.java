package com.splusz.villigo.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAlarmCategory is a Querydsl query type for AlarmCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlarmCategory extends EntityPathBase<AlarmCategory> {

    private static final long serialVersionUID = 1096967849L;

    public static final QAlarmCategory alarmCategory = new QAlarmCategory("alarmCategory");

    public final StringPath category = createString("category");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QAlarmCategory(String variable) {
        super(AlarmCategory.class, forVariable(variable));
    }

    public QAlarmCategory(Path<? extends AlarmCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAlarmCategory(PathMetadata metadata) {
        super(AlarmCategory.class, metadata);
    }

}

