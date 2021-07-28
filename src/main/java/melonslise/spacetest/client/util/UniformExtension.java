package melonslise.spacetest.client.util;

import java.util.HashMap;
import java.util.Map;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;

import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.ShaderInstance;

public class UniformExtension
{
	protected final Map<String, Object> uniforms = new HashMap<>(10);

	public void setUniform(String name, Object value)
	{
		this.uniforms.put(name, value);
	}

	public void upload(ShaderInstance shader)
	{
		for(var uniform : this.uniforms.entrySet())
		{
			Object value = uniform.getValue();
			if(value instanceof Float f)
				shader.safeGetUniform(uniform.getKey()).set(f);
			else if(value instanceof Vector3f vec)
				shader.safeGetUniform(uniform.getKey()).set(vec);
			else if(value instanceof Vector4f vec)
				shader.safeGetUniform(uniform.getKey()).set(vec);
			else if(value instanceof Matrix4f mat)
				shader.safeGetUniform(uniform.getKey()).set(mat);
		}
	}

	public void upload(EffectInstance shader)
	{
		for(var uniform : this.uniforms.entrySet())
		{
			Object value = uniform.getValue();
			if(value instanceof Float f)
				shader.safeGetUniform(uniform.getKey()).set(f);
			else if(value instanceof Vector3f vec)
				shader.safeGetUniform(uniform.getKey()).set(vec);
			else if(value instanceof Vector4f vec)
				shader.safeGetUniform(uniform.getKey()).set(vec);
			else if(value instanceof Matrix4f mat)
				shader.safeGetUniform(uniform.getKey()).set(mat);
		}
	}
}