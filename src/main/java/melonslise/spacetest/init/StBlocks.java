package melonslise.spacetest.init;

import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.block.PlanetBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.item.BlockItem;
import net.minecraft.util.registry.Registry;

public final class StBlocks
{
	private StBlocks() {}

	public static final Block PLANET = register("planet", new PlanetBlock<>(() -> StBlockEntities.PLANET, FabricBlockSettings.of(Material.STONE)));

	private static Block register(String name, Block block)
	{
		Registry.register(Registry.ITEM, SpaceTestCore.id(name), new BlockItem(block, new FabricItemSettings().group(StItems.MAIN_ITEM_TAB)));
		return Registry.register(Registry.BLOCK, SpaceTestCore.id(name), block);
	}

	public static void register()
	{
		// NO OP
	}
}