package melonslise.spacetest.init;

import melonslise.spacetest.SpaceTestCore;
import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.chunk.ChunkBuilder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;

public final class StItems
{
	private StItems() {}

	public static final ItemGroup MAIN_ITEM_TAB = FabricItemGroupBuilder.build(SpaceTestCore.id("main"), () -> new ItemStack(Items.APPLE));

	public static final Item TEST_ITEM = register("test_item", new Item(new Item.Settings().group(MAIN_ITEM_TAB))
	{
		@Override
		public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand)
		{

			return super.use(world, user, hand);
		}
	});

	private static Item register(String name, Item item)
	{
		return Registry.register(Registry.ITEM, SpaceTestCore.id(name), item);
	}

	public static void register()
	{
		// NO OP
	}
}