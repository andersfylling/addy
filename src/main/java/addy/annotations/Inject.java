package addy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Since java 7 and earlier don't support param name reflection
 * we supply a annotation with the dependency name (RegisterGameComponent name)
 * for each dep injection param.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface Inject {
    String value() default "";
}
