package com.hbm.tileentity.machine;

import java.io.IOException;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.lib.Library;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.util.fauxpointtwelve.DirPos;
import net.minecraft.util.AxisAlignedBB;

public class TileEntityMachineRadarRadome extends TileEntityMachineRadarNT {
	public static int radomeRange = 4_000;
	@Override @cpw.mods.fml.common.Optional.Method(modid="OpenComputers")
	public String getComponentName() { return "ntm_radome"; }
	@Override public String getConfigName() { return "radar_radome"; }
	@Override public int getRange() { return radomeRange; }
	@Override public void readIfPresent(JsonObject obj) {
		radomeRange = Math.max(1, IConfigurableMachine.grab(obj, "I:radomeRange", radomeRange));
	}
	@Override public void writeConfig(JsonWriter writer) throws IOException {
		writer.name("I:radomeRange").value(radomeRange);
	}
	@Override public DirPos[] getConPos() {
		return new DirPos[] {new DirPos(xCoord + 4, yCoord, zCoord, Library.POS_X),
			new DirPos(xCoord - 4, yCoord, zCoord, Library.NEG_X),
			new DirPos(xCoord, yCoord, zCoord + 4, Library.POS_Z),
			new DirPos(xCoord, yCoord, zCoord - 4, Library.NEG_Z)};
	}
	@Override public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xCoord - 3, yCoord, zCoord - 3, xCoord + 4, yCoord + 13, zCoord + 4);
	}
}
