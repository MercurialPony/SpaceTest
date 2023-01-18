package melonslise.spacetest.blockentity;

import melonslise.spacetest.init.StBlockEntities;
import melonslise.spacetest.planet.BasicPlanetProperties;
import melonslise.spacetest.planet.PlanetProperties;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.world.World;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;

public class PlanetBlockEntity extends BlockEntity
{
	public PlanetProperties planetProps;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(StBlockEntities.PLANET, pos, state);
	}

	public void placed(BlockPos pos, LivingEntity placer)
	{
		this.planetProps = new BasicPlanetProperties(new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d), ChunkSectionPos.from(pos.withY(placer.world.getBottomY())), 5, 62 - placer.world.getBottomY());

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

		Quaternionf rotation = pbe.planetProps.getRotation();
		pbe.planetProps.getLastRotation().set(rotation);

		float angle = world.getTime() / 600.0f;

		rotation.set(new Quaterniond(
			RotationAxis.POSITIVE_X.rotation(angle)
			.mul(RotationAxis.POSITIVE_Y.rotation(angle))
			.mul(RotationAxis.POSITIVE_Z.rotation(angle))));
	}
}