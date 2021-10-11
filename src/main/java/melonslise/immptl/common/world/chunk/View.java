package melonslise.immptl.common.world.chunk;

public class View implements Comparable<View>{
    protected double prevDistance;
    protected double currDistance;
    protected RenderLoader loader;

    public View(RenderLoader loader)
    {
        this.currDistance = Double.POSITIVE_INFINITY;
        this.prevDistance = currDistance;
        this.loader = loader;
    }

    /**
     * Updates the current distance to a new value, if that new value is lower than the current distance.
     * @param newDistance
     * @return - true if the new distance was lower, false otherwise.
     */
    public boolean updateDistanceIfLower(double newDistance)
    {
        if (newDistance < this.currDistance)
        {
            this.currDistance = newDistance;
            return true;
        }
        return false;
    }

    public boolean wouldUpdate(double newDistance)
    {
        return newDistance <= this.currDistance;
    }

    public void finalizeUpdate()
    {
        this.prevDistance = this.currDistance;
        this.currDistance = Double.POSITIVE_INFINITY;
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
        // Currently within the threshold
        if (this.currDistance <= threshold)
        {
            // Was previously inside the threshold
            if (this.prevDistance <= threshold)
            {
                return Thresholds.STAYED_INSIDE;
            }
            // Was previously outside the threshold
            else
            {
                return Thresholds.ENTERED;
            }
        }
        // Not currently within the threshold
        else
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

    public int compareTo(View obj)
    {
        // Sorts by current distance
        if (this == obj)
        {
            return 0;
        }
        if (this.currDistance < obj.currDistance)
        {
            return -1;
        }
        if (this.currDistance > obj.currDistance)
        {
            return 1;
        }
        // Sort within the same current distance
        if (this.loader == obj.loader)
        {
            return (System.identityHashCode(this) > System.identityHashCode(obj)) ? 1 : -1;
        }
        else
        {
            return (System.identityHashCode(this.loader) > System.identityHashCode(obj.loader)) ? 1 : -1;
        }
    }

    @Override
    public String toString()
    {
        return "View object. Current distance: "+this.currDistance+"; Previous distance: "+this.prevDistance;
    }

    public static enum Thresholds {
        ENTERED, EXITED, STAYED_INSIDE, STAYED_OUTSIDE
    }
}
