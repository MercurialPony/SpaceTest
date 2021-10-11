package melonslise.spacetest.common.init;

import melonslise.spacetest.SpaceTest;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SpaceTestItems
{
	public static final CreativeModeTab MAIN_TAB = new CreativeModeTab(SpaceTest.ID)
	{
		@Override
		public ItemStack makeIcon()
		{
			return new ItemStack(Items.APPLE);
		}
	};

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SpaceTest.ID);

	private SpaceTestItems() {}

	public static RegistryObject<Item> add(String name, Supplier<Item> supplier)
	{
		return ITEMS.register(name, supplier);
	}
}