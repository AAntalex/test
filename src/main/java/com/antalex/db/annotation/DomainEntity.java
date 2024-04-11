package com.antalex.db.annotation;

import com.antalex.db.entity.abstraction.ShardInstance;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DomainEntity {
    Class<? extends ShardInstance> value();
    Storage storage() default @Storage;
    Storage[] storages() default {};
}
