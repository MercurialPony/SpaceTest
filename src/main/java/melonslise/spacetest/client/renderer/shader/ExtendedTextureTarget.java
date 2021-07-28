package melonslise.spacetest.client.renderer.shader;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;

public class ExtendedTextureTarget extends TextureTarget
{
	public ExtendedTextureTarget(int width, int height, boolean p_166215_, boolean p_166216_)
	{
		super(width, height, p_166215_, p_166216_);
	}

	public ExtendedTextureTarget(RenderTarget target)
	{
		super(target.width, target.height, false, Minecraft.ON_OSX);
	}

	public void copyColor(RenderTarget target)
	{
		GlStateManager.getBoundFramebuffer();
		GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, target.frameBufferId);
		GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.frameBufferId);
		GlStateManager._glBlitFrameBuffer(0, 0, target.width, target.height, 0, 0, this.width, this.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
		GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}
}