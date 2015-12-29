package org.perfcake.util.properties;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a PerfCake property as mandatory.
 *
 * @author <a href="mailto:pavel.macik@gmail.com">Pavel Macík</a>
 */
@Target(ElementType.FIELD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface MandatoryProperty {
}
