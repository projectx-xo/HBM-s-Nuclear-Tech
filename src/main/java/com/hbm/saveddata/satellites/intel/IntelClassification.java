package com.hbm.saveddata.satellites.intel;

public enum IntelClassification {
	NATURAL,
	STRUCTURE,
	REINFORCED_STRUCTURE,
	MACHINERY,
	POWER,
	COMMUNICATIONS,
	LAUNCH_INFRASTRUCTURE,
	CAVITY,
	TUNNEL,
	BUNKER,
	POSSIBLE_SILO,
	MISSILE,
	SILO_HATCH,
	RADAR;

	public IntelClassification forMode(IntelScanMode mode) {
		if(mode != IntelScanMode.COMBINED) {
			if(this == LAUNCH_INFRASTRUCTURE || this == MISSILE || this == SILO_HATCH) return MACHINERY;
			if(this == POSSIBLE_SILO) return BUNKER;
			if(this == RADAR) return COMMUNICATIONS;
		}
		return this;
	}
}
