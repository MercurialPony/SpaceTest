package melonslise.immptl.common.world.chunk;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongConsumer;

/**
 * This is the master class responsible for managing the render managers for each player, and determining which chunks
 * need to be forced/unforced.
 */
public class RenderLoaderManager
{
	// Renderloader tracker
	// TODO Should I also track what dimensions a player can see?
	// TODO How to handle concurrent access to these maps? Is that even an problem?

	// Map of all renderloaders to the chunks that own them. That is, the chunk where the owner for them is currently located.
	private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<HashSet<RenderLoader>>> renderLoaderMap = new HashMap<>();

	// Manager for immutable renderloaders
	private static final ImmutableLoaderManager<ServerImmutableRenderLoader> immutableManager
			= new ImmutableLoaderManager<>(ServerImmutableRenderLoader::new);

	// Set of player managers
	private static final HashMap<ServerPlayer, PlayerRenderManager> playerManagers = new HashMap<>();


	// Map of a list of players watching each chunk, per dimension.
	// TODO Maybe have a config option that enforces completely thread-safe write behavior? Where it will use concurrent
	//		hash sets, or make a copy when modifying a watchlist, modify the copy, then substitute the copy in for the original?
	private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<HashSet<ServerPlayer>>> playersWatchingChunks = new HashMap<>();
	// Chunks that were added to the map of forced chunks in the previous update, and need to have force tickets queued.
	private static final Map<ResourceKey<Level>, LongOpenHashSet> addedChunksMap = new HashMap<>();
	// Chunks that were removed from the map of forced chunks in the previous update, and need to have unforce tickets queued.
	private static final Map<ResourceKey<Level>, LongOpenHashSet> removedChunksMap = new HashMap<>();

	// Empty collections of chunk watchers
	private static final Long2ObjectOpenHashMap<HashSet<ServerPlayer>> emptyDimensionWatchers = new Long2ObjectOpenHashMap<>();
	private static final HashSet<ServerPlayer> emptyChunkWatchers = new HashSet<>();
	private static final Long2ObjectOpenHashMap<HashSet<RenderLoader>> emptyDimensionRenderLoaders = new Long2ObjectOpenHashMap<>();
	private static final HashSet<RenderLoader> emptyChunkRenderLoaders = new HashSet<>();

	private static MinecraftServer server = null;
	private static boolean ready = false;
	// How frequently to run updates for players in the world. I.e. this value is how many server ticks will be between
	// updates.
	private static final int updatePeriod = 10;


	/**
	 *
	 * @param owner
	 * @param startCorner
	 * @param xWidth
	 * @param zWidth
	 * @return
	 */
	public static boolean addImmutableRenderLoader(DimBlockPos owner, DimChunkPos startCorner, int xWidth, int zWidth)
	{
		return immutableManager.queueCreate(owner, startCorner, xWidth, zWidth);
	}

	/**
	 *
	 * @param owner
	 * @return
	 */
	public static boolean removeImmutableRenderLoader(DimBlockPos owner)
	{
		return immutableManager.queueDestroy(owner);
	}

	/**
	 *
	 * @param loaders
	 */
	private static void addRenderLoaders(ArrayList<? extends SingleDimensionRenderLoader> loaders, HashSet<PlayerRenderManager> updatedManagers)
	{
		loaders.forEach((loader) -> {
			DimBlockPos owner = loader.getOwnerLocation();
			// Make sure the owner's dimension exists in our map
			// TODO Why is this done explicitly, if we're just going to call computeIfAbsent?
			renderLoaderMap
					.computeIfAbsent(owner.dimension, (d) -> new Long2ObjectOpenHashMap<HashSet<RenderLoader>>())
					.computeIfAbsent(ChunkPos.asLong(owner.pos), (p) -> new HashSet<>()).add(loader);
			playersWatchingChunks.getOrDefault(owner.dimension, emptyDimensionWatchers).getOrDefault(ChunkPos.asLong(owner.pos), emptyChunkWatchers).forEach((player) -> {
				PlayerRenderManager manager = playerManagers.get(player);
				manager.addCreatedRenderLoader(loader);
				updatedManagers.add(manager);
			});
		});
	}

