package melonslise.spacetest.client.init;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.client.renderer.shader.ExtendedPostChain;
import melonslise.spacetest.client.renderer.shader.ExtendedShaderInstance;
import melonslise.spacetest.client.renderer.shader.IShader;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.resource.IResourceType;
import net.minecraftforge.resource.VanillaResourceType;

@OnlyIn(Dist.CLIENT)
public final class SpaceTestShaders implements ResourceManagerReloadListener
{
	protected static final List<IShader> SHADERS = new ArrayList<>(6);

	protected static ExtendedShaderInstance
		blackHole,
		solidPlanet,
		cutoutPlanet,
		translucentPlanet,
		spaceSky;

	protected static ExtendedPostChain
		atmosphere;

	public static void init(ResourceManager mgr) throws IOException
	{
		Minecraft mc = Minecraft.getInstance();

		blackHole = add(new ExtendedShaderInstance(SpaceTest.ID, "black_hole", DefaultVertexFormat.POSITION));
		solidPlanet = add(new ExtendedShaderInstance(SpaceTest.ID, "solid_planet", DefaultVertexFormat.BLOCK));
		cutoutPlanet = add(new ExtendedShaderInstance(SpaceTest.ID, "cutout_planet", DefaultVertexFormat.BLOCK));
		translucentPlanet = add(new ExtendedShaderInstance(SpaceTest.ID, "translucent_planet", DefaultVertexFormat.BLOCK));
		spaceSky = add(new ExtendedShaderInstance(SpaceTest.ID, "space_sky", DefaultVertexFormat.POSITION_TEX));

		atmosphere = add(new ExtendedPostChain(SpaceTest.ID, "atmosphere"));
	}

	public static ExtendedShaderInstance add(ExtendedShaderInstance shader)
	{
		SHADERS.add(shader);
		return shader;
	}

	public static ExtendedPostChain add(ExtendedPostChain shader)
	{
		SHADERS.add(shader);
		return shader;
	}

	public void clear()
	{
		SHADERS.forEach(IShader::close);
		SHADERS.clear();
	}

	@Override
	public void onResourceManagerReload(ResourceManager mgr)
	{
		this.clear();
		try
		{
			init(mgr);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	/*
	public static ExtendedPostChain loadPostShader(String name)
	{
		try
		{
			Minecraft mc = Minecraft.getInstance();
			ExtendedPostChain shader = new ExtendedPostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), new ResourceLocation(SpaceTest.ID, "shaders/post/" + name + ".json"));
			shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
			return shader;
		}
		catch (IOException | JsonSyntaxException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	*/

	@Override
	public IResourceType getResourceType()
	{
		return VanillaResourceType.SHADERS;
	}

	public static ExtendedShaderInstance getBlackHole()
	{
		return blackHole;
	}

	public static ExtendedShaderInstance getSolidPlanet()
	{
		return solidPlanet;
	}

	public static ExtendedShaderInstance getCutoutPlanet()
	{
		return cutoutPlanet;
	}

	public static ExtendedShaderInstance getTranslucentPlanet()
	{
		return translucentPlanet;
	}

	public static ExtendedShaderInstance getSpaceSky()
	{
		return spaceSky;
	}

	public static ExtendedPostChain getAtmosphere()
	{
		return atmosphere;
	}
}