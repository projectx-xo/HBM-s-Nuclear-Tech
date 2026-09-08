package com.hbm.tileentity.bomb;

import static org.junit.Assert.*;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineRadarLarge;
import com.hbm.tileentity.machine.TileEntityMachineRadarRadome;
import com.hbm.util.Tuple.Triplet;
import api.hbm.entity.RadarEntry;
import api.hbm.entity.IRadarDetectableNT.RadarScanParams;
import net.minecraft.entity.Entity;

public class RadomeDetectionTest {
	@Test public void payloadIdentificationUsesWarheadNotMissileTier() {
		Object radome = new TileEntityMachineRadarRadome();
		Entity nuclear = new com.hbm.entity.missile.EntityMissileTier4.EntityMissileNuclear(null) { @Override protected void entityInit() { } };
		Entity thermo = new com.hbm.entity.missile.EntityMissileTier4.EntityMissileMirv(null) { @Override protected void entityInit() { } };
		Entity volcano = new com.hbm.entity.missile.EntityMissileTier4.EntityMissileVolcano(null) { @Override protected void entityInit() { } };
		assertEquals("NUCLEAR", com.hbm.entity.missile.MissilePayload.identify(radome,nuclear));
		assertEquals("THERMONUCLEAR", com.hbm.entity.missile.MissilePayload.identify(radome,thermo));
		assertEquals("UNKNOWN", com.hbm.entity.missile.MissilePayload.identify(radome,volcano));
		assertEquals("UNKNOWN", com.hbm.entity.missile.MissilePayload.identify(new TileEntityMachineRadarLarge(),nuclear));
		assertEquals("THERMONUCLEAR", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.BUSTER_THERMONUCLEAR));
		assertEquals("THERMONUCLEAR", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.TX));
		assertEquals("NUCLEAR", com.hbm.entity.missile.MissilePayload.classifyWarhead(com.hbm.items.weapon.ItemCustomMissilePart.WarheadType.NUCLEAR));
		assertEquals("UNKNOWN", com.hbm.entity.missile.MissilePayload.classifyWarhead(null));
	}
	private static class Stealth extends EntityMissileStealth {
		Stealth() { super(null); }
		@Override protected void entityInit() { }
	}
	@Test public void onlyRadomeDetectsStealthAndMissileFilterStillApplies() {
		List<Function<Triplet<Entity,Object,RadarScanParams>,RadarEntry>> old = new ArrayList<>(TileEntityMachineRadarNT.converters);
		try {
			TileEntityMachineRadarNT.converters.clear();
			TileEntityMachineRadarNT.registerConverters();
			Stealth target = new Stealth();
			Object[] radars = {new TileEntityMachineRadarNT(),new TileEntityMachineRadarLarge(),new TileEntityMachineRadarRadome()};
			for(int i=0;i<radars.length;i++) {
				for(boolean missiles:new boolean[] {false,true}) {
					boolean detected=false;
					for(Function<Triplet<Entity,Object,RadarScanParams>,RadarEntry> converter:TileEntityMachineRadarNT.converters) {
						if(converter.apply(new Triplet<Entity,Object,RadarScanParams>(target,radars[i],new RadarScanParams(missiles,false,false,false)))!=null) detected=true;
					}
					assertEquals(i==2 && missiles,detected);
				}
			}
			assertEquals(4000,new TileEntityMachineRadarRadome().getRange());
			assertEquals("ntm_radome",new TileEntityMachineRadarRadome().getComponentName());
			assertEquals("ntm_radar",new TileEntityMachineRadarNT().getComponentName());
			assertEquals("ntm_radar",new TileEntityMachineRadarLarge().getComponentName());
		} finally {
			TileEntityMachineRadarNT.converters.clear();TileEntityMachineRadarNT.converters.addAll(old);
		}
	}
}
