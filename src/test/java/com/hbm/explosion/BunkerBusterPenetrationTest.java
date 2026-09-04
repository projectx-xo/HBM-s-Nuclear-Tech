package com.hbm.explosion;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class BunkerBusterPenetrationTest {

	@Test
	public void penetratesMultipleHundredResistanceLayersAndDetonatesInside() {
		Grid grid = new Grid();
		grid.put(0, 99, 0, 2F).put(0, 98, 0, 6F).put(0, 97, 0, 100F).put(0, 96, 0, 100F);
		BunkerBusterPenetration.Result result = down(grid);
		assertTrue(result.armed);
		assertEquals(95.5, result.y, 0.0001);
		assertEquals(4, grid.removed.size());
		assertFalse(grid.cells.containsKey("0,96,0"));
	}

	@Test
	public void resistanceOverHundredStopsBeforeTheBlockAndDoesNotArm() {
		Grid grid = new Grid().put(0, 99, 0, 100.01F).put(0, 98, 0, 40F);
		BunkerBusterPenetration.Result result = down(grid);
		assertFalse(result.armed);
		assertTrue(result.y >= 100);
		assertTrue(grid.removed.isEmpty());
		assertTrue(grid.cells.containsKey("0,98,0"));
	}

	@Test
	public void ordinaryGroundAndCavesDoNotArmTheFullYield() {
		Grid grid = new Grid().put(0, 99, 0, 2F).put(0, 97, 0, 6F).put(0, 95, 0, 39.99F);
		BunkerBusterPenetration.Result result = down(grid);
		assertFalse(result.armed);
		assertEquals(3, grid.removed.size());
	}

	@Test
	public void ignoresCavitiesUntilItHasPenetratedBunkerMaterial() {
		Grid grid = new Grid().put(0, 99, 0, 6F).put(0, 96, 0, 40F);
		BunkerBusterPenetration.Result result = down(grid);
		assertTrue(result.armed);
		assertEquals(95.5, result.y, 0.0001);
		assertEquals(2, grid.removed.size());
	}

	@Test
	public void anArmedPayloadStopsAtTheFirstInteriorAirCell() {
		Grid grid = new Grid().put(0, 99, 0, 84F).put(0, 97, 0, 84F);
		BunkerBusterPenetration.Result result = down(grid);
		assertEquals(98.5, result.y, 0.0001);
		assertTrue(result.armed);
		assertTrue(grid.cells.containsKey("0,97,0"));
		assertEquals(1, grid.removed.size());
	}

	@Test
	public void stopsOnTheNearSideOfAnUnbreakableLayerAfterArming() {
		Grid grid = new Grid().put(0, 99, 0, 100F).put(0, 98, 0, Float.POSITIVE_INFINITY).put(0, 97, 0, 40F);
		BunkerBusterPenetration.Result result = down(grid);
		assertTrue(result.armed);
		assertEquals(99.5, result.y, 0.0001);
		assertEquals(1, grid.removed.size());
		assertTrue(grid.cells.containsKey("0,97,0"));
	}

	@Test
	public void stopsAtUnloadedSpaceWithoutProbingBeyondIt() {
		Grid grid = new Grid().put(0, 99, 0, Float.POSITIVE_INFINITY);
		down(grid);
		assertEquals(1, grid.reads.size());
		assertTrue(grid.removed.isEmpty());
	}

	@Test
	public void doesNotArmOrContinueWhenTheBlockCannotBeRemoved() {
		Grid grid = new Grid().put(0, 99, 0, 84F).put(0, 98, 0, 40F);
		grid.denied.add("0,99,0");
		BunkerBusterPenetration.Result result = down(grid);
		assertFalse(result.armed);
		assertTrue(grid.removed.isEmpty());
		assertEquals(1, grid.reads.size());
	}

	@Test
	public void doesNotSkipAThinDiagonalIntersectionWithAResistantBlock() {
		Grid grid = new Grid().put(0, 10, 0, 6F).put(0, 9, 0, 101F).put(1, 9, 0, 40F);
		BunkerBusterPenetration.Result result = BunkerBusterPenetration.penetrate(grid, 0.99, 10.001, 0.5, 1, -1, 0);
		assertFalse(result.armed);
		assertEquals(1, grid.removed.size());
		assertTrue(grid.reads.contains("0,9,0"));
		assertTrue(grid.cells.containsKey("1,9,0"));
	}

	@Test
	public void negativeCoordinatesUseTheCorrectVoxelAndNeverRevisitRemovedBlocks() {
		Grid grid = new Grid().put(-1, 9, -1, 100F);
		BunkerBusterPenetration.Result result = BunkerBusterPenetration.penetrate(grid, -0.5, 10, -0.5, 0, -20, 0);
		assertTrue(result.armed);
		assertEquals(-0.5, result.x, 0.0001);
		assertEquals(8.5, result.y, 0.0001);
		assertEquals(-0.5, result.z, 0.0001);
		assertEquals(1, grid.removed.size());
		assertEquals(2, grid.reads.size());
	}

	@Test
	public void drillingThroughOrdinaryGroundHasABoundedDepth() {
		Grid grid = new Grid();
		grid.defaultResistance = 6F;
		BunkerBusterPenetration.Result result = down(grid);
		assertFalse(result.armed);
		assertEquals(96, grid.removed.size());
		assertEquals(4.5, result.y, 0.0001);
		assertFalse(grid.reads.contains("0,3,0"));
	}

	@Test
	public void zeroDirectionDoesNotReadOrModifyTheWorld() {
		Grid grid = new Grid();
		BunkerBusterPenetration.penetrate(grid, 0.5, 100, 0.5, 0, 0, 0);
		assertTrue(grid.reads.isEmpty());
		assertTrue(grid.removed.isEmpty());
	}

	private BunkerBusterPenetration.Result down(Grid grid) {
		return BunkerBusterPenetration.penetrate(grid, 0.5, 100, 0.5, 0, -2, 0);
	}

	private static class Grid implements BunkerBusterPenetration.BlockAccess {
		final Map<String, Float> cells = new HashMap<>();
		final Set<String> denied = new HashSet<>();
		final List<String> removed = new ArrayList<>();
		final List<String> reads = new ArrayList<>();
		float defaultResistance = -1F;

		Grid put(int x, int y, int z, float resistance) {
			cells.put(x + "," + y + "," + z, resistance);
			return this;
		}

		@Override
		public float getResistance(int x, int y, int z) {
			String key = x + "," + y + "," + z;
			reads.add(key);
			return cells.getOrDefault(key, defaultResistance);
		}

		@Override
		public boolean removeBlock(int x, int y, int z) {
			String key = x + "," + y + "," + z;
			if(denied.contains(key)) return false;
			cells.remove(key);
			removed.add(key);
			return true;
		}
	}
}
