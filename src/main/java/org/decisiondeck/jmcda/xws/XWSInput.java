package org.decisiondeck.jmcda.xws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;

/**
 * Describes a field as being an input source of an XMCDA service.
 * 
 * @author Olivier Cailloux
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XWSInput {
    static class None implements FunctionWithInputCheck<Object, Object> {
	private None() {
	    /** Non instantiable. */
	}

	@Override
	public Object apply(Object input) throws InvalidInputException {
	    throw new UnsupportedOperationException();
	}
}

    /**
     * <P>
     * Indicates the name of the XML file which is to be read by the XMCDA service. Typically, should end with ".xml".
     * </P>
     * <P>
     * Defaults to the name of the field with the suffix .xml added.
     * </P>
     */
    String name() default "";

    /**
     * <P>
     * Indicates if this input parameter is optional or if it is mandatory. Note that if it is optional, your program
     * should be able to handle a request where that file is not present in the input directory.
     * </P>
     * <P>
     * This defaults to <code>false</code> (meaning that the parameter is required).
     * </P>
     * <p>
     * If <code>true</code>, the injector will check whether the file exists where it is supposed to, if it does not,
     * the value <code>null</code> will be injected instead of a source object.
     * </p>
     */
    boolean optional() default false;

    /**
     * The class to use to transform the name string into the value to be injected. If not set, a default transformer
     * dependent on the field type will be used.
     */
    Class<? extends FunctionWithInputCheck<?, ?>> transformer() default None.class;
}
