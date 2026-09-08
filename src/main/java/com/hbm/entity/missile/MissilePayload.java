package com.hbm.entity.missile;

import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.tileentity.machine.TileEntityMachineRadarRadome;
import net.minecraft.entity.Entity;

/** Payload identification available only to the advanced radome. */
public final class MissilePayload {
	private MissilePayload() { }

	public static String classifyWarhead(WarheadType type) {
		if(type == WarheadType.NUCLEAR) return "NUCLEAR";
		if(type == WarheadType.TX || type == WarheadType.BUSTER_THERMONUCLEAR) return "THERMONUCLEAR";
		return "UNKNOWN";
	}

	public static String identify(Object radar, Entity entity) {
		if(!(radar instanceof TileEntityMachineRadarRadome)) return "UNKNOWN";
		if(entity instanceof EntityMissileCustom) return ((EntityMissileCustom) entity).getRadarPayload();
		if(entity instanceof EntityMissileTier4.EntityMissileMirv) return "THERMONUCLEAR";
		if(entity instanceof EntityMissileTier4.EntityMissileNuclear
				|| entity instanceof EntityMissileTier0.EntityMissileMicro
				|| entity instanceof EntityMissileTier4.EntityMissileDoomsday) return "NUCLEAR";
		return "UNKNOWN";
	}
}
