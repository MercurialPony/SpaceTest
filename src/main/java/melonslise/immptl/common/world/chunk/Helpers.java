package melonslise.immptl.common.world.chunk;

public class Helpers {
    /**
     * Basic method for executing a method for all positions within a provided rectangle
     * The x range is [xStart, xStart+xWidth); likewise for the z range
     * So xStart=3, xWidth=3, would result in the x-range [3,4,5] - three elements, starting at x=3.
     * @param xStart - x start of the range (-x side)
     * @param zStart - z start of the range (-z side)
     * @param xWidth - x width of the range - how many x-positions there are.
     * @param zWidth - x width of the range
     * @param consumer - the method to run for each position
     */
    public static void forEachInRectangularRange(int xStart, int zStart, int xWidth, int zWidth, TwoIntConsumer consumer)
    {
        for (int x = xStart; x < (xStart+xWidth); x++)
        {
            for (int z = zStart; z < (zStart+zWidth); z++)
            {
                consumer.consume(x, z);
            }
        }
    }

    /**
     * Method which executes a given consumer on all positions within a provided rectangle, except for positions inside
     * a masking rectangle, of equal size to the main rectangle
     * @param xStart - x start of the range (-x side)
     * @param zStart - z start of the range (-z side)
     * @param xWidth - x width of both ranges - how many x-positions there are.
     * @param zWidth - x width of both ranges
     * @param xStartExclude - x start of the exclusion range (-x side)
     * @param zStartExclude - z start of the exclusion range (-z side)
     * @param consumer - the method to run for each included position
     */
    public static void forEachInRectangularExcluded(int xStart, int zStart, int xWidth, int zWidth,
                                                    int xStartExclude, int zStartExclude, TwoIntConsumer consumer)
    {
        // Recently added chunks are all chunks in the new range around the player, excluding old chunks
        // As the player has a square render zone around them, that means the recently added chunks will take a
        // rectangular or L shape. So they can be expressed as a pair of two rectangles.
        // Coordinates of the (-x, -z) corners of the old and new loading rectangles

        // Rectangle 1:
        int xStartRectangle = xStart;
        int zStartRectangle = zStart;
        int xWidthRectangle = Math.min(xWidth, Math.abs(xStartExclude-xStart));
        int zWidthRectangle = zWidth;

        if ((xStartExclude <= xStart) && (xStart < (xStartExclude+xWidth)))
        {
            xStartRectangle = xStartExclude + xWidth;
        }
        Helpers.forEachInRectangularRange(xStartRectangle, zStartRectangle, xWidthRectangle, zWidthRectangle, consumer);

        // Rectangle 2
        xStartRectangle = xStartExclude;
        zStartRectangle = zStart;
        xWidthRectangle = xWidth - xWidthRectangle;
        zWidthRectangle = Math.min(zWidth, Math.abs(zStartExclude-zStart));
        if ((xStartExclude < xStart) && (xStart < (xStartExclude+xWidth)))
        {
            xStartRectangle = xStart;
        }
        if ((zStartExclude <= zStart) && (zStart < (zStartExclude+zWidth)))
        {
            zStartRectangle = zStartExclude+zWidth;
        }
        Helpers.forEachInRectangularRange(xStartRectangle, zStartRectangle, xWidthRectangle, zWidthRectangle, consumer);

        // Well, I found out why vanilla just does it as two separate cases, and iterates over every position contained
        // in either of them.
    }

    /**
     * Returns whether or not the provided position is within the rectangular range provided.
     * Inclusive - includes the edges in the range.
     * For xStart = 4, and xWidth = 5, the following x-coords would be in the x-range: {4, 5, 6, 7, 8}
     * @param xPos - x coordinate of the position
     * @param zPos - z coordinate of the position
     * @param xStart - x-coord of the (-x, -z) corner of the rectangle
     * @param zStart - z-coord of the (-x, -z) corner of the rectangle
     * @param xWidth - xWidth of the rectangle (how many positions)
     * @param zWidth - zWidth of the rectangle (how many positions)
     * @return
     */
    public static boolean isInRectangle(int xPos, int zPos, int xStart, int zStart, int xWidth, int zWidth)
    {
        // Use less than for the start+width, because the width is the # of positions
        // For xStart=4, and xWidth = 5, the total would be 4+5=9, which is outside the desired range.
        return (xPos >= xStart) && (xPos < (xStart+xWidth))
        && (zPos >= zStart) && (zPos < (zStart+zWidth));
    }

    public static int getViewWidth(int viewDistance)
    {
        return viewDistance*2+1;
    }

    public static int computeTrueViewDistance(int viewDistance) { return viewDistance+1;}

//    public static <S, T> void dumpMap(Map<S, T> map, String header, Pair<String, String> labels)
//    {
//        SpaceTest.LOGGER.info(header);
//        map.forEach((key, value) -> {
//            SpaceTest.LOGGER.info(labels.getKey()+key+labels.getValue()+value);
//        });
//    }
//
//    public static <S, T> void dumpSet(Set<S> set, String header)
//    {
//        set.stream().sorted().forEach((item) -> {
//            SpaceTest.LOGGER.info(header+item);
//        });
//    }
//
//    public static <S, T> void dumpSet(Set<S> set, String header, Function<S, T> converter)
//    {
//        set.stream().sorted().forEach((item) -> {
//            SpaceTest.LOGGER.info(header+converter.apply(item));
//        });
//    }
//
//    public static <S, T extends Set<U>, U, V> void dumpMapSet(Map<S, T> map, String header, Pair<String, String> labels, Function<U, V> converter)
//    {
//        SpaceTest.LOGGER.info(header);
//        map.forEach((key, value) -> {
//            SpaceTest.LOGGER.info(labels.getKey()+key);
//            dumpSet(value, header, converter);
//        });
//    }

//    public static <S, T extends Map<U, V>, U, V> void dumpMapMap(Map<S, T> map, String header, Pair<String, String> labels, Function<U, V> subKeyConverter)
//    {
//        SpaceTest.LOGGER.info(header);
//        map.forEach((key, value) -> {
//            SpaceTest.LOGGER.info(labels.getKey()+key);
//            dumpSet(value, header, converter);
//        });
//    }

    @FunctionalInterface
    public static interface TwoIntConsumer
    {
        public void consume(int x, int z);
    }
}
