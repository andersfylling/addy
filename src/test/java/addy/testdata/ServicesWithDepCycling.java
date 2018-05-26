package addy.testdata;

import addy.annotations.*;

@Configuration
public class ServicesWithDepCycling {
    public final static int NUM_OF_GAME_COMPONENTS = 2;
    // this should throw an instantiation error
    @Service
    public Object throwErrorPleaseB(@Inject("throwErrorPleaseC") Object c) {
        return null;
    }
    @Service
    public Object throwErrorPleaseC(@Inject("throwErrorPleaseB") Object b) {
        return null;
    }
}
