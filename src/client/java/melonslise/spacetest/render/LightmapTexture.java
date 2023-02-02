package melonslise.spacetest.render;

import com.mojang.blaze3d.platform.GlConst;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

// FIXME will cause issues with mods that mixin to LightmapTextureManager though
// this is essentially LightmapTextureManager copy-pasted and modified to work with any world
@Environment(EnvType.CLIENT)
public class LightmapTexture implements AutoCloseable
{
	public ClientWorld world;

	public NativeImageBackedTexture texture;
	public Identifier id;

	public float flickerIntensity;

	public LightmapTexture(ClientWorld world)
	{
		this.world = world;
		this.texture = new NativeImageBackedTexture(16, 16, false);
		this.id = MinecraftClient.getInstance().getTextureManager().registerDynamicTexture("light_map", this.texture);

		for (int x = 0; x < 16; ++x)
		{
			for (int y = 0; y < 16; ++y)
			{
				this.texture.getImage().setColor(x, y, -1);
			}
		}

		this.texture.upload();
	}

	@Override
	public void close() throws Exception
	{
		this.texture.close();
	}

	public void enable()
	{
		RenderSystem.setShaderTexture(2, this.id);
		MinecraftClient.getInstance().getTextureManager().bindTexture(this.id);
		RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_LINEAR);
		RenderSystem.texParameter(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_LINEAR);
		RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
	}

	public void disable()
	{
		RenderSystem.setShaderTexture(2, 0);
	}

	public void tick()
	{
		this.flickerIntensity += (float) ((Math.random() - Math.random()) * Math.random() * Math.random() * 0.1f);
		this.flickerIntensity *= 0.9f;
	}

	// FIXME this has an insane amount of object creation!!!
	public void update(float delta)
	{
		MinecraftClient client = MinecraftClient.getInstance();

		float starBrightness = this.world.getStarBrightness(1f);

		float darknessScale = client.options.getDarknessEffectScale().getValue().floatValue();
		float darknessFactor = getDarknessFactor(delta) * darknessScale;
		float darkness = getDarkness(client.player, darknessFactor, delta) * darknessScale;

		float nightVisionStrength = 0.0f;

		if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
		{
			nightVisionStrength = GameRenderer.getNightVisionStrength(client.player, delta);
		}
		else if (client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER))
		{
			nightVisionStrength = client.player.getUnderwaterVisibility();
		}

		Vector3f light = new Vector3f();

		for (int y = 0; y < 16; ++y)
		{
			for (int x = 0; x < 16; ++x)
			{
				float ambientLight = this.world.getDimension().ambientLight();

				float p = getBrightness(ambientLight, y) * (this.world.getLightningTicksLeft() > 0 ? 1.0f : starBrightness * 0.95f + 0.05f);
				float q = getBrightness(ambientLight, x) * (this.flickerIntensity + 1.5f);

				float s = q * ((q * 0.6f + 0.4f) * 0.6f + 0.4f);
				float t = q * (q * q * 0.6f + 0.4f);
				light.set(q, s, t);

				if (this.world.getDimensionEffects().shouldBrightenLighting())
				{
					clamp(light.lerp(new Vector3f(0.99f, 1.12f, 1.0f), 0.25f));
				}
				else
				{
					light.add(new Vector3f(starBrightness, starBrightness, 1.0f).lerp(new Vector3f(1.0f, 1.0f, 1.0f), 0.35f).mul(p));
					light.lerp(new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);

					float skyDarkness = client.gameRenderer.getSkyDarkness(delta);

					if (skyDarkness > 0.0f)
					{
						light.lerp(new Vector3f(light).mul(0.7f, 0.6f, 0.6f), skyDarkness);
					}
				}

				if (nightVisionStrength > 0.0f)
				{
					float v = Math.max(light.x, Math.max(light.y, light.z));

					if (v < 1.0f)
					{
						light.lerp(new Vector3f(light).mul(1.0f / v), nightVisionStrength);
					}
				}

				if (!this.world.getDimensionEffects().shouldBrightenLighting())
				{
					if (darkness > 0f)
					{
						light.add(-darkness, -darkness, -darkness);
					}

					clamp(light);
				}

				float gamma = client.options.getGamma().getValue().floatValue();
				light.lerp(new Vector3f(easeOutQuart(light.x), easeOutQuart(light.y), easeOutQuart(light.z)), Math.max(0f, gamma - darknessFactor));
				light.lerp(new Vector3f(0.75f, 0.75f, 0.75f), 0.04f);
				clamp(light);
				light.mul(255.0f);

				int xx = (int) light.x;
				int yy = (int) light.y;
				int zz = (int) light.z;

				this.texture.getImage().setColor(x, y, 0xFF000000 | zz << 16 | yy << 8 | xx);
			}
		}

		this.texture.upload();
	}

	private static void clamp(Vector3f vec)
	{
		vec.set(MathHelper.clamp(vec.x, 0.0f, 1.0f), MathHelper.clamp(vec.y, 0.0f, 1.0f), MathHelper.clamp(vec.z, 0.0f, 1.0f));
	}

	public static float getDarknessFactor(float delta)
	{
		ClientPlayerEntity player = MinecraftClient.getInstance().player;

		if (!player.hasStatusEffect(StatusEffects.DARKNESS))
		{
			return 0f;
		}

		/*
		if (statusEffectInstance == null || !statusEffectInstance.getFactorCalculationData().isPresent())
		{
			return 0f;
		}
		*/

		return player.getStatusEffect(StatusEffects.DARKNESS).getFactorCalculationData().map(data -> data.lerp(player, delta)).orElse(0f);
	}

	public static float getDarkness(LivingEntity entity, float factor, float delta)
	{
		float f = 0.45f * factor;
		return Math.max(0f, MathHelper.cos((entity.age - delta) * (float) Math.PI * 0.025f) * f);
	}

	public static float getBrightness(float ambientLight, int lightLevel)
	{
		float f = lightLevel / 15f;
		float g = f / (4f - 3f * f);
		return MathHelper.lerp(ambientLight, g, 1f);
	}

	public static float easeOutQuart(float x)
	{
		float f = 1f - x;
		return 1f - f * f * f * f;
	}
}