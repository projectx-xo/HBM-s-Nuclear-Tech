package com.hbm.saveddata.satellites.intel;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;

public class IntelFinding {
	public IntelClassification classification = IntelClassification.STRUCTURE;
	public int minX;
	public int minY;
	public int minZ;
	public int maxX;
	public int maxY;
	public int maxZ;
	public float confidence;
	public boolean reinforced;
	public boolean machinery;
	public boolean power;
	public boolean launchInfrastructure;
	public boolean communications;
	public String targetType = "";
	public String targetId = "";
	public int targetCount;
	// Only needed while scanning, to recognize entities moving between chunks.
	public UUID sourceEntityId;

	public NBTTagCompound writeToNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setString("classification", classification.name());
		nbt.setInteger("minX", minX);
		nbt.setInteger("minY", minY);
		nbt.setInteger("minZ", minZ);
		nbt.setInteger("maxX", maxX);
		nbt.setInteger("maxY", maxY);
		nbt.setInteger("maxZ", maxZ);
		nbt.setFloat("confidence", Math.max(0F, Math.min(1F, confidence)));
		nbt.setBoolean("reinforced", reinforced);
		nbt.setBoolean("machinery", machinery);
		nbt.setBoolean("power", power);
		nbt.setBoolean("launch", launchInfrastructure);
		nbt.setBoolean("communications", communications);
		nbt.setString("targetType", targetType);
		nbt.setString("targetId", targetId);
		nbt.setInteger("targetCount", targetCount);
		return nbt;
	}

	public static IntelFinding readFromNBT(NBTTagCompound nbt) {
		IntelFinding finding = new IntelFinding();
		try {
			finding.classification = IntelClassification.valueOf(nbt.getString("classification"));
		} catch(Exception ignored) { }
		finding.minX = nbt.getInteger("minX");
		finding.minY = nbt.getInteger("minY");
		finding.minZ = nbt.getInteger("minZ");
		finding.maxX = nbt.getInteger("maxX");
		finding.maxY = nbt.getInteger("maxY");
		finding.maxZ = nbt.getInteger("maxZ");
		finding.confidence = Math.max(0F, Math.min(1F, nbt.getFloat("confidence")));
		finding.reinforced = nbt.getBoolean("reinforced");
		finding.machinery = nbt.getBoolean("machinery");
		finding.power = nbt.getBoolean("power");
		finding.launchInfrastructure = nbt.getBoolean("launch");
		finding.communications = nbt.getBoolean("communications");
		finding.targetType = nbt.getString("targetType");
		finding.targetId = nbt.getString("targetId");
		finding.targetCount = Math.max(0, nbt.getInteger("targetCount"));
		return finding;
	}
}
