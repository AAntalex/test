package com.antalex.annotation;

import com.antalex.model.enums.ShardType;

import java.lang.annotation.*;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ShardEntity {
    ShardType type() default ShardType.SHARDABLE;
}
