package melonslise.spacetest.test.blockentity.blockentity;

import melonslise.spacetest.init.StBlockEntities;
import melonslise.spacetest.core.planets.BasicPlanetState;
import melonslise.spacetest.core.planets.PlanetProperties;
import melonslise.spacetest.core.planets.PlanetState;
import melonslise.spacetest.core.planets.world.PlanetWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
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
	public PlanetState planetState;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(StBlockEntities.PLANET, pos, state);
	}

	public void placed(BlockPos pos, LivingEntity placer)
	{
		this.planetState = new BasicPlanetState(new Vector3d(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d));

		if(placer instanceof ServerPlayerEntity player)
		{
			PlanetProperties props = ((PlanetWorld) player.world).getPlanetProperties();
			PortalAPI.addChunkLoaderForPlayer(player, new ChunkLoader(new DimensionalChunkPos(World.OVERWORLD, props.getOrigin().toChunkPos()), props.getFaceSize() * 2 + 1));
		}
	}

	public static void tick(World world, BlockPos pos, BlockState state, BlockEntity be)
	{
		PlanetBlockEntity pbe = (PlanetBlockEntity) be;

		if(pbe.planetState == null)
		{
			return;
		}

		Quaternionf rotation = pbe.planetState.getRotation();
		pbe.planetState.getLastRotation().set(rotation);

		float angle = world.getTime() / 1800.0f;

		rotation.set(new Quaterniond(
			RotationAxis.POSITIVE_X.rotation(angle)
			.mul(RotationAxis.POSITIVE_Y.rotation(angle))
			.mul(RotationAxis.POSITIVE_Z.rotation(angle))));
	}
}