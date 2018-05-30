package addy.context;

import java.util.Map;

/**
 * Use this interface during runtime and not the GameContextGetterAssured as this won't crash
 * the program when the instance was not found.
 */
public interface ServiceGetter {
    void foreach(final ServiceVoidCallback cb);
    Map<String, Object> foreach(final ServiceCallback cb);
    Object getService(final String name);
}
