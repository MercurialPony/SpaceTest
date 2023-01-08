package melonslise.spacetest.init;

import melonslise.spacetest.SpaceTestCore;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.registry.Registry;

public final class StItems
{
	private StItems() {}

	public static final ItemGroup MAIN_ITEM_TAB = FabricItemGroupBuilder.build(SpaceTestCore.id("main"), () -> new ItemStack(Items.APPLE));

	private static Item register(String name, Item item)
	{
		return Registry.register(Registry.ITEM, SpaceTestCore.id(name), item);
	}

	public static void register()
	{
		// NO OP
	}
}