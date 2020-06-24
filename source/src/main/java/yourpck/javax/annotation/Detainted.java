package yourpck.javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import yourpck.javax.annotation.meta.TypeQualifierNickname;
import yourpck.javax.annotation.meta.When;

@Documented
@TypeQualifierNickname
@Untainted(when = When.ALWAYS)
@Retention(RetentionPolicy.RUNTIME)
public @interface Detainted {

}
