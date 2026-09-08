package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import org.junit.Test;
import net.minecraft.nbt.NBTTagCompound;

public class IntelProjectionLayersTest {
	@Test public void inspectionControlsDoNotChangeTheIntelligenceLayer() {
		IntelProjection p=new IntelProjection(500,1700,3,1);
		IntelProjectionView v=new IntelProjectionView();
		assertEquals("combined",v.mode);
		v.configure("view","subsurface",p,0);
		v.configure("floor","45",p,0);
		v.configure("cut","x:501",p,0);
		assertEquals("subsurface",v.mode);assertEquals(45,v.floor);assertEquals(0,v.cutAxis);
		v.configure("cut","none",p,0);
		assertEquals("subsurface",v.mode);assertEquals(-1,v.cutAxis);
		NBTTagCompound n=new NBTTagCompound();v.write(n);
		IntelProjectionView restored=new IntelProjectionView();restored.read(n);
		assertEquals("subsurface",restored.mode);assertEquals(45,restored.floor);
	}
	@Test public void oldSavedViewsKeepTheirCutsAndBecomeCombinedLayers() {
		for(String old:new String[]{"exterior","interior","cutaway"}) {
			NBTTagCompound n=new NBTTagCompound();n.setString("view",old);n.setInteger("floor",42);n.setInteger("cutAxis",2);n.setInteger("cut",1701);
			IntelProjectionView v=new IntelProjectionView();v.read(n);
			assertEquals("combined",v.mode);assertEquals(42,v.floor);assertEquals(2,v.cutAxis);assertEquals(1701,v.cut);
		}
	}
	@Test public void roofDoesNotBecomeGroundAndBuriedStructuresKeepTheirCoordinates() {
		IntelProjection p=new IntelProjection(500,1700,3,1);
		for(int x=0;x<3;x++) { p.set(x,63,0,255,true);p.setBlock(x,63,0,"minecraft:dirt",0); }
		p.set(1,80,0,255,false);p.set(1,70,0,255,false); // Surface roof and wall.
		p.set(1,50,0,255,false);p.set(1,55,0,255,false); // Bunker wall and ceiling.
		assertEquals(63,p.groundLevel(1,0));
		IntelProjectionView v=new IntelProjectionView();v.configure("view","surface",p,0);
		IntelProjectionBlockAccess surface=new IntelProjectionBlockAccess(p,v);
		assertTrue(surface.visible(1,80,0));assertTrue(surface.visible(1,70,0));assertFalse(surface.visible(1,50,0));
		v.configure("view","subsurface",p,0);
		IntelProjectionBlockAccess below=new IntelProjectionBlockAccess(p,v);
		assertTrue(below.visible(1,50,0));assertTrue(below.visible(1,55,0));assertFalse(below.visible(1,80,0));
		assertFalse(below.visible(0,63,0));assertFalse(below.visible(-1,50,0));
		assertEquals(55,p.highestVisibleLevel(v.mode,false));
		v.configure("floor","50",p,0);below=new IntelProjectionBlockAccess(p,v);
		assertTrue(below.visible(1,50,0));assertFalse(below.visible(1,55,0));
		v.configure("view","combined",p,0);
		IntelProjectionBlockAccess both=new IntelProjectionBlockAccess(p,v);
		assertTrue(both.visible(1,80,0));assertTrue(both.visible(1,50,0));
		assertFalse(surface.visible(1,50,0)); // Incremental render retained its original layer.
		IntelProjection restored=IntelProjection.readFromNBT(p.writeToNBT());
		assertEquals(63,restored.groundLevel(1,0));
	}
	@Test public void hillsHatchesAndFindingsUseTheLocalGroundLevel() {
		IntelProjection p=new IntelProjection(500,1700,4,1);
		p.set(0,60,0,255,true);p.set(3,70,0,255,true);
		p.set(1,60,0,255,false);p.set(2,70,0,255,false); // Hatches on opposite slopes.
		assertEquals(60,p.groundLevel(1,0));assertEquals(70,p.groundLevel(2,0));
		assertTrue(p.inLayer("surface",1,60,0));assertFalse(p.inLayer("subsurface",1,60,0));
		assertTrue(p.inLayer("subsurface",2,65,0));assertFalse(p.inLayer("surface",2,65,0));
		IntelFinding f=new IntelFinding();f.minX=f.maxX=502;f.minZ=f.maxZ=1700;f.minY=50;f.maxY=65;
		assertFalse(p.findingInLayer("surface",f));assertTrue(p.findingInLayer("subsurface",f));
		f.maxY=72;assertTrue(p.findingInLayer("surface",f));assertTrue(p.findingInLayer("subsurface",f));
	}
	@Test public void navigationSkipsOtherLayersWithoutRenumberingScanFindings() {
		IntelScanResult scan=new IntelScanResult();scan.projection=new IntelProjection(500,1700,1,1);
		scan.projection.set(0,63,0,255,true);
		for(int y:new int[]{70,50,75,55}) {
			IntelFinding f=new IntelFinding();f.minX=f.maxX=500;f.minZ=f.maxZ=1700;f.minY=f.maxY=y;scan.findings.add(f);
		}
		com.hbm.tileentity.machine.TileEntityIntelProjector tile=new com.hbm.tileentity.machine.TileEntityIntelProjector();tile.displayed=scan;
		tile.view.configure("view","subsurface",scan.projection,4);
		assertEquals(2,tile.visibleFindingCount());assertEquals(2,tile.nextVisibleFinding(1));assertEquals(4,tile.nextVisibleFinding(-1));
		tile.view.selected=4;assertEquals(2,tile.nextVisibleFinding(1));
		tile.view.configure("view","surface",scan.projection,4);assertEquals(0,tile.view.selected);assertEquals(1,tile.nextVisibleFinding(1));
	}

}
