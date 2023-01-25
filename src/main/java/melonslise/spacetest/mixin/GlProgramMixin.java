package melonslise.spacetest.mixin;

import me.jellysquid.mods.sodium.client.gl.GlObject;
import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import org.lwjgl.opengl.GL20C;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.function.IntFunction;

@Mixin(GlProgram.class)
public abstract class GlProgramMixin
{
	/**
	 * @author
	 * @reason
	 */
	@Overwrite
	public <U extends GlUniform<?>> U bindUniform(String name, IntFunction<U> factory)
	{
		GlObject obj = (GlObject) (Object) this;

		int index = GL20C.glGetUniformLocation(obj.handle(), name);

		return factory.apply(index);
	}
}