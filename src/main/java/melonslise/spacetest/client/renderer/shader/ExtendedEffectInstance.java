package melonslise.spacetest.client.renderer.shader;

import com.google.gson.JsonElement;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.server.ChainedJsonException;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

public class ExtendedEffectInstance extends EffectInstance
{
	public ExtendedEffectInstance(ResourceManager mgr, String name) throws IOException
	{
		super(mgr, name);
	}

	@Override
	public void parseUniformNode(JsonElement uniformElement) throws ChainedJsonException
	{
		this.uniforms.add(ExtendedUniform.parse(uniformElement, this));
	}
}