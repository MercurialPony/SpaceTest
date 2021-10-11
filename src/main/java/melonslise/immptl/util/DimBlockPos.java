package melonslise.immptl.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DimBlockPos
{
	public final ResourceKey<Level> dimension;
	public final BlockPos pos;

	public DimBlockPos(ResourceKey<Level> dim, BlockPos pos)
	{
		this.dimension = dim;
		this.pos = pos;
	}

	@Override
	public String toString()
	{
		return "dimensional block position " + pos.toString() + " in dimension " + dimension.location();
	}

	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof DimBlockPos))
		{
			return false;
		}
		return ((DimBlockPos) obj).dimension.equals(this.dimension) && ((DimBlockPos) obj).pos.equals((this.pos));
	}

	@Override
	public int hashCode()
	{
		return this.dimension.hashCode()*this.pos.hashCode();
	}
}