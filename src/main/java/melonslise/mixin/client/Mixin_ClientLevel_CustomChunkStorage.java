package melonslise.mixin.client;

import melonslise.immptl.common.world.chunk.VersatileClientChunkCache;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public class Mixin_ClientLevel_CustomChunkStorage {

    @Mutable
    @Shadow
    @Final
    private ClientChunkCache chunkSource;

    @Inject(
            method = "<init>",
            at = @At("RETURN")
    )
    void onCreation(ClientPacketListener listener, ClientLevel.ClientLevelData levelData,
                    ResourceKey<Level> dimension, DimensionType dimensionType,
                    int a, Supplier<ProfilerFiller> profilerFillerSupplier, LevelRenderer levelRenderer,
                    boolean b, long c, CallbackInfo callbackInfo)
    {
        this.chunkSource = new VersatileClientChunkCache((ClientLevel) (Object) this, a);
    }
}
