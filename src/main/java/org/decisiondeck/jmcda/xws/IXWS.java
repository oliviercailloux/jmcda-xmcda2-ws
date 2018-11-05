package org.decisiondeck.jmcda.xws;

import org.decisiondeck.jmcda.exc.InvalidInputException;

/**
 * An XMCDA Web Service. Such an object can be executed using the {@link XWSExecutor} class and be annotated with e.g.
 * {@link XWSInput}. These annotations are processed by the {@link XWSExecutor} before executing the service.
 * 
 * @author Olivier Cailloux
 * 
 */
public interface IXWS {
    public void execute() throws InvalidInputException;
}
