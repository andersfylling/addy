package addy.context;

/**
 * Use this interface during runtime and not the GameContextGetterAssured as this won't crash
 * the program when the instance was not found.
 */
public interface ServiceGetter {
    Object getService(final String name);
}
