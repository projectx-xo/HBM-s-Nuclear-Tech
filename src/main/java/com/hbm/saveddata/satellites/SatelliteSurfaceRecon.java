package com.hbm.saveddata.satellites;

import com.hbm.saveddata.satellites.intel.IntelScanMode;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class SatelliteSurfaceRecon extends SatelliteIntelligenceBase {

	@Override
	public String getType() {
		return "SURFACE_RECON";
	}

	@Override
	public IntelScanMode getScanMode() {
		return IntelScanMode.SURFACE;
	}

	@Override
	public IChatComponent[] getInfo(World world) {
		return new IChatComponent[] { new ChatComponentText("Surface Reconnaissance Satellite") };
	}
}
