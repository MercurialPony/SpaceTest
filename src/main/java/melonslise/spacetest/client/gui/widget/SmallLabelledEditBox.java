package melonslise.spacetest.client.gui.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;

public class SmallLabelledEditBox extends EditBox implements FloatConsumer
{
	public float scale = 1f;
	public String label = "";
	public int labelOffset = 0;

	public SmallLabelledEditBox(Font font, int x, int y, int width, int height, Component title)
	{
		super(font, x, y, width, height, title);
	}

	public void setScale(float scale)
	{
		this.scale = scale;
	}

	public void setLabel(String label)
	{
		this.label = label;
	}

	public void setLabelOffset(int offset)
	{
		this.labelOffset = offset;
	}

	@Override
	public void accept(float f)
	{
		super.setValue(Float.toString(f));
	}

	@Override
	public int getInnerWidth()
	{
		return super.getInnerWidth() + 23;
	}

	@Override
	public void renderButton(PoseStack mtx, int i1, int i2, float frameTime)
	{
		if (!this.isVisible())
			return;

		if (this.isBordered())
		{
			int i = this.isFocused() ? -1 : -6250336;
			fill(mtx, this.x - 0.5f, this.y - 0.5f, this.x + this.width + 0.5f, this.y + this.height + 0.5f, i);
			fill(mtx, this.x, this.y, this.x + this.width, this.y + this.height, -16777216);
		}

		int color = this.isEditable() ? this.textColor : this.textColorUneditable;
		int j = this.getCursorPosition() - this.displayPos;
		int k = this.highlightPos - this.displayPos;
		String s = this.font.plainSubstrByWidth(this.getValue().substring(this.displayPos), this.getInnerWidth());
		boolean flag = j >= 0 && j <= s.length();
		boolean flag1 = this.isFocused() && this.frame / 6 % 2 == 0 && flag;
		int x = this.isBordered() ? this.x + 4 : this.x;
		int y = this.isBordered() ? this.y + (this.height - (int) (this.font.lineHeight * this.scale)) / 2 : this.y;
		int x2 = x;
		if (k > s.length())
			k = s.length();

		float invScale = 1f / this.scale;

		mtx.pushPose();
		mtx.scale(this.scale, this.scale, this.scale);

		// Draw label
		if(!this.label.isEmpty())
			this.font.drawShadow(mtx, this.label, (x - this.labelOffset) * invScale, y * invScale, color);

		if (!s.isEmpty())
		{
			String s1 = flag ? s.substring(0, j) : s;
			x2 = this.font.drawShadow(mtx, this.formatter.apply(s1, this.displayPos), x * invScale, y * invScale, color);
		}

		boolean flag2 = this.getCursorPosition() < this.getValue().length() || this.getValue().length() >= this.getMaxLength();
		int x3 = x2;
		if (!flag)
		{
			x3 = j > 0 ? x + this.width : x;
		}
		else if (flag2)
		{
			x3 = x2 - 1;
			--x2;
		}

		if (!s.isEmpty() && flag && j < s.length())
			this.font.drawShadow(mtx, this.formatter.apply(s.substring(j), this.getCursorPosition()), x2, y * invScale, color);

		if (!flag2 && this.suggestion != null)
			this.font.drawShadow(mtx, this.suggestion, x3 - 1f, y * invScale, -8355712);

		if (flag1)
		{
			if (flag2)
				GuiComponent.fill(mtx, x3, (int) ((y  - 1) * invScale), x3 + 1,(int) ((y + 1 + (int) (this.font.lineHeight * this.scale)) * invScale), -3092272);
			else
				this.font.drawShadow(mtx, "_", x3, y * invScale, color);
		}

		// FIXME dafuq is this?
		if (k != j)
		{
			int l1 = x + this.font.width(s.substring(0, k));
			this.renderHighlight(x3, y - 1, l1 - 1, y + 1 + 9);
		}

		mtx.popPose();
	}

	public static void fill(PoseStack mtx, float x1, float y1, float x2, float y2, int color)
	{
		if (x1 < x2)
		{
			float t = x1;
			x1 = x2;
			x2 = t;
		}

		if (y1 < y2)
		{
			float t = y1;
			y1 = y2;
			y2 = t;
		}

		Matrix4f last = mtx.last().pose();

		float r = (color >> 16 & 255) / 255f;
		float g = (color >> 8 & 255) / 255f;
		float b = (color & 255) / 255f;
		float a = (color >> 24 & 255) / 255f;

		BufferBuilder bld = Tesselator.getInstance().getBuilder();
		RenderSystem.enableBlend();
		RenderSystem.disableTexture();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bld.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bld.vertex(last, x1, y2, 0f).color(r, g, b, a).endVertex();
		bld.vertex(last, x2, y2, 0f).color(r, g, b, a).endVertex();
		bld.vertex(last, x2, y1, 0f).color(r, g, b, a).endVertex();
		bld.vertex(last, x1, y1, 0f).color(r, g, b, a).endVertex();
		bld.end();
		BufferUploader.end(bld);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}
}