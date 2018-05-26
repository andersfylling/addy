package addy;

import addy.annotations.*;
import addy.context.ServiceSetter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Injector
{
    private static final String TAG = "GameConfigurationLoader";

    private final List<Class<?>> configs;
    private final List<GameComponentHolder> components;
    private final List<Object> instances;

    private boolean failOnNullInstance;

    // TODO: make use of customized annotations to help readability for projects
    private AnnotationConfig annotations;

    public Injector(final List<Class<?>> configs) {
        this.configs = new ArrayList<>(configs);
        this.components = new ArrayList<>();
        this.instances = new ArrayList<>();
        this.failOnNullInstance = false;
    }

    public Injector(Class<?>... configs) {
        this.configs = new ArrayList<>();
        this.configs.addAll(Arrays.asList(configs));

        this.instances = new ArrayList<>();
        this.components = new ArrayList<>();
        this.failOnNullInstance = false;
    }

    private List<String> getParameterServiceName(Method method) {
        List<String> params = new ArrayList<>();
        for (Annotation[] annotations : method.getParameterAnnotations()) {
            if (annotations.length == 0) {
                continue;
            }

            for (Annotation a : annotations) {
                if (a.annotationType() != Inject.class) {
                    continue;
                }

                Inject inject = (Inject) a;
                params.add(inject.value().toLowerCase());
                break;
            }
        }

        return params;
    }

    public void addGameConfigurations(final Class<?>... configs) {
        this.configs.addAll(Arrays.asList(configs));
    }

    public void addConfigurationInstances(final List<Object> instances) {
        this.instances.addAll(instances);
    }

    /**
     * Crash the program if a null instance is detected.
     * Otherwise a warning is given.
     */
    public void activateFailOnNullInstance() {
        this.failOnNullInstance = true;
    }

    /**
     * Add GameComponents manually, must be an instance however.
     * @param name
     * @param instance
     */
    public void addGameComponentInstance(final String name,
                                         final Object instance)
    {
        this.components.add(new GameComponentHolder(name.toLowerCase(), instance));
    }

    /**
     * Prioritize GameComponent.name over GameComponent.value
     * @param component
     * @param defaultName
     * @return
     */
    private String getGameComponentName(final Service component, final String defaultName) {
        if (component == null) {
            return defaultName.toLowerCase();
        }

        String name = defaultName;
        String componentName = component.name();
        String componentVal = component.value();

        if (componentVal.isEmpty() && !componentName.isEmpty()) {
            name = componentName;
        } else if (!componentVal.isEmpty() && componentName.isEmpty()) {
            name = componentVal;
        } else if (!componentVal.isEmpty() && !componentName.isEmpty()) {
            name = componentName;
        }

        return name.toLowerCase();
    }

    private void loadGameComponentRegisters(final Class<?> config, final Object instance) {
        // check for component list
        for (Method method : config.getDeclaredMethods()) {
            // ensure method is a GameComponent
            Service component = method.getAnnotation(Service.class);
            if (component == null) {
                continue;
            }

            // configure component
            // if the name attribute is not set, use the method name as fallback
            String name = "";
            String componentName = component.name();
            String componentVal = component.value();
            if (componentVal.isEmpty() && componentName.isEmpty()) {
                name = method.getName();
            } else if (componentVal.isEmpty() && !componentName.isEmpty()) {
                name = componentName;
            } else if (!componentVal.isEmpty() && componentName.isEmpty()) {
                name = componentVal;
            } else if (!componentVal.isEmpty() && !componentName.isEmpty() && componentName.equals(componentVal)) {
                name = componentName;
            } else if (!componentVal.isEmpty() && !componentName.isEmpty() && !componentName.equals(componentVal)) {
                throw new InstantiationError("different names suggested for game component when only one or zero is expected: " + componentName + ", " + componentVal);
            }

            // get params
            List<String> params = getParameterServiceName(method);

            GameComponentHolder data = new GameComponentHolder(
                    name.toLowerCase(),
                    method,
                    params,
                    GameComponentHolder::defaultMethodInvoker,
                    instance);
            components.add(data);
        }
    }

    private void loadGameComponentRegisters(final Class<?> config) {

        Object instance = null;
        try {
            instance = Class.forName(config.getName()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (instance == null) {
            return;
        }

        this.loadGameComponentRegisters(config, instance);
    }

    private void registerConstructorGameComponents(final Class<?> config) {
        Class<?>[] components = config.getAnnotation(ServiceLinker.class).value();
        for (Class<?> component : components) {
            Service service = component.getAnnotation(Service.class);
            if (service == null) {
                continue;
            }
            String className = component.getSimpleName();
            String name = this.getGameComponentName(service, className);

            // constructor annotations
            Constructor<?>[] constructors = component.getConstructors();
            List<String> dependencies = new ArrayList<>();
            Constructor<?> constructor = null;
            for (Constructor<?> candidate : constructors) {
                if (candidate.getAnnotation(DepWire.class) == null) {
                    continue;
                }

                constructor = candidate;
                break;
            }
            if (constructor == null) {
                throw new ClassFormatError("No constructor with @DepWire found for " + name);
            }

            Annotation[][] annotations = constructor.getParameterAnnotations();
            // look for @Inject
            for (Annotation[] param : annotations) {
                for (Annotation annotation : param) {
                    if (annotation.annotationType() != Inject.class) {
                        continue;
                    }

                    Inject inject = (Inject) annotation;
                    String paramName = inject.value();
                    dependencies.add(paramName);
                    break; // ONLY check the first occurring Inject annotation.
                }
            }

            GameComponentHolder holder = new GameComponentHolder(
                    name,
                    constructor,
                    dependencies,
                    GameComponentHolder::defaultConstructorInvoker,
                    null);
            this.components.add(holder);
        }
    }

    public void load() {
        for (final Class<?> config : this.configs) {
            if (config.getAnnotation(Configuration.class) == null) {
                continue;
            }

            // check if content of supplied list have any GameComponent
            if (config.getAnnotation(ServiceLinker.class) != null) {
                this.registerConstructorGameComponents(config);
            }

            // check methods
            this.loadGameComponentRegisters(config);
        }

        // also check live instances if injected
        // live instances can only use methods, otherwise the instance don't need
        // to actually use the GameComponent and it can be set from a config file
        // inside the game pkg in stead.
        for (Object instance : this.instances) {
            Class<?> config = instance.getClass();
            if (config.getAnnotation(Configuration.class) == null) {
                continue;
            }

            this.loadGameComponentRegisters(instance.getClass(), instance);
        }

        this.crashOnDuplicates();

        // update dependency list
        for (final GameComponentHolder component : this.components) {
            component.createDependencyTree(this.components);
        }

        this.sortByDependencies();

        // instantiate components
        for (GameComponentHolder component : this.components) {
            component.initialize(this.components);
        }

        this.crashOnNullInstances();

        // create game logic components

    }

    private void crashOnDuplicates() {
        // check for name duplicates
        for (GameComponentHolder a : this.components) {
            int counter = 0;
            final String name = a.getName();
            for (GameComponentHolder b : this.components) {
                if (name.equals(b.getName())) {
                    counter++;
                }
            }

            if (counter > 1) {
                throw new StackOverflowError("found " + Integer.toString(counter) + " instance of @Service " + a.toStringWithDependencies());
            }
        }
    }

    private void crashOnNullInstances() {
        // check for null instances and give a warning or fail
        for (GameComponentHolder component : this.components) {
            if (component.getInstance() != null) {
                continue;
            }

            String err = "instance for service was null: " + component.getName();
            if (this.failOnNullInstance) {
                throw new InstantiationError(err);
            } else {
                //Log.e(TAG, err);
                // TODO: logging
            }
        }
    }

    private void sortByDependencies() {
        // sort based on number of dependencies to speed up next sort
        this.components.sort((left, right) -> {
            int a = left.nrOfDependencies();
            int b = right.nrOfDependencies();

            if (a < b) {
                return -1;
            } else if (a > b) {
                return 1;
            }

            return 0;
        });

        boolean unsorted = true;
        while (unsorted) {
            unsorted = false;
            for (int i = 0; i < this.components.size() && !unsorted; i++) {
                GameComponentHolder current = this.components.get(i);
                if (current.getInstance() != null) {
                    continue;
                }

                List<String> deps = current.getDependencies();
                for (int j = i + 1; j < this.components.size() && !unsorted; j++) {
                    String n = this.components.get(j).getName();
                    for (String dep : deps) {
                        if (!dep.equals(n)) {
                            continue;
                        }
                        unsorted = true;
                        // move current component below found dependency
                        for (int y = i + 1; y <= j; y++) {
                            this.components.set(y - 1, this.components.get(y));
                        }
                        this.components.set(j, current);
                        break;
                    }
                }
            }
        }
    }

    public void installServices(ServiceSetter ctx) {
        // add instances to game context
        for (GameComponentHolder component : this.components) {
            ctx.setService(component.getName().toLowerCase(), component.getInstance());
        }
    }

    public void findDepWireMethodsAndPopulate() {
        for (Class<?> config : this.configs) {
            if (config.getAnnotation(Configuration.class) == null) {
                continue;
            }

            Object instance = null;
            try {
                instance = Class.forName(config.getName()).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (instance == null) {
                continue;
            }

            this.populateGameDepWireMethods(instance);
        }
        for (GameComponentHolder holder : this.components) {
            Object instance = holder.getInstance();
            this.populateGameDepWireMethods(instance);
        }

        // clear Game Components from this instance to free up memory
        this.components.clear();
    }

    private void populateGameDepWireMethods(final Object instance) {
        // get all methods with @GameDepWire
        Method[] methods = instance.getClass().getMethods();
        List<Method> gameDepWireMethods = new ArrayList<>();
        for (Method method : methods) { // ease up debug time
            if (method.getAnnotation(DepWire.class) == null) {
                continue;
            }

            gameDepWireMethods.add(method);
        }

        for (Method method : gameDepWireMethods) {
            List<String> params = getParameterServiceName(method);

            // get dependency instances
            List<Object> dependencies = new ArrayList<>();
            for (String dependency : params) {
                for (GameComponentHolder candidate : this.components) {
                    if (dependency.toLowerCase().equals(candidate.getName().toLowerCase())) {
                        dependencies.add(candidate.getInstance());
                        break;
                    }
                }
            }

            // inject dependencies
            try {
                GameComponentHolder.defaultMethodInvoker(method, dependencies.toArray(), instance);
            } catch (InvocationTargetException | IllegalAccessException e) {
                System.out.println("unable to inject params into method: " + method.getName() + ", in class: " + instance.getClass().getName());

                StringBuilder paramsStr = new StringBuilder();
                for (String param : params) {
                    paramsStr.append(param).append(", ");
                }
                System.out.println("params: " + paramsStr);

                e.printStackTrace();
            }
        }
    }


    public void setAnnotations(AnnotationConfig annotations) {
        this.annotations = annotations;
    }
}
