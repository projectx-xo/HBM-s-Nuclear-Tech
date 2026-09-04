package com.hbm.saveddata.satellites.intel;

import net.minecraft.nbt.NBTTagCompound;

public class IntelStructuralSummary {
	public String dominantMaterial = "";
	public float averageResistance;
	public float maxResistance;
	public int wallThickness;
	public int roofThickness;
	public int floorThickness;
	public int weakPointCount;

	public NBTTagCompound writeToNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("dominantMaterial", dominantMaterial);
		nbt.setFloat("averageResistance", averageResistance);
		nbt.setFloat("maxResistance", maxResistance);
		nbt.setInteger("wallThickness", wallThickness);
		nbt.setInteger("roofThickness", roofThickness);
		nbt.setInteger("floorThickness", floorThickness);
		nbt.setInteger("weakPointCount", weakPointCount);
		return nbt;
	}

	public static IntelStructuralSummary readFromNBT(NBTTagCompound nbt) {
		IntelStructuralSummary summary = new IntelStructuralSummary();
		summary.dominantMaterial = nbt.getString("dominantMaterial");
		summary.averageResistance = nbt.getFloat("averageResistance");
		summary.maxResistance = nbt.getFloat("maxResistance");
		summary.wallThickness = nbt.getInteger("wallThickness");
		summary.roofThickness = nbt.getInteger("roofThickness");
		summary.floorThickness = nbt.getInteger("floorThickness");
		summary.weakPointCount = nbt.getInteger("weakPointCount");
		return summary;
	}
}
