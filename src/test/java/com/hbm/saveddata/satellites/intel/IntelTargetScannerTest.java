package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;

public class IntelTargetScannerTest {

	@Test
	public void findsUndergroundLaunchEquipmentRegardlessOfTerrainCellLimit() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		for(int i = 0; i < IntelScanResult.MAX_SUBSURFACE_CELLS; i++) result.subsurfaceCells.add(new IntelSurfaceCell());
		Chunks chunks = new Chunks();
		chunks.add(target("SILO_HATCH", "hbm:tile.silo_hatch", 0, 61, 0));
		chunks.add(target("LOADED_MISSILE", "hbm:item.missile_custom", 0, 21, 0));
		scan(chunks, result);
		assertEquals(2, result.findings.size());
		assertEquals(21, find(result, "LOADED_MISSILE").minY);
		assertEquals("hbm:tile.silo_hatch", find(result, "SILO_HATCH").targetId);
	}

	@Test
	public void surfaceAndSubsurfaceDoNotInspectOrExposeTargets() {
		for(IntelScanMode mode : Arrays.asList(IntelScanMode.SURFACE, IntelScanMode.SUBSURFACE)) {
			Chunks chunks = new Chunks();
			chunks.add(target("LOADED_MISSILE", "hbm:item.missile_custom", 0, 61, 0));
			IntelScanResult result = result(mode);
			new IntelTargetScanner().process(chunks, new IntelScanJob(mode), result, 1);
			assertTrue(chunks.reads.isEmpty());
			assertTrue(chunks.checked.isEmpty());
			assertTrue(result.findings.isEmpty());
		}
	}

	@Test
	public void checksEveryIntersectingChunkButNeverLoadsMissingChunks() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		result.targetX = -1;
		result.targetZ = -1;
		Chunks chunks = new Chunks();
		chunks.add(target("RADAR", "hbm:tile.machine_radar", -33, 64, -33));
		chunks.add(target("LAUNCHPAD", "hbm:tile.launch_pad", 30, 64, 30));
		chunks.add(target("LAUNCHPAD", "hbm:tile.launch_pad", 31, 64, 30));
		chunks.add(target("LAUNCHPAD", "hbm:tile.launch_pad", -34, 64, -33));
		scan(chunks, result);
		assertEquals(25, chunks.checked.size());
		assertEquals(2, chunks.reads.size());
		assertEquals(2, result.findings.size());
		assertEquals(-33, find(result, "RADAR").minX);
	}

	@Test
	public void processesOnlyTheRequestedChunkBudgetAndResumes() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		Chunks chunks = new Chunks();
		IntelScanJob job = new IntelScanJob(IntelScanMode.COMBINED);
		IntelTargetScanner scanner = new IntelTargetScanner();
		assertEquals(1, scanner.process(chunks, job, result, 1));
		assertEquals(1, job.phaseCursor);
		assertEquals(1, chunks.checked.size());
		assertEquals(1, scanner.process(chunks, job, result, 1));
		assertEquals(Arrays.asList("-2:-2", "-1:-2"), chunks.checked);
	}

	@Test
	public void explicitTargetsAreNotDroppedWhenGeneralFindingsFillTheResult() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		for(int i = 0; i < IntelScanResult.MAX_FINDINGS; i++) result.findings.add(new IntelFinding());
		Chunks chunks = new Chunks();
		chunks.add(target("FLYING_MISSILE", "hbm.missileCustom", 0, 300, 0));
		scan(chunks, result);
		assertEquals(IntelScanResult.MAX_FINDINGS, result.findings.size());
		assertEquals(300, find(result, "FLYING_MISSILE").minY);
	}

	@Test
	public void movingMissilesAreCountedOncePerScanWithoutMergingDifferentMissiles() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		Chunks chunks = new Chunks();
		UUID id = UUID.randomUUID();
		IntelFinding first = target("FLYING_MISSILE", "hbm.missileCustom", -1, 300, 0);
		first.sourceEntityId = id;
		chunks.add(first);
		IntelFinding moved = target("FLYING_MISSILE", "hbm.missileCustom", 0, 301, 0);
		moved.sourceEntityId = id;
		chunks.add(moved);
		IntelFinding other = target("FLYING_MISSILE", "hbm.missileCustom", 0, 301, 0);
		other.sourceEntityId = UUID.randomUUID();
		chunks.add(other);
		scan(chunks, result);
		assertEquals(2, result.findings.size());
		assertTrue(result.findings.contains(first));
		assertFalse(result.findings.contains(moved));
		assertTrue(result.findings.contains(other));
	}

	@Test
	public void targetDetailsSurviveSavingAndLoadingTheScan() {
		IntelScanResult result = result(IntelScanMode.COMBINED);
		IntelFinding missile = target("STORED_MISSILE", "hbm:item.missile_custom", 4, 18, 5);
		missile.targetCount = 3;
		result.findings.add(missile);
		NBTTagCompound nbt = new NBTTagCompound();
		result.writeToNBT(nbt);
		IntelFinding loaded = IntelScanResult.readFromNBT(nbt).findings.get(0);
		assertEquals("STORED_MISSILE", loaded.targetType);
		assertEquals("hbm:item.missile_custom", loaded.targetId);
		assertEquals(3, loaded.targetCount);
		assertEquals(18, loaded.minY);
	}

	@Test
	public void olderFindingsWithoutTargetFieldsRemainReadable() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("classification", "BUNKER");
		IntelFinding loaded = IntelFinding.readFromNBT(nbt);
		assertEquals(IntelClassification.BUNKER, loaded.classification);
		assertEquals("", loaded.targetType);
		assertEquals("", loaded.targetId);
		assertEquals(0, loaded.targetCount);
	}

	private static IntelScanResult result(IntelScanMode mode) {
		IntelScanResult result = new IntelScanResult();
		result.mode = mode;
		return result;
	}

	private static IntelFinding target(String type, String id, int x, int y, int z) {
		IntelFinding finding = new IntelFinding();
		finding.targetType = type;
		finding.targetId = id;
		finding.targetCount = 1;
		finding.minX = finding.maxX = x;
		finding.minY = finding.maxY = y;
		finding.minZ = finding.maxZ = z;
		return finding;
	}

	private static IntelFinding find(IntelScanResult result, String type) {
		for(IntelFinding finding : result.findings) if(type.equals(finding.targetType)) return finding;
		fail("Missing target " + type);
		return null;
	}

	private static void scan(Chunks chunks, IntelScanResult result) {
		IntelScanJob job = new IntelScanJob(result.mode);
		IntelTargetScanner scanner = new IntelTargetScanner();
		for(int i = 0; i < 25; i++) scanner.process(chunks, job, result, 1);
	}

	private static class Chunks implements IntelTargetScanner.TargetAccess {
		final Map<String, List<IntelFinding>> loaded = new HashMap<String, List<IntelFinding>>();
		final List<String> checked = new ArrayList<String>();
		final List<String> reads = new ArrayList<String>();

		void add(IntelFinding target) {
			String key = (target.minX >> 4) + ":" + (target.minZ >> 4);
			if(!loaded.containsKey(key)) loaded.put(key, new ArrayList<IntelFinding>());
			loaded.get(key).add(target);
		}

		@Override
		public boolean isChunkLoaded(int x, int z) {
			String key = x + ":" + z;
			checked.add(key);
			return loaded.containsKey(key);
		}

		@Override
		public Iterable<IntelFinding> targetsInChunk(int x, int z) {
			String key = x + ":" + z;
			if(!loaded.containsKey(key)) throw new AssertionError("Read an unloaded chunk");
			reads.add(key);
			return loaded.get(key);
		}
	}
}
