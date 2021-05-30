package cn.org.autumn.table.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Deprecated
public @interface UniqueKey {

    String name();

    UniqueKeyFields[] fields() default {};

    IndexTypeEnum indexType() default IndexTypeEnum.UNIQUE;

    IndexMethodEnum indexMethod() default IndexMethodEnum.BTREE;

    String comment() default "";
}
