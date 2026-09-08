package com.hbm.tileentity.bomb;

import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Arguments;

public class LaunchPadMappingCallbackTest {
	@Test public void ordinaryPadExposesMapping() throws Exception {
		final Object[] result = {2};
		TileEntityLaunchPad pad = new TileEntityLaunchPad() {
			@Override public Object[] getInventoryMapping(Context c, Arguments a) { return result; }
		};
		assertTrue(Arrays.asList(pad.methods()).contains("getInventoryMapping"));
		assertSame(result, pad.invoke("getInventoryMapping", null, null));
	}
	@Test public void customPadExposesMapping() throws Exception {
		final Object[] result = {4};
		TileEntityLaunchTable pad = new TileEntityLaunchTable() {
			@Override public Object[] getInventoryMapping(Context c, Arguments a) { return result; }
		};
		assertTrue(Arrays.asList(pad.methods()).contains("getInventoryMapping"));
		assertSame(result, pad.invoke("getInventoryMapping", null, null));
	}
}
