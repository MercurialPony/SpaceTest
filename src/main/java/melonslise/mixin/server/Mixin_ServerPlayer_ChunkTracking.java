package melonslise.mixin.server;

//@Mixin(ServerPlayer.class)
//public abstract class Mixin_ServerPlayer_ChunkTracking extends Entity {
//
//    public Mixin_ServerPlayer_ChunkTracking(EntityType type, ServerLevel level) {
//        super(type, level);
//        throw new RuntimeException();
//    }
//
//    @Inject(
//            method = "trackChunk",
//            at = @At("RETURN")
//    )
//    public void addChunk(ChunkPos pos, Packet<?> chunkPacket, Packet<?> lightingPacket, CallbackInfo ci) {
//        RenderLoaderManager.registerChunkLoadedToPlayer((ServerPlayer) (Object) this, new DimChunkPos(this.level.dimension(), pos));
//    }
//
//    @Inject(
//            method = "untrackChunk",
//            at = @At("RETURN")
//    )
//    public void removeChunk(ChunkPos pos, CallbackInfo ci) {
//        RenderLoaderManager.registerChunkUnloadedFromPlayer((ServerPlayer) (Object) this, new DimChunkPos(this.level.dimension(), pos));
//    }
//}
