package addy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Client represents a class that does not need to be a service, but still depends on services.
 * If the class can be used by other classes, use the @Service annotation instead.
 *
 * Any class marked with @Client, unless marked as @Service as well, will not be added
 * to the service context. Meaning it cannot be injected into other instances by the injector.
 *
 * https://en.wikipedia.org/wiki/Dependency_injection
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Client {
    String name() default "";
    String value() default "";
}
