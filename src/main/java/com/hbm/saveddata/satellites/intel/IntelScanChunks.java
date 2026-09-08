package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.List;
import com.hbm.main.MainRegistry;
import net.minecraft.world.World;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

/** Temporary scan footprint, loaded one chunk per tick before sampling begins. */
public class IntelScanChunks {
	public static final String TICKET_KEY = "hbmIntelScan";
	private final World world;
	private final List<ChunkCoordIntPair> chunks;
	private Ticket ticket;
	private int next;
	private boolean acquired;
	public IntelScanChunks(World world, int x, int z) {
		this.world = world;
		chunks = footprint(x, z);
	}
	public static List<ChunkCoordIntPair> footprint(int x, int z) {
		List<ChunkCoordIntPair> result = new ArrayList<ChunkCoordIntPair>();
		for(int cx = (x - 32) >> 4; cx <= (x + 31) >> 4; cx++)
			for(int cz = (z - 32) >> 4; cz <= (z + 31) >> 4; cz++)
				result.add(new ChunkCoordIntPair(cx, cz));
		return result;
	}
	protected void acquire(int count) {
		ticket = ForgeChunkManager.requestTicket(MainRegistry.instance, world, Type.NORMAL);
		if(ticket == null) throw new IllegalStateException("SCAN_CHUNK_TICKET_UNAVAILABLE");
		ticket.getModData().setBoolean(TICKET_KEY, true);
		if(ticket.getChunkListDepth() < count) {
			ForgeChunkManager.releaseTicket(ticket); ticket = null;
			throw new IllegalStateException("SCAN_CHUNK_LIMIT_TOO_LOW");
		}
	}
	protected void load(ChunkCoordIntPair chunk) {
		ForgeChunkManager.forceChunk(ticket, chunk);
		world.getChunkFromChunkCoords(chunk.chunkXPos, chunk.chunkZPos);
	}
	protected void release() {
		if(ticket != null) ForgeChunkManager.releaseTicket(ticket);
		ticket = null;
	}
	public boolean tick() {
		try {
			if(!acquired) { acquire(chunks.size()); acquired = true; }
			if(next < chunks.size()) { load(chunks.get(next++)); return false; }
			return true;
		} catch(RuntimeException e) { close(); throw e; }
	}
	public void close() { if(acquired || ticket != null) release(); acquired = false; }
}
