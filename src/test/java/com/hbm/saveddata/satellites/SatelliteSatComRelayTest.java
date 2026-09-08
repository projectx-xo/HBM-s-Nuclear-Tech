package com.hbm.saveddata.satellites;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hbm.items.special.ItemSatellite.EnumSatType;

public class SatelliteSatComRelayTest {

	@Test
	public void communicationsRelayHasNewStableIdentity() {
		assertEquals(8, EnumSatType.RELAY.ordinal());
		assertEquals(17, EnumSatType.SATCOM_RELAY.ordinal());

		XSatelliteRegistry.idToClass.clear();
		XSatelliteRegistry.registerIds();

		assertEquals(SatelliteRelay.class, XSatelliteRegistry.idToClass.get(5));
		assertEquals(SatelliteSatComRelay.class, XSatelliteRegistry.idToClass.get(16));
		assertEquals("SATCOM_RELAY", new SatelliteSatComRelay().getType());
	}
}
