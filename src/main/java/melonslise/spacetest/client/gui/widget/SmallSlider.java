package melonslise.spacetest.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fmlclient.gui.GuiUtils;
import net.minecraftforge.fmlclient.gui.widget.Slider;

public class SmallSlider extends Slider implements FloatConsumer
{
	public float scale = 1f;

	public SmallSlider(int x, int y, int width, int height, Component prefix, Component suffix, double min, double max, double def, ISlider responder)
	{
		super(x, y, width, height, prefix, suffix, min, max, def, true, true, null, responder);
	}

	public void setScale(float scale)
	{
		this.scale = scale;
	}

	@Override
	public void accept(float f)
	{
		super.setValue(f);
		this.updateSlider();
	}

	@Override
	public void renderButton(PoseStack mtx, int mouseX, int mouseY, float frameTime)
	{
		if (!this.visible)
			return;
		Minecraft mc = Minecraft.getInstance();
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderTexture(0, WIDGETS_LOCATION);
		RenderSystem.setShaderColor(1f, 1f, 1f, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.enableDepthTest();
		this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
		int k = this.getYImage(this.isHovered());
		GuiUtils.drawContinuousTexturedBox(mtx, WIDGETS_LOCATION, this.x, this.y, 0, 46 + k * 20, this.width, this.height, 200, 20, 2, 3, 2, 2, this.getBlitOffset());
		this.renderBg(mtx, mc, mouseX, mouseY);

		float invScale = 1f / this.scale;

		Component buttonText = this.getMessage();
		int strWidth = (int) (mc.font.width(buttonText) * this.scale);
		int ellipsisWidth = (int) (mc.font.width("...") * this.scale);

		if (strWidth > width - 6 && strWidth > ellipsisWidth)
			buttonText = new TextComponent(mc.font.substrByWidth(buttonText, width - 6 - ellipsisWidth).getString() + "...");

		mtx.pushPose();
		mtx.scale(this.scale, this.scale, this.scale);
		drawCenteredString(mtx, mc.font, buttonText, (int) ((this.x + this.width / 2) * invScale), (int) ((this.y + (this.height - mc.font.lineHeight * this.scale) / 2) * invScale), getFGColor());
		mtx.popPose();
	}
}