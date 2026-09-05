package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import com.hbm.blocks.network.PowerCableBox;

public class IntelProjectionTest {

	@Test public void captureIsBudgetedKeepsOddLevelsAndNeverReadsMissingColumns() {
		IntelScanResult result = new IntelScanResult();
		result.mode = IntelScanMode.COMBINED; result.width = 2; result.depth = 1; result.targetX = -10;
		IntelScanJob job = new IntelScanJob(result.mode);
		final int[] reads = {0};
		IntelProjectionScanner.Access world = new IntelProjectionScanner.Access() {
			public boolean loaded(int x, int z) { return x == -11; }
			public int cell(int x, int y, int z) {
				assertEquals(-11, x); reads[0]++;
				return y == 5 ? 255 : 0;
			}
		};
		IntelProjectionScanner scanner = new IntelProjectionScanner();
		scanner.process(world, job, result, 1);
		assertEquals(1, job.phaseCursor); assertEquals(256, reads[0]);
		assertEquals(255, result.projection.mask(0, 5, 0));
		scanner.process(world, job, result, 1);
		assertEquals(256, reads[0]); assertEquals(1, result.projection.coveredColumns());
		assertEquals(0, result.projection.mask(1, 5, 0));
		assertEquals(5, result.projection.minY);
	}

	@Test public void onlyCombinedResultsCaptureAndPersistProjectionData() {
		IntelScanResult result = new IntelScanResult(); result.mode = IntelScanMode.COMBINED;
		result.projection = new IntelProjection(-32, 17, 64, 64);
		result.projection.set(63, 255, 63, 255, false);
		NBTTagCompound tag = new NBTTagCompound(); result.writeToNBT(tag);
		IntelScanResult restored = IntelScanResult.readFromNBT(tag);
		assertEquals(result.projection.id, restored.projection.id);
		assertEquals(255, restored.projection.mask(63, 255, 63));
		tag.setString("mode", "SURFACE");
		assertNull(IntelScanResult.readFromNBT(tag).projection);
		new IntelProjectionScanner().process(null, new IntelScanJob(IntelScanMode.SURFACE), new IntelScanResult(), 1);
		assertNull(IntelScanResult.readFromNBT(new NBTTagCompound()).projection);
	}

	@Test public void slabBoundsKeepHalfBlockDetail() {
		assertEquals(15, IntelProjectionScanner.mask(0, 0, 0, 1, .5, 1));
		assertEquals(240, IntelProjectionScanner.mask(0, .5, 0, 1, 1, 1));
		assertEquals(255, IntelProjectionScanner.mask(0, 0, 0, 1, 1, 1));
	}

	@Test public void cableShapeNeverFollowsAProxyIntoItsUnloadedCoreChunk() {
		PowerCableBox cable=new PowerCableBox(Material.iron) {
			@Override public void setBlockBoundsBasedOnState(IBlockAccess world,int x,int y,int z) {
				throw new AssertionError("Cable at X=14 must not follow proxy X=15 to unloaded core X=16");
			}
		};
		assertEquals(255,IntelProjectionScanner.shape(cable,null,14,5,8,true));
	}

	@Test public void meshMergesAdjacentCubesAndPreservesAnInteriorVoid() {
		IntelProjection p = new IntelProjection(0, 0, 3, 3);
		p.set(0, 4, 0, 255, false); p.set(1, 4, 0, 255, false);
		List<IntelProjectionMesh.Quad> quads = mesh(p, 255, -1, 0, false);
		assertEquals(6, quads.size());
		assertEquals(10, area(quads), .001);
		p = new IntelProjection(0, 0, 3, 3);
		for(int x=0;x<3;x++) for(int y=0;y<3;y++) for(int z=0;z<3;z++)
			if(x!=1 || y!=1 || z!=1) p.set(x,y,z,255,false);
		quads = mesh(p,255,-1,0,false);
		assertEquals(60, area(quads), .001); // 54 exterior + 6 room walls, never a filled bounding box.
		assertEquals(46, area(mesh(p,1,-1,0,false)), .001); // 42 for a solid 3x2x3 box, plus four exposed room sides.
	}