	/**
	 *
	 * @param loaders
	 */
	private static void removeRenderLoaders(ArrayList<? extends SingleDimensionRenderLoader> loaders, HashSet<PlayerRenderManager> updatedManagers)
	{
		// FIXME potential issues if a loader is destroyed and recreated on the same tick? Wait, no, because chunks are handled
		//  through playerRenderManagers, which do so safely.
		loaders.forEach((loader) -> {
			DimBlockPos owner = loader.getOwnerLocation();
			Long2ObjectOpenHashMap<HashSet<RenderLoader>> dimMap = renderLoaderMap.get(owner.dimension);
			HashSet<RenderLoader> loaderSet = dimMap.get(ChunkPos.asLong(owner.pos));
			loaderSet.remove(loader);
			if (loaderSet.size() == 0)
			{
				dimMap.remove(ChunkPos.asLong(owner.pos));
			}
			playersWatchingChunks.getOrDefault(owner.dimension, emptyDimensionWatchers).getOrDefault(ChunkPos.asLong(owner.pos), emptyChunkWatchers).forEach((player) -> {
				PlayerRenderManager manager = playerManagers.get(player);
				manager.removeDestroyedRenderLoader(loader);
				updatedManagers.add(manager);
			});
		});
	}

	/**
	 * Executes a provided consumer for each renderloader located at the provided dimension and chunk position.
	 * @param dimension
	 * @param chunkPos
	 * @param consumer
	 */
	public static void forEachRenderLoader(ResourceKey<Level> dimension, long chunkPos, Consumer<RenderLoader> consumer)
	{
		renderLoaderMap
				.getOrDefault(dimension, emptyDimensionRenderLoaders)
				.getOrDefault(chunkPos, emptyChunkRenderLoaders)
				.forEach(consumer);
	}

	/**
	 *
	 * @param dimension
	 * @param chunkPos
	 * @return
	 */
	public static HashSet<ServerPlayer> getChunkWatchers(ResourceKey<Level> dimension, long chunkPos) {

		HashSet<ServerPlayer> watchers = playersWatchingChunks.getOrDefault(dimension, emptyDimensionWatchers)
				.getOrDefault(chunkPos, emptyChunkWatchers);
//		if (watchers.isEmpty())
//		{
//			SpaceTest.LOGGER.warn("Returning empty watcher list for chunk! Chunk: "+new ChunkPos(chunkPos)+" in dimension "+dimension+".");
//		}
		return watchers;
	}

	/**
	 * Updates the chunk tracking for all added/removed chunks for a given player manager
	 * I.e. it sends new chunk packets for each added chunk, and unload packets for each removed chunk.
	 * @param managers
	 */
	public static void sendAddedAndRemovedChunks(Iterable<PlayerRenderManager> managers)
	{
		managers.forEach(manager -> {
			ServerPlayer player = manager.getPlayer();
			//SpaceTest.LOGGER.info("Sending added/removed chunks for "+player);
			// For each ChunkMap, send out the appropriate new chunk packets
			manager.forEachAdded((dimension) -> {
				// FIXME Hack so Minecraft doesn't break before I set up client-side handling for multiple dimensions.
				//		Breaks any actual loading of chunks in other dimensions though - chunks added in other dimensions
				//		don't get sent, so if the player then teleports to those chunks, they'll never be sent, as they've
				//		already been added.
				if (!dimension.equals(player.getLevel().dimension()))
				{
					return (chunkPos) -> {};
				}
				ChunkMap source = server.getLevel(dimension).getChunkSource().chunkMap;
				return (chunkPos) ->
						source.updateChunkTracking(player, new ChunkPos(chunkPos), new Packet[2], false, true);
			});
			manager.forEachRemoved((dimension) -> {
				// FIXME Hack so Minecraft doesn't break before I set up client-side handling for multiple dimensions
				//		Breaks any actual loading of chunks in other dimensions though - chunks added in other dimensions
				//		don't get sent, so if the player then teleports to those chunks, they'll never show up, as they've
				//		already been added.
				if (!dimension.equals(player.getLevel().dimension()))
				{
					return (chunkPos) -> {};
				}
				ChunkMap source = server.getLevel(dimension).getChunkSource().chunkMap;
				return (chunkPos) ->
						source.updateChunkTracking(player, new ChunkPos(chunkPos), new Packet[2], true, false);
			});
		});
	}

