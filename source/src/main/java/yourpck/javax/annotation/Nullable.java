package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yourpck.javax.annotation.meta.TypeQualifierNickname;
import yourpck.javax.annotation.meta.When;

/**
 * The annotated element could be null under some circumstances.
 * <p>
 * In general, this means developers will have to read the documentation to
 * determine when a null value is acceptable and whether it is necessary to
 * check for a null value.
 * <p>
 * This javax.annotation is useful mostly for overriding a {@link Nonnull} javax.annotation.
 * Static analysis tools should generally treat the annotated items as though they
 * had no javax.annotation, unless they are configured to minimize false negatives.
 * Use {@link CheckForNull} to indicate that the element value should always be checked
 * for a null value.
 * <p>
 * When this javax.annotation is applied to a method it applies to the method return value.
 */
@Documented
@TypeQualifierNickname
@Nonnull(when = When.UNKNOWN)
@Retention(RetentionPolicy.RUNTIME)
public @interface Nullable {

}
