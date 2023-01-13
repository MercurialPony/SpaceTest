package melonslise.spacetest.client.init;

import ladysnake.satin.api.managed.ManagedCoreShader;
import ladysnake.satin.api.managed.ShaderEffectManager;
import melonslise.spacetest.SpaceTestCore;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexFormats;

@Environment(EnvType.CLIENT)
public final class StShaders
{
	private StShaders() {}

	public static final ManagedCoreShader PLANET_SOLID = ShaderEffectManager.getInstance().manageCoreShader(SpaceTestCore.id("planet_solid"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
	public static final ManagedCoreShader PLANET_CUTOUT = ShaderEffectManager.getInstance().manageCoreShader(SpaceTestCore.id("planet_cutout"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);
	public static final ManagedCoreShader PLANET_TRANSLUCENT = ShaderEffectManager.getInstance().manageCoreShader(SpaceTestCore.id("planet_translucent"), VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

	public static final ManagedCoreShader[] PLANET_SHADERS = new ManagedCoreShader[] { PLANET_SOLID, PLANET_CUTOUT, PLANET_TRANSLUCENT };

	public static void register()
	{
		// NO OP
	}
}