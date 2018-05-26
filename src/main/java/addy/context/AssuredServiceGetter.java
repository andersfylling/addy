package addy.context;

/**
 * Use this when creating new objects in the game configuration class.
 * This will throw an exception if an error occurred to help us avoid null pointers exceptions.
 */
public interface AssuredServiceGetter
    extends ServiceGetter
{
    Object getAssuredService(final String name);
}
