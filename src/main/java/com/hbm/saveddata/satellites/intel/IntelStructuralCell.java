package com.hbm.saveddata.satellites.intel;

import net.minecraft.nbt.NBTTagCompound;

public class IntelStructuralCell {
	public int x;
	public int y;
	public int z;
	public String registryId = "";
	public int metadata;
	public float blastResistance;
	public IntelResistanceBand resistanceBand = IntelResistanceBand.LIGHT;

	public IntelStructuralCell() { }

	public IntelStructuralCell(int x, int y, int z, String registryId, int metadata, float blastResistance) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.registryId = registryId == null ? "" : registryId;
		this.metadata = metadata;
		this.blastResistance = blastResistance;
		this.resistanceBand = IntelResistanceBand.fromResistance(blastResistance);
	}

	public NBTTagCompound writeToNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", x);
		nbt.setInteger("y", y);
		nbt.setInteger("z", z);
		nbt.setString("registryId", registryId);
		nbt.setInteger("metadata", metadata);
		nbt.setFloat("blastResistance", blastResistance);
		nbt.setString("resistanceBand", resistanceBand.name());
		return nbt;
	}

	public static IntelStructuralCell readFromNBT(NBTTagCompound nbt) {
		IntelStructuralCell cell = new IntelStructuralCell();
		cell.x = nbt.getInteger("x");
		cell.y = nbt.getInteger("y");
		cell.z = nbt.getInteger("z");
		cell.registryId = nbt.getString("registryId");
		cell.metadata = nbt.getInteger("metadata");
		cell.blastResistance = nbt.getFloat("blastResistance");
		try {
			cell.resistanceBand = IntelResistanceBand.valueOf(nbt.getString("resistanceBand"));
		} catch(Exception ignored) {
			cell.resistanceBand = IntelResistanceBand.fromResistance(cell.blastResistance);
		}
		return cell;
	}
}
