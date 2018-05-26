package addy;

import addy.context.ServiceContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InjectorManager
{

    private final ServiceContext ctx;
    private final List<Class<?>> configClasses;
    private final List<Object> configClassInstances;
    private final Injector injector;
    private final AnnotationConfig annotations;

    /**
     * A package to be scanned for classes with the annotation @GameConfiguration
     *
     * @param configClasses example: team.adderall.game.Configuration.class
     */
    public InjectorManager(final Class<?>... configClasses) {
        this.ctx = new ServiceContext();

        this.configClassInstances = new ArrayList<>();
        this.configClasses = new ArrayList<>();
        this.configClasses.addAll(Arrays.asList(configClasses));
        this.annotations = new AnnotationConfig();

        this.injector = new Injector(this.configClasses);
        this.injector.setAnnotations(this.annotations);

        this.injector.addServiceInstance(ServiceContext.NAME, this.ctx);
    }

    /**
     * Crash the application
     *
     * @param err
     */
    private void missingRequiredInstance(final String err) {
        throw new InstantiationError("a required class instance is missing and must be registered: " + err);
    }

    /**
     * Load all instances to memory, blocked
     */
    public void loadAndWait() {
        this.injector.addConfigurationInstances(this.configClassInstances);

        // load all @GameComponents from @GameConfiguration classes
        this.injector.activateFailOnNullInstance();
        this.injector.load();

        // add the component instances to the GameContext
        injector.installServices(ctx);

        // invoke DepWire methods
        injector.findDepWireMethodsAndPopulate();
    }

    /**
     * Load all instances to memory asynchronously and notify listener
     */
    public void load(final FinishedLoading callback) {
        final InjectorManager self = this;
        (new Thread(() -> {
            // load all the components into memory and initialize them
            self.loadAndWait();

            // check if we should fire the callback
            if (callback != null) {
                callback.trigger();
            }

            // TODO: is this thread ever killed?
        })).start();
    }

    public ServiceContext getSrvCtx() {
        return ctx;
    }

    /**
     * Must be added before this.load is called.
     *
     * @param instances
     */
    public void addInstantiadedConfigurations(Object... instances) {
        this.configClassInstances.addAll(Arrays.asList(instances));
    }

//    private void populateGameLogicManager() {
//        GameLogicManager manager = (GameLogicManager) this.ctx.getAssuredInstance("gameLogicManager");
//
//        for (Map.Entry<String, Object> entry : this.ctx.getServices().entrySet()) {
//            Object component = entry.getValue();
//            if (component.getClass().getAnnotation(GameLogic.class) == null) {
//                continue;
//            }
//
//            int wave = component.getClass().getAnnotation(GameLogic.class).wave();
//            manager.addGameLogic(wave, (GameLogicInterface) component);
//        }
//    }

    public void close() {
        this.ctx.close();
    }
}
