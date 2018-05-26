package addy.testdata;

import addy.annotations.*;

public class ServicesWithoutConfigAnnotation {
    public final static int NUM_OF_GAME_COMPONENTS = 2;

    private String gameLogicWithParamExecuted2 = "";

    public void testingReflection() {}

    @Service
    public int getASevenaa() {
        return 7;
    }
    @Service
    public Object ensurePrivacyBetweenConfigsaa(@Inject("gameLogicWithParams") Runnable t) {
        assert(this.gameLogicWithParamExecuted2.equals(""));
        t.run();
        assert(this.gameLogicWithParamExecuted2.equals(""));

        return "test";
    }
}
