package melonslise.spacetest.client.render;

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
import net.minecraft.util.math.Vec3f;

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

	public void update(float delta)
	{
		MinecraftClient client = MinecraftClient.getInstance();

		float f = this.world.getStarBrightness(1f);

		float darknessScale = client.options.getDarknessEffectScale().getValue().floatValue();
		float i = getDarknessFactor(delta) * darknessScale;
		float j = getDarkness(client.player, i, delta) * darknessScale;

		float l = 0f;

		if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION))
		{
			l = GameRenderer.getNightVisionStrength(client.player, delta);
		}
		else if (client.player.hasStatusEffect(StatusEffects.CONDUIT_POWER))
		{
			l = client.player.getUnderwaterVisibility();
		}

		Vec3f light = new Vec3f();

		for (int y = 0; y < 16; ++y)
		{
			for (int x = 0; x < 16; ++x)
			{
				float ambientLight = this.world.getDimension().ambientLight();

				float p = getBrightness(ambientLight, y) * (this.world.getLightningTicksLeft() > 0 ? 1f : f * 0.95f + 0.05f);
				float q = getBrightness(ambientLight, x) * (this.flickerIntensity + 1.5f);

				float s = q * ((q * 0.6f + 0.4f) * 0.6f + 0.4f);
				float t = q * (q * q * 0.6f + 0.4f);
				light.set(q, s, t);

				if (this.world.getDimensionEffects().shouldBrightenLighting())
				{
					light.lerp(new Vec3f(0.99f, 1.12f, 1f), 0.25f);
					light.clamp(0f, 1f);
				}
				else
				{
					Vec3f v = new Vec3f(f, f, 1f);
					v.lerp(new Vec3f(1f, 1f, 1f), 0.35f);
					v.scale(p);

					light.add(v);
					light.lerp(new Vec3f(0.75f, 0.75f, 0.75f), 0.04f);

					float skyDarkness = client.gameRenderer.getSkyDarkness(delta);

					if (skyDarkness > 0f)
					{
						Vec3f v1 = light.copy();
						v1.multiplyComponentwise(0.7f, 0.6f, 0.6f);
						light.lerp(v1, skyDarkness);
					}
				}

				if (l > 0f)
				{
					float v = Math.max(light.getX(), Math.max(light.getY(), light.getZ()));

					if (v < 1.0F)
					{
						float u = 1.0F / v;
						Vec3f vec3f4 = light.copy();
						vec3f4.scale(u);
						light.lerp(vec3f4, l);
					}
				}

				if (!this.world.getDimensionEffects().shouldBrightenLighting())
				{
					if (j > 0f)
					{
						light.add(-j, -j, -j);
					}

					light.clamp(0f, 1f);
				}

				float gamma = client.options.getGamma().getValue().floatValue();
				Vec3f v = light.copy();
				v.modify(LightmapTexture::easeOutQuart);
				light.lerp(v, Math.max(0f, gamma - i));
				light.lerp(new Vec3f(0.75f, 0.75f, 0.75f), 0.04f);
				light.clamp(0f, 1f);
				light.scale(255f);

				int xx = (int) light.getX();
				int yy = (int) light.getY();
				int zz = (int) light.getZ();

				this.texture.getImage().setColor(x, y, 0xFF000000 | zz << 16 | yy << 8 | xx);
			}
		}

		this.texture.upload();
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