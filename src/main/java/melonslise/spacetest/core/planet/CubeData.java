package melonslise.spacetest.core.planet;

import melonslise.spacetest.util.ArrayIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Function;

public class CubeData<T> implements Iterable<T>
{
	private Object[] data = new Object[6];

	public CubeData(Function<CubemapFace, T> factory)
	{
		for(CubemapFace face : CubemapFace.values())
		{
			this.data[face.ordinal()] = factory.apply(face);
		}
	}

	public T get(CubemapFace face)
	{
		return (T) this.data[face.ordinal()];
	}

	@NotNull
	@Override
	public Iterator<T> iterator()
	{
		return new ArrayIterator<>(this.data);
	}
}