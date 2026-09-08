package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;
import org.junit.Test;
import com.hbm.tileentity.TileEntityDoorGeneric;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public class IntelProjectionMachineTest {
	@Test public void hatchPoseAndDummySuppressionSurviveSnapshotRoundTrip() {
		TileEntityDoorGeneric door=new TileEntityDoorGeneric();door.openTicks=47;
		IntelProjection p=new IntelProjection(500,1700,8,8);
		IntelProjectionMachine model=IntelProjectionMachine.capture(3,60,4,14,2,door);
		p.machines.put((4*8+3)*256+60,model);p.setModelCell(2,60,4);
		door.openTicks=0;
		IntelProjection restored=IntelProjection.readFromNBT(p.writeToNBT());
		assertNotNull(restored);assertEquals(1,restored.machines.size());
		IntelProjectionMachine actual=restored.machines.values().iterator().next();
		assertEquals(47,actual.visual.getInteger("open"));assertEquals(14,actual.meta);
		assertTrue(restored.modelCell(2,60,4));assertFalse(restored.modelCell(1,60,4));
		NBTTagCompound old=p.writeToNBT();old.removeTag("models");old.removeTag("modelCells");
		assertTrue(IntelProjection.readFromNBT(old).machines.isEmpty());
	}
	@Test public void rejectsDuplicateModelsAndMalformedSuppressionData() {
		IntelProjection p=new IntelProjection(0,0,1,1);
		NBTTagCompound n=p.writeToNBT();n.setByteArray("modelCells",new byte[1]);
		assertNull(IntelProjection.readFromNBT(n));
		n=p.writeToNBT();NBTTagList models=new NBTTagList();
		NBTTagCompound model=new NBTTagCompound();model.setInteger("kind",1);
		models.appendTag(model);models.appendTag(model.copy());n.setTag("models",models);
		assertNull(IntelProjection.readFromNBT(n));
	}
	@Test public void visualReaderRejectsUnboundedDataAndDoesNotRestoreMachineControlNbt() {
		NBTTagCompound n=new NBTTagCompound();n.setInteger("kind",4);n.setInteger("y",60);
		n.setString("target","not visual");n.setTag("Items",new NBTTagList());n.setFloat("angle",30);
		IntelProjectionMachine m=IntelProjectionMachine.read(n,8,8);
		assertNotNull(m);assertFalse(m.visual.hasKey("target"));assertFalse(m.visual.hasKey("Items"));
		n.setFloat("angle",Float.NaN);assertNull(IntelProjectionMachine.read(n,8,8));
		n.setFloat("angle",30);n.setInteger("x",8);assertNull(IntelProjectionMachine.read(n,8,8));
		n.setInteger("x",0);n.setInteger("height",100000);assertNull(IntelProjectionMachine.read(n,8,8));
	}
}