	// Update logic
	private static void updatePlayers(Iterable<PlayerRenderManager> managers, Consumer<PlayerRenderManager> updateMethod, boolean send)
	{
		//SpaceTest.LOGGER.info("#updatePlayer: Clearing pending.");
		clearPending();
		//SpaceTest.LOGGER.info("#updatePlayer: Running updates for players.");
		managers.forEach(updateMethod);
		//SpaceTest.LOGGER.info("#updatePlayer: Updating watch map from players.");
		updateForceMap(managers);
		//SpaceTest.LOGGER.info("#updatePlayer: Queueing added/removed chunks.");
		queuePendingChunks();
		if (send)
		{
			//SpaceTest.LOGGER.info("#updatePlayer: Sending loaded/unloaded chunks for players.");
			sendAddedAndRemovedChunks(managers);
		}

		//SpaceTest.LOGGER.info("#updatePlayer: Finalizing updates for players.");
		managers.forEach(PlayerRenderManager::finalizeUpdate);
	}

	/**
	 * Updates the map of what chunks should be forced, and computes what chunks were added/removed to the map, for all
	 * player managers. Processes added chunks, then removed chunks.
	 */
	private static void updateForceMap(Iterable<PlayerRenderManager> managers)
	{
		//SpaceTest.LOGGER.info("Updating forcing map.");
		// Get all added chunks, and update the tracking map.
		managers.forEach((manager) ->
				manager.forEachAdded((d) ->
						addChunks(manager, d)));

		// Get all removed chunks, and update the tracking map.
		// Do this after adding all chunks, so we don't have to handle cases where one renderloader removes a chunk,
		// then another one adds that same chunk.
		managers.forEach((manager) ->
				manager.forEachRemoved((Function<ResourceKey<Level>, LongConsumer>)(d) ->
						removeChunks(manager, d)));
		//SpaceTest.LOGGER.info("Finished updating forcing map.");
	}

	/**
	 * Adds all of a player manager's added chunks for a given dimension.
	 * @param manager
	 * @param dimension
	 * @return
	 */
	private static LongConsumer addChunks(PlayerRenderManager manager, ResourceKey<Level> dimension)
	{
		// Create the various maps if they don't exist. Validate?
		LongOpenHashSet added = addedChunksMap.computeIfAbsent(dimension, (dim) -> new LongOpenHashSet());
		Long2ObjectOpenHashMap<HashSet<ServerPlayer>> watchList
				= playersWatchingChunks.computeIfAbsent(dimension, (dim) -> new Long2ObjectOpenHashMap<>());
		ServerPlayer player = manager.getPlayer();
		return (chunkPos) -> {
			if (!watchList.computeIfAbsent(chunkPos, (pos) -> {
				//Creating the player list, so we know the chunk position was just added to the map
				added.add(chunkPos);
				return new HashSet<>();
			}).add(player))
			{
				// Player couldn't be added to the set, so we know we tried to add a duplicate.
				dumpMapsAndCrash("Attempted to add a duplicate player to a chunk's watchlist!"
						+"\n\t"+manager+"\n\tDimension: "+dimension.location()+"\n\tChunk: "+new ChunkPos(chunkPos));
			}
		};
	}

