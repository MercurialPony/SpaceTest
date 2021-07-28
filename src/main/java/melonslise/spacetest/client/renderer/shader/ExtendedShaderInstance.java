package melonslise.spacetest.client.renderer.shader;

import java.io.IOException;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Vector3f;

import melonslise.spacetest.client.util.UniformExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;

public class ExtendedShaderInstance extends ShaderInstance
{
	public final UniformExtension uniforms = new UniformExtension();

	public ExtendedShaderInstance(ResourceProvider provider, String name, VertexFormat format) throws IOException
	{
		super(provider, name, format);
	}

	public void setUniform(String name, Object value)
	{
		this.uniforms.setUniform(name, value);
	}

	@Override
	public void apply()
	{
		this.safeGetUniform("CameraPosition").set(new Vector3f(Minecraft.getInstance().gameRenderer.getMainCamera().getPosition()));
		this.uniforms.upload(this);
		super.apply();
	}
}