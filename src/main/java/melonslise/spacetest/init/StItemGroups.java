package melonslise.spacetest.init;

import melonslise.spacetest.SpaceTestCore;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

public final class StItemGroups
{
	public static final ItemGroup MAIN_ITEM_GROUP = FabricItemGroup.builder(SpaceTestCore.id("main"))
			.displayName(Text.translatable("itemGroup.main"))
			.icon(Items.APPLE::getDefaultStack)
			.entries(((enabledFeatures, entries, operatorEnabled) -> {
				entries.add(StBlocks.PLANET);
			}))
			.build();

	public static void register()
	{
		// NO OP
	}
}