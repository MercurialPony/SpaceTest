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
import net.minecraft.util.math.*;
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
		this.planetProps = new BasicPlanetProperties(new Vec3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d), ChunkSectionPos.from(pos.withY(placer.world.getBottomY())), 5, 62 - placer.world.getBottomY());

		if(placer instanceof ServerPlayerEntity player)
		{
			PortalAPI.addChunkLoaderForPlayer(player, new ChunkLoader(new DimensionalChunkPos(World.OVERWORLD, new ChunkPos(pos)), this.planetProps.getFaceSize() * 2 + 1));
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, BlockEntity be)
	{
		PlanetBlockEntity pbe = (PlanetBlockEntity) be;

		if(pbe.planetProps == null)
		{
			return;
		}

		Quaternion rotation = pbe.planetProps.getRotation();
		pbe.planetProps.getLastRotation().set(rotation.getX(), rotation.getY(), rotation.getZ(), rotation.getW());

		float rot = world.getTime() / 600f;
		Quaternion q = Vec3f.POSITIVE_X.getRadialQuaternion(rot);
		q.hamiltonProduct(Vec3f.POSITIVE_Y.getRadialQuaternion(rot));
		q.hamiltonProduct(Vec3f.POSITIVE_Z.getRadialQuaternion(rot));
		rotation.set(q.getX(), q.getY(), q.getZ(), q.getW());
	}
}