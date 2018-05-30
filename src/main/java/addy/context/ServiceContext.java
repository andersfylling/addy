package addy.context;

import addy.Closer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ServiceContext
    implements
        AssuredServiceGetter,
        ServiceGetter,
        ServiceSetter,
        AnnotationGetter,
        Closeable
{
    public static final String NAME = "service-context";
    private final Map<String, Object> services;

    public ServiceContext() {
        this.services = new HashMap<>();
    }

    @Override
    public void setService(String name, final Object service) {
        if (name == null || name.isEmpty()) {
            String err = "unable to store class instance without a name for object: " + service.toString();
            throw new RuntimeException(err);
        }
        if (service == null) {
            String err = "cannot store null instance";
            throw new RuntimeException(err);
        }
        this.services.put(name.toLowerCase(), service);
    }

    @Override
    public Object getService(final String name) {
        if (this.services.containsKey(name.toLowerCase())) {
            return this.services.get(name.toLowerCase());
        }

        return null;
    }

    @Override
    public Object getAssuredService(final String name) {
        Object instance = this.getService(name);

        if (instance == null) {
            final String err = String.format("no instance with the name %s exists", name);
            throw new InstantiationError(err);
        }

        return instance;
    }

    public Map<String, Object> getServices() {
        return this.services;
    }

    public int size() {
        return this.services.size();
    }

    public void foreach(final ServiceVoidCallback cb) {
        this.foreach((String name, Object srv) -> {
            cb.addService(name, srv);
            return false;
        }, null);
    }
    public Map<String, Object> foreach(final ServiceCallback cb) {
        return this.foreach(cb, null);
    }

    public Map<String, Object> foreach(final ServiceCallback cb, final Class classAnnotation) {
        Map<String, Object> services = new HashMap<>();
        for (Map.Entry<String, Object> entry : this.services.entrySet()) {
            Object v = entry.getValue();
            if (classAnnotation != null && v.getClass().getAnnotation(classAnnotation) == null) {
                continue;
            }

            String k = entry.getKey();
            if (cb == null || cb.addService(k, v)) {
                services.put(k, v);
            }
        }

        return services;
    }

    public void addDistinctService(List<Object> services, Object[] incoming) {
        for (Object service : incoming) {
            if (services.contains(service)) {
                continue;
            }

            services.add(service);
        }
    }

    @Override
    public Object[] getServicesWithAnnotation(final Class annotation) {
        List<Object> services = new ArrayList<>();

        addDistinctService(services, this.getServicesWithClassAnnotation(annotation));
        addDistinctService(services, this.getServicesWithConstructorAnnotation(annotation));
        addDistinctService(services, this.getServicesWithMethodAnnotation(annotation));
        addDistinctService(services, this.getServicesWithMemberAnnotation(annotation));

        return services.toArray();
    }

    @Override
    public Object[] getServicesWithClassAnnotation(Class annotation) {
        Collection<Object> values = this.foreach(null, annotation).values();

        List<Object> services = new ArrayList<>(values);
        return services.toArray();
    }

    @Override
    public Object[] getServicesWithConstructorAnnotation(Class annotation) {
        Collection<Object> values = this.foreach((String name, Object service) -> {
            for (Constructor constructor : service.getClass().getDeclaredConstructors()) {
                if (constructor.getAnnotation(annotation) != null) {
                    return true;
                }
            }

            return false;
        }).values();

        List<Object> services = new ArrayList<>(values);
        return services.toArray();
    }

    @Override
    public Object[] getServicesWithMethodAnnotation(Class annotation) {
        Collection<Object> values = this.foreach((String name, Object service) -> {
            for (Method method : service.getClass().getDeclaredMethods()) {
                if (method.getAnnotation(annotation) != null) {
                    return true;
                }
            }

            return false;
        }).values();

        List<Object> services = new ArrayList<>(values);
        return services.toArray();
    }

    @Override
    public Object[] getServicesWithMemberAnnotation(Class annotation) {
        Collection<Object> values = this.foreach((String name, Object service) -> {
            for (Field field : service.getClass().getDeclaredFields()) {
                if (field.getAnnotation(annotation) != null) {
                    return true;
                }
            }
            return false;
        }).values();

        List<Object> services = new ArrayList<>(values);
        return services.toArray();
    }

    public Object[] getServicesWithInterface(Class i) {
        Collection<Object> values = this.foreach((String name, Object service) -> {
            for (Class interfaceClass: service.getClass().getInterfaces()) {
                if (interfaceClass == i) {
                    return true;
                }
            }
            return false;
        }).values();

        return values.toArray();
    }

    private void print() {
        System.out.println("Services");
        foreach((String name, Object srv) -> {
            System.out.print("srv => " + name + "\n");
            return false;
        });
        System.out.print("-----------\n");
        System.out.flush();
    }


    /**
     * When shutting down the application, go through every service
     * implementing the Closer interface and invoke the close method.
     */
    @Override
    public void close()
    {
        for (Object service : getServicesWithInterface(Closer.class)) {
            Closer closer = (Closer) service;
            try {
                closer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
