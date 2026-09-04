package com.hbm.saveddata.satellites;

import com.hbm.saveddata.satellites.intel.IntelBlockClassifier;
import com.hbm.saveddata.satellites.intel.IntelScanJob;
import com.hbm.saveddata.satellites.intel.IntelScanMode;
import com.hbm.saveddata.satellites.intel.IntelScanResult;
import com.hbm.saveddata.satellites.intel.IntelScanState;
import com.hbm.saveddata.satellites.intel.IntelStructuralAnalyzer;
import com.hbm.saveddata.satellites.intel.IntelSubsurfaceScanner;
import com.hbm.saveddata.satellites.intel.IntelSurfaceScanner;

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

	protected final IntelBlockClassifier intelClassifier = new IntelBlockClassifier();
	protected final IntelSurfaceScanner surfaceScanner = new IntelSurfaceScanner(intelClassifier);
	protected final IntelSubsurfaceScanner subsurfaceScanner = new IntelSubsurfaceScanner(intelClassifier);
	protected final IntelStructuralAnalyzer structuralAnalyzer = new IntelStructuralAnalyzer(intelClassifier);

	public IntelScanJob activeJob;
	public IntelScanResult activeResult;
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
		activeResult = result;
		markDirty();
		return true;
	}

	public String getScanStatus() {
		if(activeJob != null) {
			int coverage = activeResult == null ? 0 : activeResult.getCoveragePercent();
			return activeJob.state.name() + ";" + activeJob.processedWork + ";" + activeJob.totalWork + ";" + coverage;
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

	public IntelScanResult getActiveResult() {
		return activeResult;
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
				tx = "STRUCTURE;MATERIAL=" + lastResult.structuralSummary.dominantMaterial
						+ ";AVG=" + lastResult.structuralSummary.averageResistance
						+ ";MAX=" + lastResult.structuralSummary.maxResistance
						+ ";WALL=" + lastResult.structuralSummary.wallThickness
						+ ";ROOF=" + lastResult.structuralSummary.roofThickness
						+ ";FLOOR=" + lastResult.structuralSummary.floorThickness
						+ ";WEAK=" + lastResult.structuralSummary.weakPointCount;
			}
		}
	}

	@Override
	public void onUpdateTick(World world) {
		if(activeJob != null) processScanTick(world);
	}

	protected void processScanTick(World world) {
		if(world == null || world.isRemote || activeJob == null || activeResult == null) return;
		try {
			if(getScanMode() == IntelScanMode.SURFACE) {
				surfaceScanner.process(world, activeJob, activeResult, WORK_BUDGET_PER_TICK);
				if(activeJob.phaseCursor >= SCAN_SIZE * SCAN_SIZE) finishOrFail(world);
				return;
			}

			if(getScanMode() == IntelScanMode.SUBSURFACE) {
				subsurfaceScanner.process(world, activeJob, activeResult, WORK_BUDGET_PER_TICK);
				if(activeJob.phaseCursor >= SCAN_SIZE * SCAN_SIZE) {
					subsurfaceScanner.finalizeFindings(activeResult);
					finishOrFail(world);
				}
				return;
			}

			if(activeJob.phase == 0) {
				surfaceScanner.process(world, activeJob, activeResult, WORK_BUDGET_PER_TICK);
				if(activeJob.phaseCursor >= SCAN_SIZE * SCAN_SIZE) {
					activeJob.phase = 1;
					activeJob.phaseCursor = 0;
				}
				return;
			}

			if(activeJob.phase == 1) {
				subsurfaceScanner.process(world, activeJob, activeResult, WORK_BUDGET_PER_TICK);
				if(activeJob.phaseCursor >= SCAN_SIZE * SCAN_SIZE) {
					subsurfaceScanner.finalizeFindings(activeResult);
					activeJob.phase = 2;
					activeJob.phaseCursor = 0;
				}
				return;
			}

			if(activeJob.phase == 2) {
				structuralAnalyzer.process(world, activeJob, activeResult, WORK_BUDGET_PER_TICK);
				if(activeJob.phaseCursor >= structuralAnalyzer.getCandidateCount(activeResult)) {
					activeResult.structuralSummary = structuralAnalyzer.finalizeSummary(activeResult);
					activeJob.processedWork = activeJob.totalWork;
					finishOrFail(world);
				}
			}
		} catch(Throwable t) {
			failScan(t.getClass().getSimpleName());
		}
	}

	protected void onIntelligenceReady(IntelScanResult result) { }

	private void finishOrFail(World world) {
		if(activeResult == null || activeResult.coveredColumns <= 0) {
			tx = "UNLOADED";
			failScan("UNLOADED");
			return;
		}
		onIntelligenceReady(activeResult);
		completeScan(world);
	}

	protected void completeScan(World world) {
		if(activeResult != null) {
			activeResult.completedAt = world.getTotalWorldTime();
			lastResult = activeResult;
			activeResult = null;
		}
		if(activeJob != null) activeJob.state = IntelScanState.COMPLETE;
		lastState = IntelScanState.COMPLETE;
		tx = "COMPLETE";
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
		activeResult = null;
		markDirty();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// SatelliteBase's persistence methods are inverted on this branch. Do not call
		// super here: its writeToNBT reads from the outgoing tag and mutates this object.
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
		// Likewise, do not call the inverted base readFromNBT, which writes defaults
		// back into the tag being loaded. Intelligence satellites own their persisted
		// target/status/result data under the intel* keys below.
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
		activeResult = null;
		if(lastState == IntelScanState.SCANNING) lastState = lastResult == null ? IntelScanState.IDLE : IntelScanState.COMPLETE;
	}
}