	/**
	 * Handles all of a player manager's removed chunks for a given dimension.
	 * @param manager
	 * @param dimension
	 * @return
	 */
	private static LongConsumer removeChunks(PlayerRenderManager manager, ResourceKey<Level> dimension)
	{
		// Get the various maps/sets for this dimension, so we don't have to get them for every position.
		LongOpenHashSet removed = removedChunksMap.computeIfAbsent(dimension, (d) -> new LongOpenHashSet());
		Long2ObjectOpenHashMap<HashSet<ServerPlayer>> watchMap
			= playersWatchingChunks.computeIfAbsent(dimension, (dim) -> {
				dumpMapsAndCrash("Attempted to unforce a chunk in a dimension that didn't have any forced chunks!"
						+"\n\t"+manager+"\n\tDimension: "+dimension.location());
				return new Long2ObjectOpenHashMap<>(); // Unused, but stops my IDE complaining.
			});
		ServerPlayer player = manager.getPlayer();

		return (chunkPos) -> {
			// Get the current chunk's watchmap
			HashSet<ServerPlayer> players = watchMap.computeIfAbsent(chunkPos, (pos) -> {
				// Attempting to remove an unforced chunk.
				dumpMapsAndCrash("Attempted to unforce a chunk that wasn't forced!"
						+"\n\t"+manager+"\n\tDimension: "+dimension.location()+"\n\tChunk: "+new ChunkPos(chunkPos));
				return new HashSet<>(); // Unused, but stops my IDE complaining.
			});

			// Remove the player from the watch list
			if (!players.remove(player))
			{
				dumpMapsAndCrash("Attempted to remove a player that wasn't in the watchlist!"
						+"\n\t"+manager+"\n\tDimension: "+dimension.location()+"\n\tChunk: "+new ChunkPos(chunkPos));
			}

			// Chunk has no watchers
			if (players.isEmpty())
			{
				removed.add(chunkPos);
				watchMap.remove(chunkPos);
			}
			// TODO Remove dimensions from the maps if they don't have any chunks?
		};
	}

	/**
	 * For each chunk added to/removed from our chunk tracking system, queue a corresponding force/unforce in the
	 * ticket manager.
	 */
	private static void queuePendingChunks()
	{
		addedChunksMap.forEach((dim, chunkMap) -> {
			ServerLevel level = server.getLevel(dim);
			chunkMap.forEach((LongConsumer) (chunkPos) -> {
				TicketManager.queueForcedChunk(level, new ChunkPos (chunkPos));
			});
		});
		removedChunksMap.forEach((dim, chunkMap) -> {
			ServerLevel level = server.getLevel(dim);
			chunkMap.forEach((LongConsumer) (chunkPos) -> {
				TicketManager.queueUnforcedChunk(level, new ChunkPos (chunkPos));
			});
		});
//		if (!addedChunksMap.isEmpty()) {
//			int i = 0;
//			for (LongOpenHashSet chunkSet : addedChunksMap.values())
//			{
//				i += chunkSet.size();
//			}
//			SpaceTest.LOGGER.info("Queued " + i + " chunks to be forced.");
//		}
//		if (!removedChunksMap.isEmpty()) {
//			int i = 0;
//			for (LongOpenHashSet chunkSet : removedChunksMap.values())
//			{
//				i += chunkSet.size();
//			}
//			SpaceTest.LOGGER.info("Queued " + i + " chunks to be unforced.");
//		}
		addedChunksMap.clear();
		removedChunksMap.clear();
	}

	// View distance update logic
	// Currently, I assume that view distance updates will be processed before general updates within the same tick.

