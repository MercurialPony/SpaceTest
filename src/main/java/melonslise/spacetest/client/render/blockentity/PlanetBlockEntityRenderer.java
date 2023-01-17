package melonslise.spacetest.client.render.blockentity;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import melonslise.spacetest.client.render.planet.PlanetRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
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
		if(be.pr == null)
		{
			if(be.planetProps != null)
			{
				be.pr = new PlanetRenderer();
				be.pr.init(World.OVERWORLD, be.planetProps);
			}

			return;
		};

		mtx.push();
		mtx.translate(0.5d, 0.5d, 0.5d);
		be.pr.render(mtx, frameDelta);
		mtx.pop();
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