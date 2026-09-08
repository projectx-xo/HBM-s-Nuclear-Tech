package com.hbm.entity.missile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import net.minecraft.world.ChunkCoordIntPair;

public class EntityMissileChunkLoadingTest {

	@Test
	public void keepsCurrentAndAdjacentChunksLoaded() {
		List<ChunkCoordIntPair> chunks = EntityMissileBaseNT.getChunksToLoad(200, -75);
		Set<Long> coordinates = new HashSet<Long>();
		for(ChunkCoordIntPair chunk : chunks) {
			coordinates.add(ChunkCoordIntPair.chunkXZ2Int(chunk.chunkXPos, chunk.chunkZPos));
		}

		assertEquals(9, chunks.size());
		assertEquals(9, coordinates.size());
		for(int x = 199; x <= 201; x++) {
			for(int z = -76; z <= -74; z++) {
				assertTrue(coordinates.contains(ChunkCoordIntPair.chunkXZ2Int(x, z)));
			}
		}
	}

	@Test
	public void loadsDestinationAfterForcingAndBeforeUnforcing() {
		List<ChunkCoordIntPair> oldChunks = EntityMissileBaseNT.getChunksToLoad(198, 261);
		final List<String> operations = new ArrayList<String>();

		MissileChunkLoading.moveTo(198, 260, oldChunks, new MissileChunkLoading.Operations() {
			@Override
			public void force(ChunkCoordIntPair chunk) {
				operations.add("force:" + chunk.chunkXPos + "," + chunk.chunkZPos);
			}

			@Override
			public void load(ChunkCoordIntPair chunk) {
				operations.add("load:" + chunk.chunkXPos + "," + chunk.chunkZPos);
			}

			@Override
			public void unforce(ChunkCoordIntPair chunk) {
				operations.add("unforce:" + chunk.chunkXPos + "," + chunk.chunkZPos);
			}
		});

		assertEquals(13, operations.size());
		for(int i = 0; i < 9; i++) assertTrue(operations.get(i).startsWith("force:"));
		assertEquals("load:198,260", operations.get(9));
		assertEquals("unforce:197,262", operations.get(10));
		assertEquals("unforce:198,262", operations.get(11));
		assertEquals("unforce:199,262", operations.get(12));
	}
}
