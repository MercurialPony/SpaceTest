package melonslise.mixin.server;

import com.mojang.datafixers.util.Either;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkMap.class)
public abstract class Mixin_ChunkMap_Debug {
    @Inject(
            method = "Lnet/minecraft/server/level/ChunkMap;prepareTickingChunk(Lnet/minecraft/server/level/ChunkHolder;)Ljava/util/concurrent/CompletableFuture;",
            at = @At("HEAD")
    )
    public void logPreparation(ChunkHolder chunkHolder, CallbackInfoReturnable<CompletableFuture<Either<LevelChunk, ChunkHolder.ChunkLoadingFailure>>> ci) {
        //SpaceTest.LOGGER.info("ChunkMap#prepareTickingChunk called for chunk at " + chunkHolder.getPos() + ".");
    }

    @Inject(
            method = "playerLoadedChunk",
            at = @At("HEAD")
    )
    private void logLoadedChunk(ServerPlayer player, Packet<?>[] packets, LevelChunk chunk, CallbackInfo ci) {
        //SpaceTest.LOGGER.info("ChunkMap#playerLoadedChunk called for chunk at " + chunk.getPos() + ".");
    }

    @Inject(
            method = "move",
            at = @At("HEAD")
    )
    private void logMove(ServerPlayer player, CallbackInfo ci) {
        //SpaceTest.LOGGER.info("ChunkMap#move called for player currently at " + player.position() + ", formerly at "+player.getLastSectionPos()+".");
    }
}
