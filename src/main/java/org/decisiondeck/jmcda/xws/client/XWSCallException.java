package org.decisiondeck.jmcda.xws.client;


/**
 * Indicates an exception related to an XMCDA Web Service invocation.
 * 
 * @author Olivier Cailloux
 * 
 */
public class XWSCallException extends Exception {

    private static final long serialVersionUID = 1L;

    public XWSCallException() {
	super();
    }

    public XWSCallException(String message, Throwable cause) {
	super(message, cause);
    }

    public XWSCallException(String message) {
	super(message);
    }

    public XWSCallException(Throwable cause) {
	super(cause);
    }

}