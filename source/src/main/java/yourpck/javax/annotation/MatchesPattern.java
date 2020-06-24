package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;

import yourpck.javax.annotation.meta.TypeQualifier;
import yourpck.javax.annotation.meta.TypeQualifierValidator;
import yourpck.javax.annotation.meta.When;

/**
 * This javax.annotation is used to denote String values that should always match given pattern.
 * <p>
 * When this javax.annotation is applied to a method it applies to the method return value.
 */
@Documented
@TypeQualifier(applicableTo = String.class)
@Retention(RetentionPolicy.RUNTIME)
public @interface MatchesPattern {
    @RegEx
    String value();

    int flags() default 0;

    static class Checker implements TypeQualifierValidator<MatchesPattern> {
        public When forConstantValue(MatchesPattern annotation, Object value) {
            Pattern p = Pattern.compile(annotation.value(), annotation.flags());
            if (p.matcher(((String) value)).matches())
                return When.ALWAYS;
            return When.NEVER;
        }

    }
}
