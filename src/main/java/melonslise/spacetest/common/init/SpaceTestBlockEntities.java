package melonslise.spacetest.common.init;

import java.util.function.Supplier;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.blockentity.TestBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class SpaceTestBlockEntities
{
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SpaceTest.ID);

	private SpaceTestBlockEntities() {}

	public static final RegistryObject<BlockEntityType<TestBlockEntity>> TEST = add("test", () -> type(TestBlockEntity::new, SpaceTestBlocks.TEST.get()));

	public static <T extends BlockEntity> BlockEntityType<T> type(BlockEntitySupplier<T> supplier, Block... blocks)
	{
		return BlockEntityType.Builder.of(supplier, blocks).build(null);
	}

	public static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> add(String name, Supplier<BlockEntityType<T>> supplier)
	{
		return BLOCK_ENTITIES.register(name, supplier);
	}
}