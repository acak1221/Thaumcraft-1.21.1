package thaumcraft.api.aspects;

/** TC4's aspect-container contract. addToContainer returns the unaccepted remainder. */
public interface IAspectContainer {
    AspectList getAspects();
    void setAspects(AspectList aspects);
    boolean doesContainerAccept(Aspect aspect);
    int addToContainer(Aspect aspect, int amount);
    boolean takeFromContainer(Aspect aspect, int amount);
    /** Legacy multi-aspect removal with implementation-specific support; native nodes return false even for null.
     * @deprecated Use takeFromContainer(Aspect, int) in aspect units; a caller loop is not an atomic transaction. */
    @Deprecated boolean takeFromContainer(AspectList aspects);
    boolean doesContainerContainAmount(Aspect aspect, int amount);
    /** Legacy multi-aspect containment query; native nodes return false even for null rather than implementing an all/any test.
     * @deprecated Query doesContainerContainAmount(Aspect, int) for each required aspect to express all-of semantics explicitly; implementations may reject queries. */
    @Deprecated boolean doesContainerContain(AspectList aspects);
    int containerContains(Aspect aspect);
}
