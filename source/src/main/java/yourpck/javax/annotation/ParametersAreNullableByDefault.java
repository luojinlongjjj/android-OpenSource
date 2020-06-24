package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yourpck.javax.annotation.meta.TypeQualifierDefault;

/**
 * This javax.annotation can be applied to a package, class or method to indicate that
 * the method parameters in that element are nullable by default unless there is:
 * <ul>
 * <li>An explicit nullness javax.annotation
 * <li>The method overrides a method in a superclass (in which case the
 * javax.annotation of the corresponding parameter in the superclass applies)
 * <li>There is a default parameter javax.annotation applied to a more tightly nested element.
 * </ul>
 * <p>This javax.annotation implies the same "nullness" as no javax.annotation. However, it is different
 * than having no javax.annotation, as it is inherited and it can override a {@link ParametersAreNonnullByDefault}
 * javax.annotation at an outer scope.
 *
 * @see Nullable
 */
@Documented
@Nullable
@TypeQualifierDefault(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface ParametersAreNullableByDefault {
}
