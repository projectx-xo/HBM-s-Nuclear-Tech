package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import org.junit.Test;
import net.minecraft.world.ChunkCoordIntPair;

public class IntelScanChunksTest {
	static class Loader extends IntelScanChunks {
		int loaded, released, acquired;
		boolean fail;
		Loader() { super(null, 3499, 1001); }
		@Override protected void acquire(int count) { acquired++; assertTrue(count <= 25); }
		@Override protected void load(ChunkCoordIntPair chunk) { loaded++; if(fail) throw new IllegalStateException(); }
		@Override protected void release() { released++; }
	}
	@Test public void footprintCoversNegativeAndUnalignedCoordinates() {
		assertEquals(16, IntelScanChunks.footprint(0, 0).size());
		assertEquals(25, IntelScanChunks.footprint(-1, -1).size());
		assertTrue(IntelScanChunks.footprint(-1, -1).contains(new ChunkCoordIntPair(-3, -3)));
		assertTrue(IntelScanChunks.footprint(-1, -1).contains(new ChunkCoordIntPair(1, 1)));
	}
	@Test public void loadsOnePerTickAndKeepsTicketUntilClosed() {
		Loader loader = new Loader();
		for(int i=1;i<=25;i++) { assertFalse(loader.tick()); assertEquals(i, loader.loaded); }
		assertTrue(loader.tick()); assertTrue(loader.tick());
		assertEquals(1, loader.acquired); assertEquals(0, loader.released);
		loader.close(); loader.close(); assertEquals(1, loader.released);
	}
	@Test public void failedLoadReleasesTicket() {
		Loader loader = new Loader(); loader.fail=true;
		try { loader.tick(); fail("Expected load failure"); } catch(IllegalStateException expected) { }
		assertEquals(1, loader.released); loader.close(); assertEquals(1, loader.released);
	}
}