	@Test public void windowsRemainDistinctFromTheSurroundingFacade() {
		IntelProjection p=new IntelProjection(0,0,2,1);
		p.set(0,4,0,255,false);p.set(1,4,0,255,false,true);
		int solid=0,glass=0;
		for(IntelProjectionMesh.Quad q:mesh(p,255,-1,0,false)) { if(q.glass) glass++;else solid++; }
		assertEquals(6,solid);assertEquals(5,glass); // The solid frame's side remains visible through the window.
		IntelProjection restored=IntelProjection.readFromNBT(p.writeToNBT());
		assertTrue(restored.glass(1,4,0));assertFalse(restored.glass(0,4,0));
	}

	@Test public void terrainAndCoordinateCutsDoNotMoveTheRemainingGeometry() {
		IntelProjection p = new IntelProjection(507,1709,2,1);
		p.set(0,5,0,255,false); p.set(1,5,0,255,true);
		assertEquals(6, area(mesh(p,255,-1,0,false)), .001);
		assertEquals(10, area(mesh(p,255,-1,0,true)), .001);
		assertEquals(6, area(mesh(p,255,0,507,true)), .001);
		for(IntelProjectionMesh.Quad q : mesh(p,255,0,507,true)) {
			assertTrue(q.x >= 0 && q.x <= 1); assertTrue(q.y >= 5 && q.y <= 6);
		}
	}

	@Test public void aTerrainOnlySceneStillFitsTheEntireProjectionHeight() {
		IntelProjection p=new IntelProjection(0,0,64,64);p.set(0,200,0,255,true);
		assertEquals(201,p.bounds(true)[4],0);
		p.set(7,20,8,255,false);
		assertArrayEquals(new double[]{7,20,8,8,21,9},p.bounds(false),0);
		assertEquals(201,p.bounds(true)[4],0);
	}

	@Test public void nativeReferencesCannotSelectAnotherScanOrSatelliteMode() {
		IntelScanResult r=new IntelScanResult(); r.mode=IntelScanMode.COMBINED; r.dimension=7;
		r.projection=new IntelProjection(0,0,1,1);
		assertTrue(IntelProjection.matches(r,7,r.projection.id));
		assertFalse(IntelProjection.matches(r,0,r.projection.id));
		assertFalse(IntelProjection.matches(r,7,"previous scan"));
		r.mode=IntelScanMode.SUBSURFACE;
		assertFalse(IntelProjection.matches(r,7,r.projection.id));
	}

	@Test public void viewCutsUseWorldCoordinatesAndRejectInvalidInput() {
		IntelProjection p=new IntelProjection(500,1700,64,64); p.set(8,5,9,255,false); p.set(8,53,9,255,false);
		IntelProjectionView v=new IntelProjectionView();
		v.configure("view","interior",p,3); assertEquals(52,v.floor);
		v.configure("cut","z:1709",p,3); assertEquals(2,v.cutAxis); assertEquals(1709,v.cut);
		v.configure("floor","20",p,3); assertEquals(20,v.floor);
		v.configure("view","exterior",p,3); assertEquals(255,v.floor); assertEquals(-1,v.cutAxis);
		for(String[] bad:new String[][]{{"floor","256"},{"scale","NaN"},{"select","4"},{"terrain","yes"}}) {
			try { v.configure(bad[0],bad[1],p,3); fail("Accepted invalid "+bad[0]); } catch(IllegalArgumentException expected) { }
		}
	}

	private static List<IntelProjectionMesh.Quad> mesh(IntelProjection p, int floor, int axis, int cut, boolean terrain) {
		IntelProjectionMesh.Builder builder = new IntelProjectionMesh.Builder(p, floor, axis, cut, terrain);
		while(!builder.step(8)) { }
		assertFalse(builder.truncated);
		return builder.quads;
	}
	private static double area(List<IntelProjectionMesh.Quad> quads) {
		double area=0; for(IntelProjectionMesh.Quad q:quads) area+=q.width*q.height; return area;
	}
}
