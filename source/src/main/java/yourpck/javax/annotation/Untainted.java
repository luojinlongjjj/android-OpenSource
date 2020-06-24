package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yourpck.javax.annotation.meta.TypeQualifier;
import yourpck.javax.annotation.meta.When;

/**
 * This javax.annotation is used to denote String values that are untainted,
 * i.e. properly validated.
 * <p>
 * For example, this javax.annotation should be used on the String value which
 * represents SQL query to be passed to database engine.
 * <p>
 * When this javax.annotation is applied to a method it applies to the method return value.
 *
 * @see Tainted
 */
@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Untainted {
    When when() default When.ALWAYS;
}
