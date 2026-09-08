package com.hbm.entity.missile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.ChunkCoordIntPair;

final class MissileChunkLoading {

	interface Operations {
		void force(ChunkCoordIntPair chunk);
		void load(ChunkCoordIntPair chunk);
		void unforce(ChunkCoordIntPair chunk);
	}

	private MissileChunkLoading() { }

	static List<ChunkCoordIntPair> moveTo(int centerX, int centerZ, Iterable<ChunkCoordIntPair> previouslyForced, Operations operations) {
		List<ChunkCoordIntPair> desired = getChunksToLoad(centerX, centerZ);

		for(ChunkCoordIntPair chunk : desired) {
			operations.force(chunk);
		}

		operations.load(new ChunkCoordIntPair(centerX, centerZ));

		for(ChunkCoordIntPair chunk : previouslyForced) {
			if(!desired.contains(chunk)) operations.unforce(chunk);
		}

		return desired;
	}

	static List<ChunkCoordIntPair> getChunksToLoad(int centerX, int centerZ) {
		List<ChunkCoordIntPair> chunks = new ArrayList<ChunkCoordIntPair>();
		for(int x = -1; x <= 1; x++) {
			for(int z = -1; z <= 1; z++) {
				chunks.add(new ChunkCoordIntPair(centerX + x, centerZ + z));
			}
		}
		return chunks;
	}
}
