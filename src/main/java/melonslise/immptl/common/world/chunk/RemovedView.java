package melonslise.immptl.common.world.chunk;

public class RemovedView extends View{
    public RemovedView(View oldView)
    {
        super(oldView.loader);
        this.prevDistance = oldView.prevDistance;
        this.currDistance = Double.POSITIVE_INFINITY;
    }


    /**
     * Updates the current distance to a new value, if that new value is lower than the current distance.
     * @param newDistance
     * @return - true if the new distance was lower, false otherwise.
     */
    public boolean updateDistanceIfLower(double newDistance)
    {
        return false;
    }

    public boolean wouldUpdate(double newDistance)
    {
        return false;
    }

    public void finalizeUpdate()
    {

    }

    public double getCurrDistance()
    {
        return this.currDistance;
    }

    /**
     * Determines if the view entered/exited the threshold, or stayed inside/outside the threshold
     * @param threshold
     * @return
     */
    public Thresholds getThresholdStatus(double threshold)
    {
        // Was previously inside the threshold
        if (this.prevDistance <= threshold)
        {
            return Thresholds.EXITED;
        }
        // Was previously outside the threshold
        else
        {
            return Thresholds.STAYED_OUTSIDE;
        }
    }
}
