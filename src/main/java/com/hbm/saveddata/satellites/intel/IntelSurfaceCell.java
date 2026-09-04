package com.hbm.saveddata.satellites.intel;

import net.minecraft.nbt.NBTTagCompound;

public class IntelSurfaceCell {
	public int x;
	public int y;
	public int z;
	public IntelClassification classification = IntelClassification.NATURAL;
	public boolean structural;

	public IntelSurfaceCell() { }

	public IntelSurfaceCell(int x, int y, int z, IntelClassification classification, boolean structural) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.classification = classification == null ? IntelClassification.NATURAL : classification;
		this.structural = structural;
	}

	public NBTTagCompound writeToNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("x", x);
		nbt.setInteger("y", y);
		nbt.setInteger("z", z);
		nbt.setString("classification", classification.name());
		nbt.setBoolean("structural", structural);
		return nbt;
	}

	public static IntelSurfaceCell readFromNBT(NBTTagCompound nbt) {
		IntelSurfaceCell cell = new IntelSurfaceCell();
		cell.x = nbt.getInteger("x");
		cell.y = nbt.getInteger("y");
		cell.z = nbt.getInteger("z");
		try {
			cell.classification = IntelClassification.valueOf(nbt.getString("classification"));
		} catch(Exception ignored) { }
		cell.structural = nbt.getBoolean("structural");
		return cell;
	}
}
