package melonslise.spacetest.render.blockentity;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import melonslise.spacetest.render.PlanetWorldRenderer;
import melonslise.spacetest.render.planet.PlanetRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.World;
import qouteall.imm_ptl.core.ClientWorldLoader;

@Environment(EnvType.CLIENT)
public class PlanetBlockEntityRenderer implements BlockEntityRenderer<PlanetBlockEntity>
{
	public PlanetBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

	@Override
	public void render(PlanetBlockEntity be, float frameDelta, MatrixStack mtx, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		if(be.planetState != null && ClientWorldLoader.getWorldRenderer(World.OVERWORLD) instanceof PlanetWorldRenderer wr)
		{
			PlanetRenderer pr = wr.getPlanetRenderer();

			mtx.push();
			mtx.translate(0.5d, 0.5d, 0.5d);

			pr.render(be.planetState, mtx, frameDelta);

			mtx.pop();
		}
	}

	@Override
	public boolean rendersOutsideBoundingBox(PlanetBlockEntity be)
	{
		return true;
	}

	@Override
	public int getRenderDistance()
	{
		return 2048;
	}
}