package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yourpck.javax.annotation.meta.TypeQualifier;
import yourpck.javax.annotation.meta.TypeQualifierValidator;
import yourpck.javax.annotation.meta.When;

/**
 * The annotated element must not be null.
 * <p>
 * Annotated fields must not be null after construction has completed.
 * <p>
 * When this javax.annotation is applied to a method it applies to the method return value.
 */
@Documented
@TypeQualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface Nonnull {
    When when() default When.ALWAYS;

    class Checker implements TypeQualifierValidator<Nonnull> {

        public When forConstantValue(Nonnull qualifierArgument, Object value) {
            if (value == null)
                return When.NEVER;
            return When.ALWAYS;
        }
    }
}
