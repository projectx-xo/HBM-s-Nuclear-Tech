package com.hbm.saveddata.satellites;

import com.hbm.saveddata.satellites.intel.IntelScanJob;
import com.hbm.saveddata.satellites.intel.IntelScanMode;
import com.hbm.saveddata.satellites.intel.IntelScanResult;
import com.hbm.saveddata.satellites.intel.IntelScanState;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class SatelliteIntelligenceBase extends SatelliteBase {

	public static final int SCAN_SIZE = 64;
	public static final int WORK_BUDGET_PER_TICK = 32;

	public static final String CMD_SCAN = "scan";
	public static final String CMD_STATUS = "status";
	public static final String CMD_SUMMARY = "summary";
	public static final String CMD_SURFACE = "surface";
	public static final String CMD_SUBSURFACE = "subsurface";
	public static final String CMD_STRUCTURE = "structure";

	public IntelScanJob activeJob;
	public IntelScanResult lastResult;
	protected IntelScanState lastState = IntelScanState.IDLE;

	public abstract IntelScanMode getScanMode();

	public boolean supportsSurface() {
		return getScanMode() == IntelScanMode.SURFACE || getScanMode() == IntelScanMode.COMBINED;
	}

	public boolean supportsSubsurface() {
		return getScanMode() == IntelScanMode.SUBSURFACE || getScanMode() == IntelScanMode.COMBINED;
	}

	public boolean supportsStructure() {
		return getScanMode() == IntelScanMode.COMBINED;
	}

	public boolean startScan(World world) {
		if(world == null || world.isRemote || activeJob != null) return false;
		activeJob = new IntelScanJob(getScanMode());
		lastState = IntelScanState.SCANNING;
		IntelScanResult result = new IntelScanResult();
		result.mode = getScanMode();
		result.targetX = targetX;
		result.targetZ = targetZ;
		result.width = SCAN_SIZE;
		result.depth = SCAN_SIZE;
		result.dimension = world.provider.dimensionId;
		result.startedAt = world.getTotalWorldTime();
		result.totalColumns = SCAN_SIZE * SCAN_SIZE;
		lastResult = result;
		markDirty();
		return true;
	}

	public String getScanStatus() {
		if(activeJob != null) {
			return activeJob.state.name() + ";" + activeJob.processedWork + ";" + activeJob.totalWork + ";" + activeJob.getProgressPercent();
		}
		int coverage = lastResult == null ? 0 : lastResult.getCoveragePercent();
		return lastState.name() + ";0;0;" + coverage;
	}

	public String getScanSummary() {
		return lastResult == null ? "NO_DATA" : lastResult.getSummary();
	}

	public IntelScanResult getLastResult() {
		return lastResult;
	}

	@Override
	public void onCommandImpl(World world, String... cmd) {
		if(cmd == null || cmd.length == 0) return;
		String op = cmd[0].toLowerCase(java.util.Locale.US);
		if(CMD_SCAN.equals(op)) {
			tx = startScan(world) ? "STARTED" : "BUSY";
			return;
		}
		if(CMD_STATUS.equals(op)) {
			tx = getScanStatus();
			return;
		}
		if(CMD_SUMMARY.equals(op)) {
			tx = getScanSummary();
			return;
		}
		if(CMD_SURFACE.equals(op)) {
			tx = supportsSurface() ? (lastResult == null ? "NO_DATA" : "SURFACE;" + lastResult.surfaceCells.size()) : "UNSUPPORTED";
			return;
		}
		if(CMD_SUBSURFACE.equals(op)) {
			tx = supportsSubsurface() ? (lastResult == null ? "NO_DATA" : "SUBSURFACE;" + lastResult.subsurfaceCells.size()) : "UNSUPPORTED";
			return;
		}
		if(CMD_STRUCTURE.equals(op)) {
			if(!supportsStructure()) {
				tx = "UNSUPPORTED";
			} else if(lastResult == null || lastResult.structuralSummary == null) {
				tx = "NO_DATA";
			} else {
				tx = "STRUCTURE;AVG=" + lastResult.structuralSummary.averageResistance + ";MAX=" + lastResult.structuralSummary.maxResistance + ";WEAK=" + lastResult.structuralSummary.weakPointCount;
			}
		}
	}

	@Override
	public void onUpdateTick(World world) {
		if(activeJob != null) processScanTick(world);
	}

	protected void processScanTick(World world) {
		// Scanner phases are wired by the surface/subsurface/combined implementation tasks.
	}

	protected void completeScan(World world) {
		if(lastResult != null) lastResult.completedAt = world.getTotalWorldTime();
		if(activeJob != null) activeJob.state = IntelScanState.COMPLETE;
		lastState = IntelScanState.COMPLETE;
		activeJob = null;
		markDirty();
	}

	protected void failScan(String error) {
		if(activeJob != null) {
			activeJob.state = IntelScanState.ERROR;
			activeJob.error = error == null ? "ERROR" : error;
		}
		lastState = IntelScanState.ERROR;
		activeJob = null;
		markDirty();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		// SatelliteBase in this branch has legacy inverted base persistence helpers;
		// explicitly preserve intelligence satellite target/tx state here.
		nbt.setInteger("intelTargetX", targetX);
		nbt.setInteger("intelTargetZ", targetZ);
		nbt.setString("intelTx", tx == null ? "" : tx);
		nbt.setString("intelState", lastState.name());
		if(lastResult != null) {
			NBTTagCompound result = new NBTTagCompound();
			lastResult.writeToNBT(result);
			nbt.setTag("intelLastResult", result);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		targetX = nbt.getInteger("intelTargetX");
		targetZ = nbt.getInteger("intelTargetZ");
		tx = nbt.getString("intelTx");
		try {
			lastState = IntelScanState.valueOf(nbt.getString("intelState"));
		} catch(Exception ignored) {
			lastState = IntelScanState.IDLE;
		}
		if(nbt.hasKey("intelLastResult")) lastResult = IntelScanResult.readFromNBT(nbt.getCompoundTag("intelLastResult"));
		activeJob = null;
		if(lastState == IntelScanState.SCANNING) lastState = lastResult == null ? IntelScanState.IDLE : IntelScanState.COMPLETE;
	}
}
