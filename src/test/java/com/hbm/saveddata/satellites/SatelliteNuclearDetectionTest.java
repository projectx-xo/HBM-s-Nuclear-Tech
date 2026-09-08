package com.hbm.saveddata.satellites;

import static org.junit.Assert.*;
import org.junit.Test;
import com.hbm.items.special.ItemSatellite.EnumSatType;

public class SatelliteNuclearDetectionTest {
	@Test public void stableRegistration() {
		XSatelliteRegistry.registerIds();
		assertEquals(18, EnumSatType.NUCLEAR_DETECTION.ordinal());
		assertEquals(SatelliteNuclearDetection.class, XSatelliteRegistry.idToClass.get(17));
		assertEquals(SatelliteSatComRelay.class, XSatelliteRegistry.idToClass.get(16));
	}
	@Test public void journalPagesAndRetainsExactCoordinates() {
		SatelliteNuclearDetection.Journal j = new SatelliteNuclearDetection.Journal();
		for(int i=0;i<10;i++) j.add("EXPLOSION","NUCLEAR",12.25,null,30.5,0,100);
		Object[] page=j.page("",0,100);
		assertEquals(8,page[2]);assertEquals(true,page[5]);
		assertTrue(((String)page[3]).startsWith("1,EXPLOSION,NUCLEAR,12.25,?,30.5,0,100"));
		Object[] next=j.page((String)page[1],8,100);
		assertEquals(10,next[2]);assertEquals(false,next[5]);
		assertEquals(page[3],j.page((String)page[1],0,100)[3]);
		assertEquals(false,j.page((String)page[1],11,100)[0]);
	}
	@Test public void journalBoundsAndExpiryAreExplicit() {
		SatelliteNuclearDetection.Journal j = new SatelliteNuclearDetection.Journal();
		for(int i=0;i<300;i++)j.add("MISSILE","NUCLEAR",1,2D,3,0,100);
		Object[] page=j.page("",0,100);
		assertEquals(44,page[4]);assertEquals(52,page[2]);
		Object[] expired=j.page((String)page[1],52,12101);
		assertEquals("",expired[3]);assertEquals(300,expired[2]);assertEquals(248,expired[4]);
		j.add("MISSILE","NUCLEAR",Double.NaN,2D,3,0,12200);
		assertEquals(300,j.page((String)page[1],300,12200)[2]);
	}
	@Test public void conventionalWarheadsAreExplicitAndSpecialsStayUnknown() {
		assertEquals("CONVENTIONAL", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.HE));
		assertEquals("NUCLEAR", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.NUCLEAR));
		assertEquals("THERMONUCLEAR", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.TX));
		assertEquals("UNKNOWN", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.CUSTOM0));
	}
}
