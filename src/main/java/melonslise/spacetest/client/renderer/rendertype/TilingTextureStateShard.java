package melonslise.spacetest.client.renderer.rendertype;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class TilingTextureStateShard extends TextureStateShard
{
	public TilingTextureStateShard(ResourceLocation texture, boolean blur, boolean mipmap)
	{
		super(texture, blur, mipmap);
		this.setupState = () ->
		{
			RenderSystem.enableTexture();
			TextureManager texMgr = Minecraft.getInstance().getTextureManager();
			texMgr.getTexture(texture).setFilter(blur, mipmap);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
			RenderSystem.setShaderTexture(0, texture);
		};
	}
}