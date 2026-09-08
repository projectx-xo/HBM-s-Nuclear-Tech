package com.hbm.saveddata.satellites;

import com.hbm.items.ModItems;
import com.hbm.items.special.ItemSatellite.EnumSatType;

import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

/** Dedicated orbital relay for OpenComputers SATCOM packet transport. */
public class SatelliteSatComRelay extends SatelliteRelay {

	@Override public String getType() { return "SATCOM_RELAY"; }

	@Override
	public IChatComponent[] getInfo(World world) {
		return new IChatComponent[] {
				new ChatComponentTranslation(ModItems.satellite.getUnlocalizedName(new ItemStack(ModItems.satellite, 1, EnumSatType.SATCOM_RELAY.ordinal())) + ".name")
		};
	}
}
