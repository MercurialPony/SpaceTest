package melonslise.spacetest.client.renderer.shader;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.ResourceProvider;

public class ExtendedShaderInstance extends ShaderInstance implements IShader
{
	public ExtendedShaderInstance(ResourceProvider provider, ResourceLocation id, VertexFormat format) throws IOException
	{
		super(provider, id, format);
	}

	public ExtendedShaderInstance(String domain, String name, VertexFormat format) throws IOException
	{
		this(Minecraft.getInstance().getResourceManager(), new ResourceLocation(domain, name), format);
	}

	@Override
	public List<Uniform> getUniforms()
	{
		return this.uniforms;
	}

	@Override
	public void parseUniformNode(JsonElement uniformElement) throws ChainedJsonException
	{
		this.uniforms.add(ExtendedUniform.parse(uniformElement, this));
	}
}