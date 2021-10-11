package melonslise.spacetest.common.init;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.blockentity.BlackHoleBlockEntity;
import melonslise.spacetest.common.blockentity.PlanetBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.BlockEntitySupplier;
import net.minecraftforge.fmllegacy.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class SpaceTestBlockEntities
{
	public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITIES, SpaceTest.ID);

	private SpaceTestBlockEntities() {}

	public static final RegistryObject<BlockEntityType<BlackHoleBlockEntity>> BLACK_HOLE = add("black_hole", () -> type(BlackHoleBlockEntity::new, SpaceTestBlocks.BLACK_HOLE.get()));
	public static final RegistryObject<BlockEntityType<PlanetBlockEntity>> PLANET = add("planet", () -> type(PlanetBlockEntity::new, SpaceTestBlocks.PLANET.get()));

	public static <T extends BlockEntity> BlockEntityType<T> type(BlockEntitySupplier<T> supplier, Block... blocks)
	{
		return BlockEntityType.Builder.of(supplier, blocks).build(null);
	}

	public static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> add(String name, Supplier<BlockEntityType<T>> supplier)
	{
		return BLOCK_ENTITIES.register(name, supplier);
	}
}