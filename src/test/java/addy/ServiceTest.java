package addy;

import addy.testdata.*;
import org.junit.Test;

public class ServiceTest {
    public ServiceTest() {}

    @Test
    public void testLoadSimpleGameComponent() {
        InjectorManager initializer = new InjectorManager(
                addy.testdata.Services.class
        );
        initializer.loadAndWait();
        initializer.getSrvCtx().close();
    }



    // Check for cycling dependency issues
    //

    @Test(expected = InstantiationError.class)
    public void testSelfCyclingDependency() {
        Injector injector = new Injector(ServicesWithSelfDepCycling.class);
        injector.load();
        injector.branchOutDependencyTree();
        injector.sortByDependencies();
        injector.instantiateComponents();
        injector.crashOnNullInstances();
    }

    @Test(expected = InstantiationError.class)
    public void testCyclingDependency() {
        Injector injector = new Injector(ServicesWithDepCycling.class);
        injector.load();
        injector.branchOutDependencyTree();
        injector.sortByDependencies();
        injector.instantiateComponents();
        injector.crashOnNullInstances();
    }
}
