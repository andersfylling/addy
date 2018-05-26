package addy;

import addy.context.ServiceContext;
import addy.testdata.*;
import org.junit.Test;



import static org.junit.Assert.assertEquals;

public class InjectorTest {
    @Test
    public void testLoadingConfiguration() {
        Class<?> c = Services.class;
        Injector loader = new Injector(c);
        loader.activateFailOnNullInstance();
        loader.load();

        ServiceContext ctx = new ServiceContext();
        loader.installServices(ctx);

        // make sure all the methods were found, processed and instantiated
        assertEquals(Services.NUM_OF_GAME_COMPONENTS, ctx.size());
    }

    @Test
    public void testLoadingClassWithoutConfiguration() {
        Class<?> c = ServicesWithoutConfigAnnotation.class;
        ServiceContext ctx = new ServiceContext();
        Injector loader = new Injector(c);
        loader.activateFailOnNullInstance();
        loader.load();
        loader.installServices(ctx);

        // make sure all the methods were found, processed and instantiated
        assertEquals(0, ctx.size());
    }

    @Test
    public void testLoadingMultipleConfigurations() {
        Class<?> c1 = Services.class;
        Class<?> c2 = ServicesExtra.class;
        Injector loader = new Injector(c1, c2);
        loader.activateFailOnNullInstance();
        loader.load();

        ServiceContext ctx = new ServiceContext();
        loader.installServices(ctx);

        // make sure all the methods were found, processed and instantiated
        assertEquals(Services.NUM_OF_GAME_COMPONENTS + ServicesExtra.NUM_OF_GAME_COMPONENTS, ctx.size());
    }

    @Test(expected = InstantiationError.class)
    public void testServicesWithUnknownDependency() {
        Class<?> c = ServicesWithUnknownDep.class;
        Injector loader = new Injector(c);
        loader.activateFailOnNullInstance();
        loader.load();

        ServiceContext ctx = new ServiceContext();
        loader.installServices(ctx);

        assertEquals(0, ctx.size());
    }

    @Test(expected = InstantiationError.class)
    public void testSelfCyclingDependency() {
        Class<?> c = ServicesWithSelfDepCycling.class;
        Injector loader = new Injector(c);
        loader.activateFailOnNullInstance();
        loader.load();
    }

    @Test(expected = InstantiationError.class)
    public void testCyclingDependency() {
        Class<?> c = ServicesWithDepCycling.class;
        Injector loader = new Injector(c);
        loader.activateFailOnNullInstance();
        loader.load();
    }

}