	/**
	 * View distance update logic.
	 * Currently, I assume that view distance updates will be processed before general updates within the same tick.
	 * So everything involving view distance updates uses old positions.
	 * @param newViewDistance
	 */
	public static void updateViewDistanceAndSendChunks(int newViewDistance)
	{
		// While this is somewhat inefficient, as it has to recreate the packet for a given chunk each and every time
		// it sends it to a player - as opposed to vanilla, which reuses the packet for every player that needs it -
		// this isn't a very frequent operation. And to bring it in line with vanilla, I would need to also keep
		// lists of new/removed watchers for each chunk - on top of every other map in RenderLoaderManager.
		// So unless that's needed more frequently, I'm not going to implement it that way.
		// Plus, that's how the 'move' function handles it - it creates a new packet array for each chunk in a player's
		// old+new position rectangles; though tbf, for normal use cases (player moved a few blocks), you only
		// populate those packets for a couple dozen chunks, at most. Exceptions would be teleporting, which is rare, I think.
		int trueViewDistance = Helpers.computeTrueViewDistance(newViewDistance);
		SpaceTest.LOGGER.info("View distance updated to: "+trueViewDistance);
		if (ready)
		{
			updatePlayers(playerManagers.values(), (manager) -> manager.updateViewDistance(trueViewDistance), true);
		}
		else
		{
			SpaceTest.LOGGER.info("View distance set before server was ready!");
		}
	}

	public static void updateSinglePlayerAndSendChunks(ServerPlayer player)
	{
//		SpaceTest.LOGGER.info("Updating individual " + player + ".");
		PlayerRenderManager manager = playerManagers.get(player);
		if (manager == null)
		{
			dumpMapsAndCrash("Attempted to update a player which didn't have a render manager! Player: "+player);
		}
		if (ready)
		{
			updatePlayers(Collections.singleton(manager), PlayerRenderManager::update, true);
//			SpaceTest.LOGGER.info("Finished updating individual " + player + ".");
		}
		else
		{
			SpaceTest.LOGGER.info("Attempted to update player before server was ready!");
		}
	}

	/**
	 * Attempts to add a player to the player managers.
	 * @param player
	 * @return - true if player was added, false if the player already existed in the map.
	 */
	private static boolean addPlayerAndSendChunks(ServerPlayer player)
	{
		SpaceTest.LOGGER.info("Adding player "+player+" to the render manager.");
		if (playerManagers.get(player) != null)
		{
			SpaceTest.LOGGER.warn("Attempted to add a duplicate of player " + player + " to the render manager!");
			return false;
		}
		else
		{
			player.toString();
//			SpaceTest.LOGGER.info("Creating new manager");
			PlayerRenderManager manager = new PlayerRenderManager(new Object(), player, ServerHelper.getTrueViewDistance());
			playerManagers.put(player, manager);
//			SpaceTest.LOGGER.info("Player Managers: "+playerManagers);
			// Get all added chunks, and update the tracking map.
//			SpaceTest.LOGGER.info("Updating new player");
			updatePlayers(Collections.singleton(manager), (loader) -> {}, true);
			return true;
		}
	}

	/**
	 * Attempts to remove a player from the player managers.
	 * @param player
	 * @return - true if player was removed, false if the player wasn't in the map.
	 */
	private static boolean removePlayer(ServerPlayer player)
	{
		SpaceTest.LOGGER.info("Removing player "+player+" from the render manager.");
		PlayerRenderManager manager = playerManagers.remove(player);
//		SpaceTest.LOGGER.info("Player Managers: "+playerManagers);
		if (manager == null)
		{
			SpaceTest.LOGGER.warn("Attempted to remove a nonexistent player, "+player+", from the render manager!");
			//dumpMapsAndCrash("Attempted to remove a nonexistent player " + player + " from the render manager!");
			return false;
		}
		else
		{
			// Get all removed chunks, and update the tracking map.
			updatePlayers(Collections.singleton(manager), PlayerRenderManager::destroyChunkMap, false);
			// Don't need to send chunks, as this is only called when the player logs out.
			return true;
		}
	}

