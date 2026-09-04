package com.hbm.saveddata.satellites.intel;

public enum IntelResistanceBand {
	LIGHT,
	HARDENED,
	HEAVY,
	EXTREME,
	STRATEGIC;

	public static IntelResistanceBand fromResistance(float resistance) {
		if(resistance < 10F) return LIGHT;
		if(resistance < 40F) return HARDENED;
		if(resistance < 100F) return HEAVY;
		if(resistance < 500F) return EXTREME;
		return STRATEGIC;
	}
}
