package melonslise.spacetest.util;

@FunctionalInterface
public interface Vec3iConsumer
{
	void accept(int x, int y, int z);
}