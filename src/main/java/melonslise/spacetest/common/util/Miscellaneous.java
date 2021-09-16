package melonslise.spacetest.common.util;

import net.minecraftforge.fml.util.thread.SidedThreadGroups;

public class Miscellaneous {
    public static boolean IsServerSide() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }
}
