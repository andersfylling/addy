package addy.testdata;


import addy.annotations.*;

@Configuration
public class ServiceVoid {
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
    public Object randomGameLogic(){
        return 345;
    }
    @DepWire
    public void gameLogicWithParams(@Inject("getASixer") final int v,
                                    @Inject("getAnders") final String n)
    {}
}
