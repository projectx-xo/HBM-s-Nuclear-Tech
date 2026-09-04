package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;

import org.junit.Test;

import net.minecraft.nbt.NBTTagCompound;

public class IntelTargetClassificationTest {
	@Test
	public void recognizesTheActualRegisteredHbmLauncherAndSiloNames() {
		assertEquals("LAUNCHPAD", IntelTargetDetector.typeForBlock("hbm:tile.launch_pad_large"));
		assertEquals("LAUNCH_TABLE", IntelTargetDetector.typeForBlock("hbm:tile.launch_table"));
		assertEquals("COMPACT_LAUNCHER", IntelTargetDetector.typeForBlock("hbm:tile.compact_launcher"));
		assertEquals("SILO_HATCH", IntelTargetDetector.typeForBlock("hbm:tile.silo_hatch_large"));
		assertEquals("RADAR", IntelTargetDetector.typeForBlock("hbm:tile.machine_radar"));
	}

	@Test
	public void doesNotReportMultiblockProxyPortsOrUnrelatedBlocksAsInstallations() {
		assertEquals("", IntelTargetDetector.typeForBlock("hbm:tile.dummy_port_launch_table"));
		assertEquals("", IntelTargetDetector.typeForBlock("minecraft:chest"));
		assertEquals("", IntelTargetDetector.typeForBlock("other:tile.launch_pad"));
	}

	@Test
	public void olderNonCombinedResultsDoNotExposeSpecificTargetsAfterReload() {
		for(IntelScanMode mode : new IntelScanMode[] {IntelScanMode.SURFACE, IntelScanMode.SUBSURFACE}) {
			IntelScanResult old = new IntelScanResult();
			old.mode = mode;
			IntelFinding finding = new IntelFinding();
			finding.classification = IntelClassification.POSSIBLE_SILO;
			finding.launchInfrastructure = true;
			old.findings.add(finding);
			old.surfaceCells.add(new IntelSurfaceCell(0, 64, 0, IntelClassification.LAUNCH_INFRASTRUCTURE, true));
			old.subsurfaceCells.add(new IntelSurfaceCell(0, 21, 0, IntelClassification.LAUNCH_INFRASTRUCTURE, true));
			NBTTagCompound nbt = new NBTTagCompound();
			old.writeToNBT(nbt);
			IntelScanResult loaded = IntelScanResult.readFromNBT(nbt);
			assertEquals(IntelClassification.BUNKER, loaded.findings.get(0).classification);
			assertFalse(loaded.findings.get(0).launchInfrastructure);
			assertEquals(IntelClassification.MACHINERY, loaded.surfaceCells.get(0).classification);
			assertEquals(IntelClassification.MACHINERY, loaded.subsurfaceCells.get(0).classification);
		}
	}

	@Test
	public void onlyCombinedModeNamesMissileSiloAndRadarTargets() {
		for(IntelScanMode mode : new IntelScanMode[] {IntelScanMode.SURFACE, IntelScanMode.SUBSURFACE}) {
			assertEquals(IntelClassification.MACHINERY, IntelClassification.LAUNCH_INFRASTRUCTURE.forMode(mode));
			assertEquals(IntelClassification.MACHINERY, IntelClassification.MISSILE.forMode(mode));
			assertEquals(IntelClassification.MACHINERY, IntelClassification.SILO_HATCH.forMode(mode));
			assertEquals(IntelClassification.BUNKER, IntelClassification.POSSIBLE_SILO.forMode(mode));
			assertEquals(IntelClassification.COMMUNICATIONS, IntelClassification.RADAR.forMode(mode));
		}
		assertEquals(IntelClassification.MISSILE, IntelClassification.MISSILE.forMode(IntelScanMode.COMBINED));
		assertEquals(IntelClassification.POSSIBLE_SILO, IntelClassification.POSSIBLE_SILO.forMode(IntelScanMode.COMBINED));
	}
}
