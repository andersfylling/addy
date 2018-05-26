package addy.context;

import java.util.Map;

public interface AnnotationGetter
{
    Object[] getServicesWithAnnotation(Class annotation);
    //Object[] getServicesWithAnnotations(Class[] annotations);
    Object[] getServicesWithClassAnnotation(Class annotation);
    //Object[] getServicesWithClassAnnotations(Class[] annotations);
    Object[] getServicesWithConstructorAnnotation(Class annotation);
    //Object[] getServicesWithConstructorAnnotations(Class[] annotations);
    Object[] getServicesWithMethodAnnotation(Class annotation);
    //Object[] getServicesWithMethodAnnotations(Class[] annotations);
    Object[] getServicesWithMemberAnnotation(Class annotation);
    //Object[] getServicesWithMemberAnnotations(Class[] annotations);

    Map<String, Object> foreach(final ServiceCallback cb);
    Map<String, Object> foreach(final ServiceCallback cb, final Class classAnnotation);
}
