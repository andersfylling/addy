package addy;

import addy.annotations.*;

public class AnnotationConfig
        implements
        AnnotationConfiger
{
    // package private
    Class injectDependencies;
    Class configurationClass;
    Class service;
    Class services; // annotation that holds one or several service classes
    Class paramInject; // used to name params, some java versions won't let us read the param name with reflection

    public AnnotationConfig() {
        injectDependencies = DepWire.class;
        configurationClass = Configuration.class;
        paramInject = Inject.class;
        service = Service.class;
        services = ServiceLinker.class;
    }
}
