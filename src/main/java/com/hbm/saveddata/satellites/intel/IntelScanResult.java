package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;

public class IntelScanResult {
	public static final int MAX_FINDINGS = 128;
	public static final int MAX_SURFACE_CELLS = 4096;
	public static final int MAX_SUBSURFACE_CELLS = 8192;
	public static final int MAX_STRUCTURAL_CELLS = 8192;
	public static final int PAGE_SIZE = 64;

	public IntelScanMode mode = IntelScanMode.SURFACE;
	public int targetX;
	public int targetZ;
	public int width = 64;
	public int depth = 64;
	public int dimension;
	public long startedAt;
	public long completedAt;
	public int coveredColumns;
	public int totalColumns = 4096;
	public final List<IntelFinding> findings = new ArrayList<IntelFinding>();
	public final List<IntelSurfaceCell> surfaceCells = new ArrayList<IntelSurfaceCell>();
	public final List<IntelSurfaceCell> subsurfaceCells = new ArrayList<IntelSurfaceCell>();
	public final List<IntelStructuralCell> structuralCells = new ArrayList<IntelStructuralCell>();
	public IntelStructuralSummary structuralSummary;

	public int getCoveragePercent() {
		if(totalColumns <= 0) return 0;
		return Math.max(0, Math.min(100, (int) Math.round(coveredColumns * 100D / totalColumns)));
	}

	public String getSummary() {
		StringBuilder builder = new StringBuilder();
		builder.append(mode.name()).append(';');
		builder.append(targetX).append(';').append(targetZ).append(';');
		builder.append(getCoveragePercent()).append('%').append(';');
		builder.append("FINDINGS=").append(findings.size());
		if(structuralSummary != null) {
			builder.append(";AVG_RES=").append(String.format(java.util.Locale.US, "%.2f", structuralSummary.averageResistance));
			builder.append(";MAX_RES=").append(String.format(java.util.Locale.US, "%.2f", structuralSummary.maxResistance));
			builder.append(";WEAK=").append(structuralSummary.weakPointCount);
		}
		return builder.toString();
	}

	public void writeToNBT(NBTTagCompound nbt) {
		IntelResultCodec.writeResult(this, nbt);
	}

	public static IntelScanResult readFromNBT(NBTTagCompound nbt) {
		return IntelResultCodec.readResult(nbt);
	}
}
