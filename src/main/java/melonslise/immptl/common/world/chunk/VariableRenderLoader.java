package melonslise.immptl.common.world.chunk;

import java.util.function.LongConsumer;

/**
 * Interface that represents a render loader for which the chunks it's watching can change when the view distance changes.
 */
public interface VariableRenderLoader {

    /**
     * Updates the renderloader's view distance.
     * @param newViewDistance
     */
    public void updateViewDistance(int newViewDistance);

//    /**
//     * Finalizes a view distance update for the renderloader, overwriting the copy of the old view distance with the
//     * new view distance.
//     * Should be called when all work involving the view distance update is complete.
//     */
//    public void finalizeViewDistanceUpdate();

    /**
     *
     * Gets the renderloader's current view distance, in blocks.
     * @return - View distance in chunks.
     */
    public int getBlockViewDistance();

    /**
     * Executes a provided method for every chunk that was added to watching by a view distance change.
     * Note that it uses the player's OLD position, just like vanilla does.
     * FIXME should this use the new position? It only made sense when I was reading the player's position, under the
     *  assumption that this's updates were synched with the player's.
     * @param consumer - method to execute.
     */
    public void forEachViewAdded(LongConsumer consumer);

    /**
     * Executes a provided method for every chunk that was removed from watching by a view distance change.
     * Note that it uses the player's OLD position, just like vanilla does.
     * FIXME should this use the new position?
     * @param consumer - method to execute.
     */
    public void forEachViewRemoved(LongConsumer consumer);

    /**
     * Returns whether or not chunks were added to the renderloader's chunks due to a view distance change.
     * @return
     */
    public boolean hasViewAdded();

    /**
     * Returns whether or not chunks were removed from the renderloader's chunks due to a view distance change.
     * @return
     */
    public boolean hasViewRemoved();
}
