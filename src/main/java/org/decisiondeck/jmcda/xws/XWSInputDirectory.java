package org.decisiondeck.jmcda.xws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the field holding, in a XMCDA Web Service, the directory where the input files are to be found.
 * 
 * @author Olivier Cailloux
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XWSInputDirectory {
    /** Only a marker. */
}
