package addy;

import addy.annotations.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class GameComponentHolder
{

    private final String name;
    private final Object function;
    private final List<String> dependencies;
    private final List<String> requiredDependencies;
    private final ServiceInvoker initializer;
    private final Object classInstance;

    private Object instance;

    /**
     * Used to initialize either a constructor or method.
     *
     * @param name
     * @param parameters
     * @param initializer
     */
    public GameComponentHolder(final String name,
                               final Object function,
                               final List<String> parameters,
                               final ServiceInvoker initializer,
                               final Object classInstance)
    {
        this.name = name;
        this.function = function;
        this.dependencies = new ArrayList<>(parameters); // this will populate later on
        this.requiredDependencies = new ArrayList<>(parameters);
        this.initializer = initializer;
        this.classInstance = classInstance;

        this.instance = null;
    }

    /**
     * Add already initialized components.
     *
     * @param name
     * @param instance
     */
    public GameComponentHolder(final String name,
                               final Object instance)
    {
        this.name = name;
        this.instance = instance;

        this.function = null;
        this.dependencies = new ArrayList<>();
        this.requiredDependencies = new ArrayList<>();
        this.initializer = null;
        this.classInstance = null;
    }

    /**
     * Given all objects added to the injector, we recursively go through
     * dependencies and add them to our dependency list. We can then detect dependency
     * cycling and find out which order components needs to be instantiated for for every
     * dependency to not get a null param.
     *
     * @param components is a list of injectable components/services (initialized or not)
     */
    public void createDependencyTree(final List<GameComponentHolder> components)
    {
        if (this.instance != null) {
            return;
        }

        this.populateDependencyTree(components, this.name);
    }

    /**
     * Supplement the root element to help detect circular dependency.
     *
     * @param components
     * @param root
     * @see GameComponentHolder#createDependencyTree
     */
    private void populateDependencyTree(final List<GameComponentHolder> components,
                                        final String root)
    {
        for (GameComponentHolder dependency : components) {
            final String depName = dependency.getName();

            // don't evaluate itself
            if (this.name.equals(depName)) {
                continue;
            }

            // ignore if not a dependency
            if (!this.dependsOnComponent(depName, false)) {
                continue;
            }

            // if this is a dependency and it requires the root component,
            // its a circular dependency issue
            if (!root.equals(this.name) && root.equals(depName)) {
                throwDependencyCyclingError(root, this.name);
            }

            // skip dependencies with no sub dependencies
            if (dependency.nrOfDependencies() == 0) {
                continue;
            }

            // lay out the dependencies of the dependency
            dependency.populateDependencyTree(components, root);

            List<String> required = dependency.getDependencies();
            for (String want : required) {
                boolean alreadyGotIt = false;
                for (String got : this.dependencies) {
                    if (want.equals(got)) {
                        alreadyGotIt = true;
                        break;
                    }
                }

                if (!alreadyGotIt) {
                    this.dependencies.add(want);
                }
            }
        }
    }

    /**
     * Initialize component and every dependency recursively.
     */
    public void initialize(final List<GameComponentHolder> components)
    {
        if (this.instance != null) {
            return;
        }

        // check if component uses itself as a param
        for (final String dependency : this.dependencies) {
            if (this.name.equals(dependency)) {
                throwDependencyCyclingError(this.name, this.name);
            }
        }

        // find and initialize every construction dependency
        // (dependencies required later, "insert DI", is ignored)
        List<GameComponentHolder> parameters = new ArrayList<>();
        for (final String dependency : this.dependencies) {
            for (final GameComponentHolder component : components) {
                if (!dependency.equals(component.getName())) {
                    continue;
                }

                component.initialize(components);
                final Object depInstance = component.getInstance();
                if (depInstance != null && this.dependsOnComponent(component.getName(), true)) {
                    parameters.add(component);
                }
                break;
            }
        }

        // check if any dependencies are missing
        if (this.requiredDependencies.size() != parameters.size()) {
            String err = "missing dependencies for @GameComponent: " + this.toStringWithDependencies();

            StringBuilder have = new StringBuilder();
            for (Object dep : parameters) {
                have.append(dep.toString()).append(",");
            }


            err += ", have: " + have;

            throw new InstantiationError(err);
        }

        // convert GameComponent to Object instance
        List<Object> instances = new ArrayList<>();
        for (GameComponentHolder holder : parameters) {
            instances.add(holder.getInstance());
        }

        // everything is alright, instantiate component
        try {
            this.instance = this.initializer.initiate(this.function, instances.toArray(), this.classInstance);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void throwDependencyCyclingError(String a, String b)
    {
        throw new InstantiationError("both methods require each other (cycling dependency): " + a + ", " + b);
    }

    /**
     * Check if a given component is a dependency of this.
     *
     * @param name of the potential dependency
     * @return
     */
    public boolean dependsOnComponent(final String name, boolean checkOnlyMains)
    {
        boolean dependent = false;
        List<String> dependencies;
        if (checkOnlyMains) {
            dependencies = this.requiredDependencies;
        } else {
            dependencies = this.dependencies;
        }

        for (final String dependency : dependencies) {
            if (name.equals(dependency)) {
                dependent = true;
                break;
            }
        }

        return dependent;
    }

    // ########################################################################################
    // ###
    // ### Getters / setters
    // ###
    // ########################################################################################
    public String getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public int nrOfDependencies() {
        return dependencies.size();
    }

    public Object getInstance() {
        return instance;
    }


    @Override
    public String toString() {
        return this.name;
    }

    public String toStringWithDependencies()
    {
        StringBuilder content = new StringBuilder(this.name + "{");
        for (String dependency : this.requiredDependencies) {
            content.append(dependency).append(",");
        }
        content.append("}");

        return content.toString();
    }

    public String toStringWithAllDependencies()
    {
        StringBuilder content = new StringBuilder(this.name + "{");
        for (String dependency : this.dependencies) {
            content.append(dependency).append(",");
        }
        content.append("}");

        return content.toString();
    }


    // ########################################################################################
    // ###
    // ### Default initializer's according to GameComponentInvoker
    // ###
    // ########################################################################################

    /**
     * Initiate a class (constructor)
     *
     * @param constructor
     * @param dependencies
     * @param p ignored
     * @return
     * @see addy.annotations.ServiceInvoker
     */
    public static Object defaultConstructorInvoker(final Object constructor,
                                                   final Object[] dependencies,
                                                   Object p)
            throws
            IllegalAccessException,
            InvocationTargetException,
            InstantiationException
    {
        if (!(constructor instanceof Constructor)) {
            return null;
        }
        Constructor c = (Constructor) constructor;

        Object instance;
        if (dependencies.length == 0) {
            instance = c.newInstance();
        } else {
            instance = c.newInstance(dependencies);
        }

        return instance;
    }

    /**
     * Initiate a method
     *
     * @param method
     * @param dependencies
     * @param classInstance
     * @return
     * @see addy.annotations.ServiceInvoker
     */
    public static Object defaultMethodInvoker(final Object method,
                                              final Object[] dependencies,
                                              final Object classInstance)
            throws
            InvocationTargetException,
            IllegalAccessException
    {
        if (!(method instanceof Method)) {
            return null;
        }

        Method m = (Method) method;

        Object instance;
        if (dependencies.length == 0) {
            instance = m.invoke(classInstance);
        } else {
            instance = m.invoke(classInstance, dependencies);
        }

        return instance;
    }
}
