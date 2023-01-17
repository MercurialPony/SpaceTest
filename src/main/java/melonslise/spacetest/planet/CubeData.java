package melonslise.spacetest.planet;

import melonslise.spacetest.util.ArrayIterator;
import net.minecraft.util.math.ChunkSectionPos;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.BiFunction;

public class CubeData<T> implements Iterable<T>
{
	private Object[] data = new Object[6];

	public CubeData(ChunkSectionPos originChunkPos, int faceSize, BiFunction<CubemapFace, ChunkSectionPos, T> factory)
	{
		for(CubemapFace face : CubemapFace.values())
		{
			this.data[face.ordinal()] = factory.apply(face, originChunkPos.add(face.planeOffsetX * faceSize, 0, face.planeOffsetZ * faceSize));
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