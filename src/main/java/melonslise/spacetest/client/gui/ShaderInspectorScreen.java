package melonslise.spacetest.client.gui;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import melonslise.spacetest.client.gui.widget.SmallLabelledEditBox;
import melonslise.spacetest.client.gui.widget.SmallSlider;
import melonslise.spacetest.client.renderer.shader.ExtendedUniform;
import melonslise.spacetest.client.renderer.shader.IShader;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class ShaderInspectorScreen extends Screen
{
	public static final Component
		TITLE = new TextComponent(""),
		NEXT_PAGE_TXT = new TextComponent(">"),
		PREV_PAGE_TXT = new TextComponent("<"),
		RESET_TXT = new TextComponent("x");

	public static final String[] COMPONENTS = new String[] { "x", "y", "z", "w" };

	public static final int WIDGETS_PER_PAGE = 10;

	public static final int WINDOW_WIDTH = 110, WINDOW_HEIGHT = 220, BORDER_HEIGHT = 7, BORDER_WIDTH = 2;

	public final List<IShader> shaders;

	public static float windowCornerX, windowCornerY;
	public static IShader currentShader;
	public static int currentPage;

	public boolean draggingWindow;

	public CycleButton<IShader> shaderButton;
	public Button prevPageButton, nextPageButton;

	public List<List<Pair<AbstractWidget, AbstractWidget>>> widgetPages;

	public ShaderInspectorScreen(List<IShader> shaders)
	{
		super(TITLE);
		this.shaders = shaders;
	}

	@Override
	public boolean isPauseScreen()
	{
		return false;
	}

	// FIXME crash on reload shaders
	@Override
	protected void init()
	{
		this.shaderButton = this.addRenderableWidget(CycleButton.builder(ShaderInspectorScreen::getShaderName)
			.withValues(this.shaders)
			.withInitialValue(currentShader != null ? currentShader : this.shaders.get(0))
			.displayOnlyValue()
			.create(0, 0, 90, 20, TITLE, this::switchShader));
		// FIXME smaller
		this.prevPageButton = this.addRenderableWidget(new Button(0, 0, 20, 20, PREV_PAGE_TXT, this::prevPage));
		this.nextPageButton = this.addRenderableWidget(new Button(0, 0, 20, 20, NEXT_PAGE_TXT, this::nextPage));
		this.initWidgetPages(this.shaderButton.getValue());
		this.addPage(currentPage);
		super.init();
	}

	public void initWidgetPages(IShader shader)
	{
		List<Uniform> uniforms = shader.getUniforms();
		this.widgetPages = new ArrayList<>(uniforms.size());
		int count = 0;
		for(Uniform uniform : uniforms)
		{
			if(uniform.getCount() > 4) // exclude matrices
				continue;
			for(int i = 0; i < uniform.getCount(); ++i)
			{
				List<Pair<AbstractWidget, AbstractWidget>> widgetPage;
				int page = count / WIDGETS_PER_PAGE;
				if(count % WIDGETS_PER_PAGE == 0)
				{
					widgetPage = new ArrayList<>(WIDGETS_PER_PAGE);
					this.widgetPages.add(widgetPage);
				}
				else
					widgetPage = this.widgetPages.get(page);
				++count;

				if(uniform instanceof ExtendedUniform exUniform)
				{
					FloatConsumer inputWidget = this.isSliderUniform(exUniform) ? this.uniformSlider(exUniform, i) : this.uniformBox(uniform, i);
					widgetPage.add(Pair.of((AbstractWidget) inputWidget, this.resetButton(exUniform, inputWidget, i)));
				}
				else
					widgetPage.add(Pair.of(this.uniformBox(uniform, i), this.resetButton(uniform)));
			}
		}
		this.updatePage(currentPage, (int) windowCornerX, (int) windowCornerY);
	}

	public float getUniform(Uniform u, int i)
	{
		return u.getFloatBuffer().get(i);
	}

	public boolean isSliderUniform(ExtendedUniform u)
	{
		return u.min != Float.MIN_VALUE && u.max != Float.MAX_VALUE;
	}

	public String getUniformLabel(Uniform u, int i)
	{
		String label = u.getName();
		if(u.getCount() >1)
			label += "." + COMPONENTS[i];
		label += ": ";
		return label;
	}

	public SmallLabelledEditBox uniformBox(Uniform u, int i)
	{
		SmallLabelledEditBox box = new SmallLabelledEditBox(this.font, 0, 0, 35, 8, TITLE);
		box.setScale(0.5f);
		box.setLabel(this.getUniformLabel(u, i));
		box.setLabelOffset(62);
		box.setValue(Float.toString(this.getUniform(u, i)));
		box.setResponder(s ->
		{
			try
			{
				float f = Float.parseFloat(s);
				u.set(i, f);
			} catch(NumberFormatException e) {}
		});
		return box;
	}

	public SmallSlider uniformSlider(ExtendedUniform u, int i)
	{
		SmallSlider slider = new SmallSlider(0, 0, 92, 8, new TextComponent(this.getUniformLabel(u, i)), TITLE, u.min, u.max, this.getUniform(u, i), sl -> u.set(i, (float) sl.getValue()));
		slider.setScale(0.5f);
		return slider;
	}

	public Button resetButton(Uniform u)
	{
		Button button = new Button(0, 0, 8, 8, RESET_TXT, b -> {});
		button.active = false;
		return button;
	}

	public Button resetButton(ExtendedUniform u, FloatConsumer action, int i)
	{
		return new Button(0, 0, 8, 8, RESET_TXT, b -> action.accept(u.defaults[i]));
	}

	public void switchShader(CycleButton<IShader> button, IShader shader)
	{
		currentShader = shader;
		this.clearPage(currentPage);
		this.initWidgetPages(shader);
		currentPage = 0;
		this.addPage(currentPage);
		this.updatePage(currentPage, (int) windowCornerX, (int) windowCornerY);
	}

	public void switchPage(int page)
	{
		if(page < 0 || page >= this.widgetPages.size())
			return;
		this.clearPage(currentPage);
		this.addPage(page);
		this.updatePage(page, (int) windowCornerX, (int) windowCornerY);
		currentPage = page;
	}

	public void prevPage(Button button)
	{
		this.switchPage(currentPage - 1);
	}

	public void nextPage(Button button)
	{
		this.switchPage(currentPage + 1);
	}

	public void addPage(int page)
	{
		for(var widgetPair : this.widgetPages.get(page))
		{
			this.addRenderableWidget(widgetPair.getLeft());
			this.addRenderableWidget(widgetPair.getRight());
		}
	}

	public void updatePage(int page, int cornerX, int cornerY)
	{
		this.shaderButton.x = cornerX + 10;
		this.shaderButton.y = cornerY + 10;
		this.prevPageButton.x = cornerX + 10;
		this.prevPageButton.y = cornerY + WINDOW_HEIGHT - this.prevPageButton.getHeight() - 10;
		this.nextPageButton.x = cornerX + WINDOW_WIDTH - this.nextPageButton.getWidth() - 10;
		this.nextPageButton.y = cornerY + WINDOW_HEIGHT - this.nextPageButton.getHeight() - 10;
		this.prevPageButton.active = page > 0;
		this.nextPageButton.active = page < this.widgetPages.size() - 1;

		var widgetPage = this.widgetPages.get(page); 
		for(int i = 0; i < widgetPage.size(); ++i)
		{
			var widgetPair = widgetPage.get(i);
			AbstractWidget inputWidget = widgetPair.getLeft();
			inputWidget.x = cornerX;
			if(inputWidget instanceof SmallLabelledEditBox box)
				inputWidget.x += box.labelOffset;
			else
				inputWidget.x += 5;
			inputWidget.y = cornerY + 25 + 15 * (i + 1);

			AbstractWidget resetWidget = widgetPair.getRight();
			resetWidget.x = cornerX + WINDOW_WIDTH - 10;
			resetWidget.y = cornerY + 25 + 15 * (i + 1);
		}
	}

	public void tickPage(int page)
	{
		for(var widgetPair : this.widgetPages.get(page))
			if(widgetPair.getLeft() instanceof EditBox box)
				box.tick();
	}

	public void clearPage(int page)
	{
		for(var widgetPair : this.widgetPages.get(page))
		{
			this.removeWidget(widgetPair.getLeft());
			this.removeWidget(widgetPair.getRight());
		}
	}

	public boolean inWindow(double mouseX, double mouseY)
	{
		return mouseX > windowCornerX && mouseX < windowCornerX + WINDOW_WIDTH && mouseY > windowCornerY && mouseY < windowCornerY + WINDOW_HEIGHT;
	}

	public boolean inBorderOrWindow(double mouseX, double mouseY)
	{
		return mouseX > windowCornerX - BORDER_WIDTH && mouseX < windowCornerX + WINDOW_WIDTH + BORDER_WIDTH && mouseY > windowCornerY - BORDER_HEIGHT && mouseY < windowCornerY + WINDOW_HEIGHT + BORDER_WIDTH;
	}

	public boolean inBorder(double mouseX, double mouseY)
	{
		return !this.inWindow(mouseX, mouseY) && this.inBorderOrWindow(mouseX, mouseY);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int i)
	{
		if(this.inBorder(mouseX, mouseY))
			this.draggingWindow = true;
		return super.mouseClicked(mouseX, mouseY, i);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int i)
	{
		this.draggingWindow = false;
		return super.mouseReleased(mouseX, mouseY, i);
	}

	public static Component getShaderName(IShader shader)
	{
		return new TextComponent(shader.getName());
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int i, double deltaX, double deltaY)
	{
		if(this.draggingWindow)
		{
			windowCornerX += deltaX;
			windowCornerY += deltaY;
			this.updatePage(currentPage, (int) windowCornerX, (int) windowCornerY);
		}
		return super.mouseDragged(mouseX, mouseY, i, deltaX, deltaY);
	}

	@Override
	public void tick()
	{
		this.tickPage(currentPage);
		super.tick();
	}

	@Override
	public void render(PoseStack mtx, int mouseX, int mouseY, float frameTime)
	{
		fill(mtx, (int) windowCornerX, (int) windowCornerY + WINDOW_HEIGHT, (int) windowCornerX + WINDOW_WIDTH, (int) windowCornerY, this.minecraft.options.getBackgroundColor(Integer.MIN_VALUE));

		fill(mtx, (int) windowCornerX, (int) windowCornerY - BORDER_HEIGHT, (int) windowCornerX + WINDOW_WIDTH, (int) windowCornerY, 0x80FFFFFF);
		fill(mtx, (int) windowCornerX - BORDER_WIDTH, (int) windowCornerY + WINDOW_HEIGHT + BORDER_WIDTH, (int) windowCornerX, (int) windowCornerY - 7, 0x80FFFFFF);
		fill(mtx, (int) windowCornerX + WINDOW_WIDTH, (int) windowCornerY + WINDOW_HEIGHT + BORDER_WIDTH, (int) windowCornerX + WINDOW_WIDTH + BORDER_WIDTH, (int) windowCornerY - BORDER_HEIGHT, 0x80FFFFFF);
		fill(mtx, (int) windowCornerX, (int) windowCornerY + WINDOW_HEIGHT + BORDER_WIDTH, (int) windowCornerX + WINDOW_WIDTH, (int) windowCornerY + WINDOW_HEIGHT, 0x80FFFFFF);

		drawCenteredString(mtx, this.font, (currentPage + 1) + "/" + this.widgetPages.size(), (int) windowCornerX + WINDOW_WIDTH / 2, (int) windowCornerY + WINDOW_HEIGHT - 23, 0xFFFFFF);
		super.render(mtx, mouseX, mouseY, frameTime);
	}
}