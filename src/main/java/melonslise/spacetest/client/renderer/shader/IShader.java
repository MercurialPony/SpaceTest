package melonslise.spacetest.client.renderer.shader;

import com.mojang.blaze3d.shaders.Uniform;

import java.util.List;

public interface IShader
{
	String getName();

	List<Uniform> getUniforms();

	void close();
}