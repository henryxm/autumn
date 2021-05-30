package cn.org.autumn.table.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface Index {

    String name() default "";

    IndexField[] fields() default {};

    IndexTypeEnum indexType() default IndexTypeEnum.NORMAL;

    IndexMethodEnum indexMethod() default IndexMethodEnum.BTREE;

    String comment() default "";
}
