package addy.testdata;

import addy.annotations.*;

@Configuration
public class ServicesWithUnknownDep {
    public final static int NUM_OF_GAME_COMPONENTS = 1;

    @Service
    public String gameLogicWithParams(@Inject("thisNameShouldNotExist") final Object t){
        return "test";
    }
}
