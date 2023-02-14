package melonslise.spacetest.core.planet;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.chunk.ChunkBuilder;

/**
 * Is essentially the vanilla BuiltChunkStorage but adapted to be more efficient for a single face of a planet
 */
@Environment(EnvType.CLIENT)
public class VanillaPlanetFaceStorage
{
	public final CubeFaceContext ctx;

	public ChunkBuilder.BuiltChunk[] chunks;

	public VanillaPlanetFaceStorage(ChunkBuilder chunkBuilder, CubeFaceContext ctx)
	{
		this.ctx = ctx;

		this.createChunks(chunkBuilder);
	}

	public int getChunkIndex(int x, int y, int z)
	{
		return (z * this.ctx.faceHeight() + y) * this.ctx.faceSize() + x;
	}

	public void createChunks(ChunkBuilder chunkBuilder)
	{
		this.chunks = new ChunkBuilder.BuiltChunk[this.ctx.faceSize() * this.ctx.faceSize() * this.ctx.faceHeight()];

		// TODO: use something like BlockPos#iterate here instead or use a single loop and calculate xyz
		for(int x = 0; x < this.ctx.faceSize(); ++x)
		{
			for(int z = 0; z < this.ctx.faceSize(); ++z)
			{
				for(int y = 0; y < this.ctx.faceHeight(); ++y)
				{
					int idx = this.getChunkIndex(x, y, z);

					this.chunks[idx] = chunkBuilder.new BuiltChunk(idx, (this.ctx.x() + x) * 16, (this.ctx.y() + y) * 16, (this.ctx.z() + z) * 16);
				}
			}
		}
	}

	public ChunkBuilder.BuiltChunk get(int x, int y, int z)
	{
		x -= this.ctx.x();
		y -= this.ctx.y();
		z -= this.ctx.z();

		if(x < 0 || x >= this.ctx.faceSize() || z < 0 || z >= this.ctx.faceSize() || y < 0 || y >= this.ctx.faceHeight())
		{
			return null;
		}

		return this.chunks[this.getChunkIndex(x, y, z)];
	}
}