package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.items.special.ItemSatellite.EnumSatType;
import com.hbm.saveddata.satellites.intel.IntelScanMode;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
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
		return new IChatComponent[] {
				new ChatComponentTranslation(ModItems.satellite.getUnlocalizedName(new ItemStack(ModItems.satellite, 1, EnumSatType.SUBSURFACE_INTEL.ordinal())) + ".name")
		};
	}
}
