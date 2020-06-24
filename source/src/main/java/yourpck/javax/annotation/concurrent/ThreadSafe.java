/*
 * Copyright (c) 2005 Brian Goetz
 * Released under the Creative Commons Attribution License
 *   (http://creativecommons.org/licenses/by/2.5)
 * Official home: http://www.jcip.net
 */
package yourpck.javax.annotation.concurrent;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The class to which this javax.annotation is applied is thread-safe. This means that
 * no sequences of accesses (reads and writes to public fields, calls to public
 * methods) may put the object into an invalid state, regardless of the
 * interleaving of those actions by the runtime, and without requiring any
 * additional synchronization or coordination on the part of the caller.
 *
 * @see NotThreadSafe
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ThreadSafe {
}
