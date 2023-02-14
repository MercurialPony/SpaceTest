package melonslise.spacetest.init;

import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.test.blockentity.blockentity.PlanetBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class StBlockEntities
{
	private StBlockEntities() {}

	public static final BlockEntityType<PlanetBlockEntity> PLANET = register("planet", PlanetBlockEntity::new, StBlocks.PLANET);

	private static <T extends BlockEntity> BlockEntityType<T> register(String name, BlockEntityType.BlockEntityFactory<T> factory, Block... blocks)
	{
		return Registry.register(Registries.BLOCK_ENTITY_TYPE, SpaceTestCore.id(name), FabricBlockEntityTypeBuilder.create(factory::create, blocks).build());
	}

	public static void register()
	{
		// NO OP
	}
}