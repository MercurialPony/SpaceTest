package melonslise.spacetest.blockentity;

import melonslise.spacetest.client.render.planet.PlanetRenderer;
import melonslise.spacetest.init.StBlockEntities;
import melonslise.spacetest.planet.BasicPlanetProperties;
import melonslise.spacetest.planet.PlanetProperties;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;

public class PlanetBlockEntity extends BlockEntity
{
	@Environment(EnvType.CLIENT)
	public PlanetRenderer pr;

	public PlanetProperties planetProps;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(StBlockEntities.PLANET, pos, state);
	}

	public void placed(BlockPos pos, LivingEntity placer)
	{
		this.planetProps = new BasicPlanetProperties(new Vec3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d), ChunkSectionPos.from(pos.withY(placer.world.getBottomY())), 5, 62);

		if(placer instanceof ServerPlayerEntity player)
		{
			PortalAPI.addChunkLoaderForPlayer(player, new ChunkLoader(new DimensionalChunkPos(World.OVERWORLD, new ChunkPos(pos)), this.planetProps.getFaceSize() * 2 + 1));
		}
	}
}