package melonslise.spacetest.util;

@FunctionalInterface
public interface Vec3iFunction<R>
{
	R apply(int x, int y, int z);
}