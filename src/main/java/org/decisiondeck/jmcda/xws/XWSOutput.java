package org.decisiondeck.jmcda.xws;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.xmlbeans.XmlObject;

import com.google.common.base.Function;

/**
 * Describes a field as being a destination field for an output of an XMCDA service.
 * 
 * @author Olivier Cailloux
 * 
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface XWSOutput {
    static class None implements Function<Object, XmlObject> {
	private None() {
	    /** Non instantiable. */
	}

	@Override
	public XmlObject apply(Object input) {
	    throw new UnsupportedOperationException();
	}
    }

    /**
     * <P>
     * Indicates the name of the XML file which is to be written by the XMCDA service, including extension.
     * </P>
     * <P>
     * Defaults to the name of the field with a ".xml" suffix.
     * </P>
     */
    String name() default "";

    /**
     * The class to use to transform the the field value into an {@link XmlObject}. If not set, a default transformer
     * dependent on the field type will be used if available.
     */
    Class<? extends Function<?, ? extends XmlObject>> transformer() default None.class;
}
