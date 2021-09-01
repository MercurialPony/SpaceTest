package melonslise.spacetest.client.renderer.shader;

import java.util.List;

import com.mojang.blaze3d.shaders.Uniform;

public interface IShader
{
	String getName();

	List<Uniform> getUniforms();

	void close();
}