	private static void clearPending()
	{
		addedChunksMap.clear();
		removedChunksMap.clear();
	}

	private static void clearMaps()
	{
		immutableManager.clear();
		renderLoaderMap.clear();
		playerManagers.clear();
		playersWatchingChunks.clear();
		immutableManager.clear();
		clearPending();
	}

	// Event methods
	public static void onServerStart(MinecraftServer serverIn)
	{
		SpaceTest.LOGGER.info("Received server start event");
		if (server != null)
		{
			SpaceTest.LOGGER.warn("Received FMLServerStartedEvent but server variable already referenced a server.");
		}
		server = serverIn;
		ready = true;
		// TODO is this necessary?
		//updateViewDistance(ServerHelper.getTrueViewDistance());
	}

	//TODO Should creating new maps be done upon server initialization? Would need to be sure I'm doing it before any
	//	chunks are loaded. Probably safer to do on server closing.
	public static void onServerClosing(MinecraftServer serverIn)
	{
		ready = false;
		SpaceTest.LOGGER.info("Received server stopped event");
		server = null;
		clearMaps();
	}

	public static void tick()
	{
		ImmutableLoaderManager.ExistentialLoaders loaders = immutableManager.processQueued();
		HashSet<PlayerRenderManager> updatedPlayers = new HashSet<>();
		removeRenderLoaders(loaders.destroyed, updatedPlayers);
		addRenderLoaders(loaders.created, updatedPlayers);
		// FIXME potential crash if a renderloader is somehow destroyed and recreated for the same owner on the same
		//  tick.
		//  Player with old renderloader receives destruction message -> sets the renderloader to have the old view
		// 	-> receives creation message -> crashes because it doesn't allow duplicates.
		//  Or maybe not, since it uses the renderloader's hash code, which should be different unless the renderloader
		//  is the same...
		if (server.getTickCount() % updatePeriod == 0)
		{
			// Should be:
			// 		1. Update all renderloaders via update()
			// 		2. Update all player render managers, via update();
			// 		3. Update chunk forcing map based on added/removed chunks from the render managers
			//		4. Update chunk forcing based on chunks added/removed from the chunk forcing map.
			//		4.5. Send chunk load/unload packets for all players.
			//		4.6, 4.7 Finalize updates for renderloaders and player managers.
			// 		5. Update ticket manager
			// TODO - Should 4 & 5 be combined into one thing? Or should the actual management of the forcing map
			//		be handled in a RenderLoaderManager master object?
			// TODO - Also, currently the player render managers are what actually update the player render loaders, not
			//		this class - this class never even sees them directly. Should that be changed? Should I add the player
			//		render loaders to this class to bring them all in line with that logic? Right now I have to ensure
			//		the player renderloaders are updated at the start of the player render manager logic, as I can't assume
			//		they're updated when that method is called. I mean, I can get away with it for now, b/c the player
			//		render loaders aren't "watchable" - nothing depends on their state, aside from the player render
			//		loader manager object that holds them.

//			SpaceTest.LOGGER.info("Updating all render loaders.");

			immutableManager.updateLoaders();
//			SpaceTest.LOGGER.info("Player Managers: "+playerManagers);

			updatePlayers(playerManagers.values(), PlayerRenderManager::update, true);

			immutableManager.finalizeUpdateLoaders();
//			SpaceTest.LOGGER.info("Updating all render loaders END.");
		}
		else if (!updatedPlayers.isEmpty())
		{
			updatePlayers(updatedPlayers, RenderLoader::update, true);
		}
		TicketManager.processPendingChunks(); // 5.
	}

	public static void playerJoined(PlayerEvent.PlayerLoggedInEvent event)
	{
		SpaceTest.LOGGER.info("Received player login event.");
		if (event.getPlayer() instanceof ServerPlayer) {
			addPlayerAndSendChunks((ServerPlayer) event.getPlayer());
		}
		// How to handle the added chunks from a player joining the server?
		// Well, there isn't (functionally) a difference between a new player's added chunks, and the added chunks for
		// a player that had their view distance updated from 0 to something else. So I could leverage that, somehow?
		// Likewise for a player leaving the server.
	}

