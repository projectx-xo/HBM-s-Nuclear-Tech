package com.hbm.saveddata.satellites.intel;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class IntelResultCodec {

	private IntelResultCodec() { }

	public static void writeResult(IntelScanResult result, NBTTagCompound nbt) {
		if(result == null || nbt == null) return;
		nbt.setString("mode", result.mode.name());
		nbt.setInteger("targetX", result.targetX);
		nbt.setInteger("targetZ", result.targetZ);
		nbt.setInteger("width", result.width);
		nbt.setInteger("depth", result.depth);
		nbt.setInteger("dimension", result.dimension);
		nbt.setLong("startedAt", result.startedAt);
		nbt.setLong("completedAt", result.completedAt);
		nbt.setInteger("coveredColumns", result.coveredColumns);
		nbt.setInteger("totalColumns", result.totalColumns);

		NBTTagList findings = new NBTTagList();
		for(int i = 0; i < Math.min(result.findings.size(), IntelScanResult.MAX_FINDINGS); i++) findings.appendTag(result.findings.get(i).writeToNBT());
		nbt.setTag("findings", findings);

		NBTTagList surface = new NBTTagList();
		for(int i = 0; i < Math.min(result.surfaceCells.size(), IntelScanResult.MAX_SURFACE_CELLS); i++) surface.appendTag(result.surfaceCells.get(i).writeToNBT());
		nbt.setTag("surfaceCells", surface);

		NBTTagList subsurface = new NBTTagList();
		for(int i = 0; i < Math.min(result.subsurfaceCells.size(), IntelScanResult.MAX_SUBSURFACE_CELLS); i++) subsurface.appendTag(result.subsurfaceCells.get(i).writeToNBT());
		nbt.setTag("subsurfaceCells", subsurface);

		NBTTagList structural = new NBTTagList();
		for(int i = 0; i < Math.min(result.structuralCells.size(), IntelScanResult.MAX_STRUCTURAL_CELLS); i++) structural.appendTag(result.structuralCells.get(i).writeToNBT());
		nbt.setTag("structuralCells", structural);

		if(result.structuralSummary != null) nbt.setTag("structuralSummary", result.structuralSummary.writeToNBT());
	}

	public static IntelScanResult readResult(NBTTagCompound nbt) {
		IntelScanResult result = new IntelScanResult();
		if(nbt == null) return result;
		try {
			result.mode = IntelScanMode.valueOf(nbt.getString("mode"));
		} catch(Exception ignored) { }
		result.targetX = nbt.getInteger("targetX");
		result.targetZ = nbt.getInteger("targetZ");
		result.width = Math.max(1, Math.min(64, nbt.getInteger("width")));
		result.depth = Math.max(1, Math.min(64, nbt.getInteger("depth")));
		result.dimension = nbt.getInteger("dimension");
		result.startedAt = nbt.getLong("startedAt");
		result.completedAt = nbt.getLong("completedAt");
		result.coveredColumns = Math.max(0, nbt.getInteger("coveredColumns"));
		result.totalColumns = Math.max(1, nbt.getInteger("totalColumns"));

		NBTTagList findings = nbt.getTagList("findings", 10);
		for(int i = 0; i < Math.min(findings.tagCount(), IntelScanResult.MAX_FINDINGS); i++) result.findings.add(IntelFinding.readFromNBT(findings.getCompoundTagAt(i)));

		NBTTagList surface = nbt.getTagList("surfaceCells", 10);
		for(int i = 0; i < Math.min(surface.tagCount(), IntelScanResult.MAX_SURFACE_CELLS); i++) result.surfaceCells.add(IntelSurfaceCell.readFromNBT(surface.getCompoundTagAt(i)));

		NBTTagList subsurface = nbt.getTagList("subsurfaceCells", 10);
		for(int i = 0; i < Math.min(subsurface.tagCount(), IntelScanResult.MAX_SUBSURFACE_CELLS); i++) result.subsurfaceCells.add(IntelSurfaceCell.readFromNBT(subsurface.getCompoundTagAt(i)));

		NBTTagList structural = nbt.getTagList("structuralCells", 10);
		for(int i = 0; i < Math.min(structural.tagCount(), IntelScanResult.MAX_STRUCTURAL_CELLS); i++) result.structuralCells.add(IntelStructuralCell.readFromNBT(structural.getCompoundTagAt(i)));

		if(nbt.hasKey("structuralSummary")) result.structuralSummary = IntelStructuralSummary.readFromNBT(nbt.getCompoundTag("structuralSummary"));
		return result;
	}
}
