package cn.com.za.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Route {

    /**
     * 路由路径，标识一个路由节点
     *
     * @return
     */
    String path();

    /**
     * 将路由进行分组，可以按分组动态假造
     *
     * @return
     */
    String group() default "";
}
