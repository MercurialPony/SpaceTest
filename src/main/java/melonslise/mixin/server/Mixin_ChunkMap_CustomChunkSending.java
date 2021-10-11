package melonslise.mixin.server;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import melonslise.immptl.common.world.chunk.RenderLoaderManager;
import melonslise.spacetest.SpaceTest;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Stream;

@Mixin(ChunkMap.class)
public abstract class Mixin_ChunkMap_CustomChunkSending {

    // TODO Remove any sending of chunk packets from the ChunkMap class
    //      as it will no longer make sense - the player won't have chunks for just one dimension/ChunkMap.
    //      Instead, I should have it send new chunks via ServerPlayer.move, on dimension changes, on player joins,
    //      and so on, in the appropriate classes, instead of in the ChunkMap class.
    //      Though, tbf, in vanilla's code, the player (always?) exists in only one ChunkMap at a time.
    //      And it may be less obtuse if I keep it all in one place.
    @Shadow
    @Final
    ServerLevel level;

    @Shadow
    int viewDistance;

    @Shadow
    private ChunkMap.DistanceManager distanceManager;

    @Shadow
    private Long2ObjectLinkedOpenHashMap<ChunkHolder> updatingChunkMap;

    @Shadow
    private PlayerMap playerMap;

    @Shadow
    private Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow
    protected void updateChunkTracking(ServerPlayer p_140187_, ChunkPos p_140188_, Packet<?>[] p_140189_, boolean p_140190_, boolean p_140191_) {}

    @Shadow
    private boolean skipPlayer(ServerPlayer p_140187_) {return false;}

    @Shadow
    private SectionPos updatePlayerPos(ServerPlayer p_140187_) {return null;}

    @Shadow
    private static int checkerboardDistance(ChunkPos p_140339_, ServerPlayer p_140340_, boolean p_140341_) {return 42;}

    @Shadow
    private static int checkerboardDistance(ChunkPos p_140207_, int p_140208_, int p_140209_) {return 42;}

    /**
     * @author Joekeen
     * @reason Old version isn't very useful with the renderloading.
     */
    @Overwrite
    public Stream<ServerPlayer> getPlayers(ChunkPos chunkPos, boolean p_140253_) {
        // Can this be optimized? Makes sending new chunks ~14% slower than the vanilla method.
        // Is it the way I'm doing it? Or is it the call to a method outside the class, from within a completable future?
        // Okay, I think it was the way I was doing it -
        return RenderLoaderManager.getChunkWatchers(this.level.dimension(), chunkPos.toLong()).stream();
    }

    /**
     * @author Joekeen
     * @param viewDistance
     * @reason Not sure how to use injection to replace a for loop, so I just rewrote it.
     */
    @Overwrite
    public void setViewDistance(int viewDistance) {
        // Vanilla stuff, we don't want to break things.
        int i = Mth.clamp(viewDistance + 1, 3, 33);
        if (i != this.viewDistance) {
            int j = this.viewDistance;
            this.viewDistance = i;
            this.distanceManager.updatePlayerTickets(this.viewDistance);

            // My custom stuff
            RenderLoaderManager.updateViewDistanceAndSendChunks(viewDistance);
        }
    }

    // In case I no longer use this (just because I think it's hilariously obtuse - or maybe not?):
//    RenderLoaderManager.forEachAddedChunkWatcher(this.level.dimension(), (player) -> (chunkPosLong) -> { // This isn't obtuse or anything
//        ChunkHolder chunkHolder = this.updatingChunkMap.get(chunkPosLong);
//        ChunkPos chunkPos = chunkHolder.getPos();
//        Packet<?>[] packet = new Packet[2];
//        this.updateChunkTracking(player, chunkPos, packet, flag, flag1);
//    });


    @ModifyVariable(
            method="updatePlayerStatus",
            name = "l",
            at=@At(
                    value="STORE",
                    ordinal=0
            )
    )
    public int removeForLoop(int var)
    {
        //SpaceTest.LOGGER.info("updatePlayerStatus forloop index value: "+var);
        return Integer.MAX_VALUE;
    }

    @Inject(
            method="updatePlayerStatus",
            at=@At(
                    value="INVOKE",
                    target="Lnet/minecraft/world/level/ChunkPos;<init>(II)V"
            )
    )
    public void checkInjectSuccess(CallbackInfo ci)
    {
        SpaceTest.LOGGER.info("Executing from within updatePlayerStatus forloop");
    }

    @Inject(
            method="updatePlayerStatus",
            at=@At(
                    "TAIL"
            )
    )
    public void updatePlayerStatusNew(ServerPlayer player, boolean addPlayer, CallbackInfo ci)
    {
//        SpaceTest.LOGGER.info("updatePlayerStatus new logic.");
//        SpaceTest.LOGGER.info("updatePlayerStatus new logic END.");
    }

    /**
     * @author Joekeen
     * @param player
     * @reason Not sure how to use injection to replace a for loop, so I just rewrote it.
     */
    @Overwrite
    public void move(ServerPlayer player)
    {
        for(ChunkMap.TrackedEntity chunkmap$trackedentity : this.entityMap.values()) {
            if (chunkmap$trackedentity.entity == player) {
                chunkmap$trackedentity.updatePlayers(this.level.players());
            } else {
                chunkmap$trackedentity.updatePlayer(player);
            }
        }

        int l1 = SectionPos.blockToSectionCoord(player.getBlockX());
        int i2 = SectionPos.blockToSectionCoord(player.getBlockZ());
        SectionPos sectionpos = player.getLastSectionPos();
        SectionPos sectionpos1 = SectionPos.of(player);
        long i = sectionpos.chunk().toLong();
        long j = sectionpos1.chunk().toLong();
        boolean flag = this.playerMap.ignored(player);
        boolean flag1 = this.skipPlayer(player);
        boolean flag2 = sectionpos.asLong() != sectionpos1.asLong();
        if (flag2 || flag != flag1) {
            this.updatePlayerPos(player);
            if (!flag) {
                this.distanceManager.removePlayer(sectionpos, player);
            }

            if (!flag1) {
                this.distanceManager.addPlayer(sectionpos1, player);
            }

            if (!flag && flag1) {
                this.playerMap.ignorePlayer(player);
            }

            if (flag && !flag1) {
                this.playerMap.unIgnorePlayer(player);
            }

            if (i != j) {
                this.playerMap.updatePlayer(i, j, player);
            }
        }
        // Only update the player if they changed chunks.
        if (i != j)
        {
//            SpaceTest.LOGGER.info("Player moved. Updating render manager for "+player+".");
            RenderLoaderManager.updateSinglePlayerAndSendChunks(player);
//            SpaceTest.LOGGER.info("Player moved. Updating render manager for "+player+".");
        }
    }
}
