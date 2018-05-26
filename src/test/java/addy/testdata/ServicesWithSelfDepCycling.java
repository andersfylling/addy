package addy.testdata;

import addy.annotations.*;

@Configuration
public class ServicesWithSelfDepCycling {
    public final static int NUM_OF_GAME_COMPONENTS = 1;
    // this should throw an instantiation error
    @Service
    public Object throwErrorPleaseA(@Inject("throwErrorPleaseA") Object a) {
        return null;
    }
}
