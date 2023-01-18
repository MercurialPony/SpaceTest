package melonslise.spacetest.render.planet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.util.math.ChunkSectionPos;

/**
 * Is essentially the vanilla BuiltChunkStorage but adapted to be more efficient for a single face of a planet
 */
@Environment(EnvType.CLIENT)
public class PlanetFaceStorage
{
	public ChunkSectionPos cornerChunkPos;
	public int faceSize;
	public int height;

	public ChunkBuilder.BuiltChunk[] chunks;

	public PlanetFaceStorage(ChunkBuilder chunkBuilder, ChunkSectionPos cornerChunkPos, int faceSize, int height)
	{
		this.cornerChunkPos = cornerChunkPos;
		this.faceSize = faceSize;
		this.height = height;

		this.createChunks(chunkBuilder);
	}

	public int getChunkIndex(int x, int y, int z)
	{
		return (z * this.height + y) * this.faceSize + x;
	}

	public void createChunks(ChunkBuilder chunkBuilder)
	{
		this.chunks = new ChunkBuilder.BuiltChunk[this.faceSize * this.faceSize * this.height];

		// TODO: use something like BlockPos#iterate here instead or use a single loop and calculate xyz
		for(int x = 0; x < this.faceSize; ++x)
		{
			for(int z = 0; z < this.faceSize; ++z)
			{
				for(int y = 0; y < this.height; ++y)
				{
					int idx = this.getChunkIndex(x, y, z);

					this.chunks[idx] = chunkBuilder.new BuiltChunk(idx, (this.cornerChunkPos.getX() + x) * 16, (this.cornerChunkPos.getY() + y) * 16, (this.cornerChunkPos.getZ() + z) * 16);
				}
			}
		}
	}

	public ChunkBuilder.BuiltChunk get(int x, int y, int z)
	{
		x -= this.cornerChunkPos.getX();
		y -= this.cornerChunkPos.getY();
		z -= this.cornerChunkPos.getZ();

		if(x < 0 || x >= this.faceSize || z < 0 || z >= this.faceSize || y < 0 || y >= this.height)
		{
			return null;
		}

		return this.chunks[this.getChunkIndex(x, y, z)];
	}
}