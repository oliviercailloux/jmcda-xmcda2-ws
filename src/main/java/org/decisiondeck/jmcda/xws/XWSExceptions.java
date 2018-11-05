package org.decisiondeck.jmcda.xws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the field holding, in a XMCDA Web Service, the exceptions that may arise while parsing the sources.
 * 
 * @author Olivier Cailloux
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XWSExceptions {
    /** Only a marker. */
}
