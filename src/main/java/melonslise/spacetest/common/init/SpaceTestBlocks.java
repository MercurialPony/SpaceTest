package melonslise.spacetest.common.init;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.block.BlockWithEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class SpaceTestBlocks
{
	public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SpaceTest.ID);

	private SpaceTestBlocks() {}

	public static final RegistryObject<Block>
		BLACK_HOLE = add("black_hole", new BlockWithEntity(SpaceTestBlockEntities.BLACK_HOLE, BlockBehaviour.Properties.of(Material.STONE))),
		PLANET = add("planet", new BlockWithEntity(SpaceTestBlockEntities.PLANET, BlockBehaviour.Properties.of(Material.STONE)));

	public static RegistryObject<Block> add(String name, Block block)
	{
		SpaceTestItems.add(name, () -> new BlockItem(block, new Item.Properties().tab(SpaceTestItems.MAIN_TAB)));
		return BLOCKS.register(name, () -> block);
	}
}