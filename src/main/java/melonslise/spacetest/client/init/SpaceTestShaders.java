package melonslise.spacetest.client.init;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.client.renderer.shader.ExtendedShaderInstance;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.VanillaResourceType;

@OnlyIn(Dist.CLIENT)
public final class SpaceTestShaders implements ResourceManagerReloadListener
{
	public static final RenderStateShard.ShaderStateShard BLACK_HOLE_SHADER_STATE = new RenderStateShard.ShaderStateShard(SpaceTestShaders::getBlackHoleShader);

	public static final List<ShaderInstance> SHADERS = new ArrayList<>(1);

	public static ExtendedShaderInstance blackHoleShader;

	@Override
	public void onResourceManagerReload(ResourceManager mgr)
	{
		SHADERS.forEach(ShaderInstance::close);
		SHADERS.clear();

		try
		{
			SHADERS.add(blackHoleShader = new ExtendedShaderInstance(mgr, SpaceTest.ID + "/black_hole", DefaultVertexFormat.POSITION));
		}
		catch (IOException e)
		{
			SHADERS.forEach(ShaderInstance::close);
			e.printStackTrace();
		}
	}

	@Override
	public IResourceType getResourceType()
	{
		return VanillaResourceType.SHADERS;
	}

	public static ShaderInstance getBlackHoleShader()
	{
		return blackHoleShader;
	}
}