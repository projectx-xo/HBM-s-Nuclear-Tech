package com.hbm.saveddata.satellites;

import com.hbm.saveddata.satellites.intel.IntelScanMode;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class SatelliteSubsurfaceIntel extends SatelliteIntelligenceBase {

	@Override
	public String getType() {
		return "SUBSURFACE_INTEL";
	}

	@Override
	public IntelScanMode getScanMode() {
		return IntelScanMode.SUBSURFACE;
	}

	@Override
	public IChatComponent[] getInfo(World world) {
		return new IChatComponent[] { new ChatComponentText("Subsurface Intelligence Satellite") };
	}
}
