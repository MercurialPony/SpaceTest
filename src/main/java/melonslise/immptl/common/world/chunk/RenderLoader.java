package melonslise.immptl.common.world.chunk;

import melonslise.immptl.util.DimBlockPos;

public interface RenderLoader {
    /**
     * Returns where the actual render loader is located.
     * @return
     */
    public DimBlockPos getOwnerLocation();

    /**
     * Returns whether the provided dimensional block position is in the current chunks.
     * @param pos
     * @return
     */
    public boolean isPositionInCurrent(DimBlockPos pos);

    /**
     * Returns whether or not this renderloader has chunks that were added in the previous update
     * @return
     */
    // FIXME Unused.
    public boolean hasAdded();

    /**
     * Returns whether or not this renderloader has chunks that were removed in the previous update
     * @return
     */
    // FIXME Unused.
    public boolean hasRemoved();

    /**
     * Used to run updates specific to the renderloader. Copies the current state to the old state, and updates the
     * current state.
     * Calls to the "Added", "Removed", "Current", and "Old" methods operate on whatever these states are, so this
     * method should be called before calling those methods, on each tick.
     * Maybe have a "setPendingUpdate" method(s), for updating the renderloader's state based on external information?
     * ???
     * Maybe this should take the server's current time (tick time)?
     */
    public void update();

    /**
     * Finalizes updates run in this renderloader - meaning, the old state is overwritten - "forgets" which chunks were added/removed.
     */
    public void finalizeUpdate();

    /**
     * Returns the distance to a given position from this renderloader's "source"
     * @param pos
     * @return
     */
    public double getSourceDistanceTo(DimBlockPos pos);
}
