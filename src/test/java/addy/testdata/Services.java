package addy.testdata;


import addy.annotations.*;

import static org.junit.Assert.assertEquals;

@Configuration
public class Services {
    public final static int NUM_OF_GAME_COMPONENTS = 5;

    private boolean randomGameLogicExecuted = false;
    private int gameLogicWithParamExecuted1 = 0;
    private String gameLogicWithParamExecuted2 = "";

    public void testingReflection() {}

    @Service
    public int getASixer() {
        return 6;
    }
    @Service
    public String getAnders() {
        return "anders";
    }
    @Service
    public Runnable randomGameLogic(){
        return () -> randomGameLogicExecuted = true;
    }
    @Service
    public Runnable gameLogicWithParams(@Inject("getASixer") final int v, @Inject("getAnders") final String n){
        return () -> {
            gameLogicWithParamExecuted1 = v;
            gameLogicWithParamExecuted2 = n;
        };
    }
    @Service
    public Object testPlease(@Inject("gameLogicWithParams") Runnable t) {
        assertEquals(this.gameLogicWithParamExecuted2, "");
        t.run();
        assertEquals(this.gameLogicWithParamExecuted2, this.getAnders());

        return "test";
    }
}
