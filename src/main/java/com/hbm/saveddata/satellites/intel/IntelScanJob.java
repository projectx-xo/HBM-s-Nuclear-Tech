package com.hbm.saveddata.satellites.intel;

public class IntelScanJob {
	public IntelScanMode mode;
	public IntelScanState state = IntelScanState.SCANNING;
	public int phase;
	public int cursor;
	public int phaseCursor;
	public int processedWork;
	public int totalWork;
	public int missingColumns;
	public String error = "";

	public IntelScanJob(IntelScanMode mode) {
		this.mode = mode;
		this.totalWork = mode == IntelScanMode.COMBINED ? 64 * 64 * 3 : 64 * 64;
	}

	public int getProgressPercent() {
		if(totalWork <= 0) return 0;
		return Math.max(0, Math.min(100, (int) Math.round(processedWork * 100D / totalWork)));
	}
}
