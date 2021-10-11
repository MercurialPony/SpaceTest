package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;

import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.function.LongConsumer;

public class VersatileClientChunkCache extends ClientChunkCache {

    private final Long2ObjectOpenHashMap<LevelChunk> levelChunks = new Long2ObjectOpenHashMap<>();

    public VersatileClientChunkCache(ClientLevel level, int p_104415_) {
        super(level, p_104415_);
    }

    // TODO Is the isValidChunk necessary? Vanilla does it, but I'm not sure if it's necessary for my stuff. How would
    //  an invalid chunk end up in the map, anyways? I think I just need to check if the LevelChunk returned is null...
    @Override
    public void drop(int chunkX, int chunkZ)
    {
        long chunkPos = ChunkPos.asLong(chunkX, chunkZ);
        LevelChunk levelChunk = this.levelChunks.get(chunkPos);
        if (isValidChunk(levelChunk, chunkX, chunkZ))
        {
            MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(levelChunk));
            levelChunks.remove(chunkPos);
        }
    }

    @Nullable
    @Override
    public LevelChunk getChunk(int chunkX, int chunkZ, ChunkStatus status, boolean something)
    {
        long chunkPos = ChunkPos.asLong(chunkX, chunkZ);
        LevelChunk levelChunk = this.levelChunks.get(chunkPos);
        if (isValidChunk(levelChunk, chunkX, chunkZ))
        {
            return levelChunk;
        }
        return something ? this.emptyChunk : null;
    }

    @Nullable
    @Override
    public LevelChunk replaceWithPacketData(int chunkX, int chunkZ, ChunkBiomeContainer biomeContainer, FriendlyByteBuf byteBuf, CompoundTag compoundTag, BitSet bitSet) {
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);
        long longPos = chunkPos.toLong();
        LevelChunk levelChunk = this.levelChunks.get(longPos);
        if (!isValidChunk(levelChunk, chunkX, chunkZ))
        {
            levelChunk = new LevelChunk(this.level, chunkPos, biomeContainer);
            levelChunk.replaceWithPacketData(biomeContainer, byteBuf, compoundTag, bitSet);
            this.levelChunks.put(longPos, levelChunk);
        }
        else
        {
            levelChunk.replaceWithPacketData(biomeContainer, byteBuf, compoundTag, bitSet);
        }

        LevelChunkSection[] levelChunkSections = levelChunk.getSections();
        LevelLightEngine levelLightEngine = this.getLightEngine();
        levelLightEngine.enableLightSources(chunkPos, true);

        for (int j = 0; j < levelChunkSections.length; ++j)
        {
            LevelChunkSection levelChunkSection = levelChunkSections[j];
            int k = this.level.getSectionYFromSectionIndex(j);
            levelLightEngine.updateSectionStatus(SectionPos.of(chunkX, k, chunkZ), LevelChunkSection.isEmpty(levelChunkSection));
        }

        this.level.onChunkLoaded(chunkPos);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkEvent.Load(levelChunk));

        return levelChunk;
    }

    @Override
    public void updateViewCenter(int chunkX, int chunkZ)
    {

    }

    @Override
    public void updateViewRadius(int newViewRadius)
    {
        // TODO Hook this into the client's render mapping?
        //  Though, since this is mostly for dictating what chunks are stored, maybe not?
    }

    @Override
    public String gatherStats() {
        return this.levelChunks.size() + ", " + this.levelChunks.size();
    }

    public void forEachChunk(LongConsumer posConsumer) {
        this.levelChunks.keySet().forEach(posConsumer);
    }
}
