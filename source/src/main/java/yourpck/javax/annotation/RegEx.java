package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import yourpck.javax.annotation.meta.TypeQualifierNickname;
import yourpck.javax.annotation.meta.TypeQualifierValidator;
import yourpck.javax.annotation.meta.When;

/**
 * This qualifier is used to denote String values that should be a Regular
 * expression.
 * <p>
 * When this javax.annotation is applied to a method it applies to the method return value.
 */
@Documented
@Syntax("RegEx")
@TypeQualifierNickname
@Retention(RetentionPolicy.RUNTIME)
public @interface RegEx {
    When when() default When.ALWAYS;

    static class Checker implements TypeQualifierValidator<RegEx> {

        public When forConstantValue(RegEx annotation, Object value) {
            if (!(value instanceof String))
                return When.NEVER;

            try {
                Pattern.compile((String) value);
            } catch (PatternSyntaxException e) {
                return When.NEVER;
            }
            return When.ALWAYS;

        }

    }

}