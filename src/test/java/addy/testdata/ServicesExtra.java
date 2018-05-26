package addy.testdata;

import addy.annotations.*;

import static org.junit.Assert.assertEquals;

@Configuration
public class ServicesExtra {
    public final static int NUM_OF_GAME_COMPONENTS = 2;

    private String gameLogicWithParamExecuted2 = "";

    public void testingReflection() {}

    @Service
    public int getASeven() {
        return 7;
    }
    @Service
    public Object ensurePrivacyBetweenConfigs(@Inject("gameLogicWithParams") Runnable t) {
        assertEquals(this.gameLogicWithParamExecuted2, "");
        t.run();
        assertEquals(this.gameLogicWithParamExecuted2, "");

        return "test";
    }
}
