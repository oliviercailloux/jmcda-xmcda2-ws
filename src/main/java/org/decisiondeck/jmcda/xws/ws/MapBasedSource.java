package org.decisiondeck.jmcda.xws.ws;

import java.util.HashMap;

import org.decisiondeck.jmcda.exc.FunctionWithInputCheck;
import org.decisiondeck.jmcda.exc.InvalidInputException;

import com.google.common.io.ByteSource;

public class MapBasedSource extends HashMap<String, ByteSource> implements
        FunctionWithInputCheck<String, ByteSource> {
    public MapBasedSource() {
        /** Public default constructor. */
    }

    @Override
    public ByteSource apply(String input) throws InvalidInputException {
        return get(input);
    }
}