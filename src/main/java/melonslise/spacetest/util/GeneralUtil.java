package melonslise.spacetest.util;

public final class GeneralUtil
{
	public static boolean checkBit(int x, int i)
	{
		return (x & 1 << i) != 0;
	}

	public static float log(float base, float x)
	{
		return (float) Math.log10(x) / (float) Math.log10(base);
	}
}