	public static void playerLeft(PlayerEvent.PlayerLoggedOutEvent event)
	{
		SpaceTest.LOGGER.info("Received player logout event.");
		if (event.getPlayer() instanceof ServerPlayer) {
			removePlayer((ServerPlayer) event.getPlayer());
		}
	}

	public static void playerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if (event.getPlayer() instanceof ServerPlayer)
		{
			updateSinglePlayerAndSendChunks((ServerPlayer) event.getPlayer());
		}
	}

	public static void playerRespawned(PlayerEvent.PlayerRespawnEvent event)
	{
		SpaceTest.LOGGER.info("Received player clone event.");
		if (event.getPlayer() instanceof ServerPlayer)
		{
			// Do this instead of add/remove player because I can handle it internally to the playerManager - potentially
			// taking advantage of the preexisting chunkmap. Also allows me to do any special handling that's necessary.
			ServerPlayer player = (ServerPlayer) event.getPlayer();
			playerManagers.get(player).playerRespawned(player);
		}
	}

	public static int dumpPlayerManager(ServerPlayer player)
	{
		PlayerRenderManager manager = playerManagers.get(player);
		if (manager == null)
		{
			SpaceTest.LOGGER.warn("Attempted to dump a player manager for a player without a manager!");
			return -1;
		}
		manager.dumpManager();
		return 0;
	}

	public static void dumpMapsAndCrash(String message)
	{
		// Necessary because under certain circumstances, the RuntimeException just seems to kill the logical client, but not the whole game.
		SpaceTest.LOGGER.fatal(message);
		dumpMapsToLog();
		throw new RuntimeException(message);
	}

	// Crash dump method
	public static void dumpMapsToLog()
	{
		// Set of all renderloaders (excluding player render loaders).
		SpaceTest.LOGGER.info("\nDumping RenderLoaderManager maps to log.");
		SpaceTest.LOGGER.info("***Renderloader managers.");
		immutableManager.dumpManager();

		// Set of player managers
		SpaceTest.LOGGER.info("***Player managers map.");
		playerManagers.forEach((player, manager) -> SpaceTest.LOGGER.info("\nPlayer "+player+"\nManager: "+manager));

		// Chunks that were added to the map of forced chunks in the previous update, and need to have force tickets queued.
		SpaceTest.LOGGER.info("***Added chunks map.");
		addedChunksMap.forEach((dim, chunkSet) -> {
			SpaceTest.LOGGER.info("**Dimension: "+dim.location());
			chunkSet.stream().sorted().forEach((chunkPos) ->
					SpaceTest.LOGGER.info("*Chunk: "+new ChunkPos(chunkPos)));
		});
		// Chunks that were removed from the map of forced chunks in the previous update, and need to have unforce tickets queued.
		SpaceTest.LOGGER.info("***Removed chunks map.");
		removedChunksMap.forEach((dim, chunkSet) -> {
			SpaceTest.LOGGER.info("**Dimension: "+dim.location());
			chunkSet.stream().sorted().forEach((chunkPos) ->
					SpaceTest.LOGGER.info("*Chunk: "+new ChunkPos(chunkPos)));
		});

		// Map of a list of players watching each chunk, per dimension.
		SpaceTest.LOGGER.info("***Chunk watchmap.");
		playersWatchingChunks.forEach((dim, chunkMap) -> {
			SpaceTest.LOGGER.info("**Dimension: "+dim.location());
			chunkMap.keySet().stream().sorted().forEach((chunkPos) ->
					SpaceTest.LOGGER.info("\n*Chunk: "+new ChunkPos(chunkPos)+"\nWatchlist: "+chunkMap.get((long)chunkPos)));
		});
	}
}
