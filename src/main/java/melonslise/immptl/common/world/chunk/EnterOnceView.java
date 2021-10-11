package melonslise.immptl.common.world.chunk;

/**
 * A view which starts at positive infinity, then transitions to a provided distance and stays there, permanently
 */
public class EnterOnceView extends View{

    public EnterOnceView(RenderLoader loader, double distance)
    {
        super(loader);
        this.currDistance = distance;
    }

    /**
     * Updates the current distance to a new value, if that new value is lower than the current distance.
     * @param newDistance
     * @return - true if the new distance was lower, false otherwise.
     */
    @Override
    public boolean updateDistanceIfLower(double newDistance)
    {
        return false;
    }

    @Override
    public boolean wouldUpdate(double newDistance)
    {
        return false;
    }

    @Override
    public void finalizeUpdate()
    {
        this.prevDistance = this.currDistance;
    }

    /**
     * Determines if the view entered/exited the threshold, or stayed inside/outside the threshold
     * @param threshold
     * @return
     */
    @Override
    public Thresholds getThresholdStatus(double threshold)
    {
        // Currently within the threshold
        if (this.currDistance <= threshold)
        {
            if (this.prevDistance <= threshold)
            {
                return Thresholds.STAYED_INSIDE;
            }
            else
            {
                return Thresholds.ENTERED;
            }
        }
        // Not currently within the threshold
        else
        {
            return Thresholds.STAYED_OUTSIDE;
        }
    }
}
