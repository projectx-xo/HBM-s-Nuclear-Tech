package com.hbm.tileentity.machine;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;

public class SatelliteProjectionCallbackTest {
	@Test
	public void exposesAndDispatchesProjectionReference() throws Exception {
		final Object[] reference = {true, 42, 0, "11111111-1111-1111-1111-111111111111"};
		TileEntityMachineSatLink station = new TileEntityMachineSatLink() {
			@Override
			public Object[] intelProjection(Context context, Arguments args) {
				return reference;
			}
		};
		assertTrue(Arrays.asList(station.methods()).contains("intelProjection"));
		assertSame(reference, station.invoke("intelProjection", null, null));
	}
}
