package melonslise.spacetest.client.render.blockentity;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import melonslise.spacetest.client.render.PlanetRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

// FIXME test chunk updating when placing blocks and stuff in multiplayer
// FIXME apply spherical frustum culling
@Environment(EnvType.CLIENT)
public class PlanetBlockEntityRenderer implements BlockEntityRenderer<PlanetBlockEntity>
{
	public PlanetBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

	@Override
	public void render(PlanetBlockEntity be, float frameDelta, MatrixStack mtx, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		BlockPos pos = be.getPos();

		if(be.pr == null)
		{
			be.pr = new PlanetRenderer();
			be.pr.init(World.OVERWORLD, new ChunkPos(pos), 10, 10);
		}

		be.pr.render(mtx, frameDelta);
	}

	@Override
	public boolean rendersOutsideBoundingBox(PlanetBlockEntity be)
	{
		return true;
	}

	@Override
	public int getRenderDistance()
	{
		return 512;
	}
}