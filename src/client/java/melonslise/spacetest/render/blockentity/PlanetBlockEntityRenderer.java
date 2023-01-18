package melonslise.spacetest.render.blockentity;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import melonslise.spacetest.render.planet.PlanetRenderer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class PlanetBlockEntityRenderer implements BlockEntityRenderer<PlanetBlockEntity>
{
	private Map<PlanetBlockEntity, PlanetRenderer> testThingy = new HashMap<>();

	public PlanetBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

	@Override
	public void render(PlanetBlockEntity be, float frameDelta, MatrixStack mtx, VertexConsumerProvider vertexConsumers, int light, int overlay)
	{
		PlanetRenderer pr = this.testThingy.get(be);

		if(pr == null)
		{
			if(be.planetProps != null)
			{
				pr = new PlanetRenderer();
				this.testThingy.put(be, pr);
				pr.init(World.OVERWORLD, be.planetProps);
			}

			return;
		};

		mtx.push();
		mtx.translate(0.5d, 0.5d, 0.5d);
		pr.render(mtx, frameDelta);
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