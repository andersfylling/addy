package addy.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to identify an array of Objects which may or may not contain classes
 * using the @GameComponent annotation. Only classes with @GameComponent annotation
 * will be instantiated (Note: that these must have a constructor with @GameDepWire,
 *                             to correctly wire dependencies).
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceLinker {
    Class<?>[] value();
}
