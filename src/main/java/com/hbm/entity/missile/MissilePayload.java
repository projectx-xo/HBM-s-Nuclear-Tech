package com.hbm.entity.missile;

import com.hbm.items.weapon.ItemCustomMissilePart.WarheadType;
import com.hbm.tileentity.machine.TileEntityMachineRadarRadome;
import net.minecraft.entity.Entity;

/** Nuclear payload classification shared by the advanced radome and detection satellite. */
public final class MissilePayload {
	private MissilePayload() { }

	public static String classifyWarhead(WarheadType type) {
		if(type == WarheadType.NUCLEAR) return "NUCLEAR";
		if(type == WarheadType.TX || type == WarheadType.BUSTER_THERMONUCLEAR) return "THERMONUCLEAR";
		return "UNKNOWN";
	}

	public static String identify(Object radar, Entity entity) {
		if(!(radar instanceof TileEntityMachineRadarRadome)) return "UNKNOWN";
		return classify(entity);
	}

	public static String classify(Entity entity) {
		if(entity instanceof EntityMissileCustom) return ((EntityMissileCustom) entity).getRadarPayload();
		if(entity instanceof EntityMissileTier4.EntityMissileMirv) return "THERMONUCLEAR";
		if(entity instanceof EntityMissileTier4.EntityMissileNuclear
				|| entity instanceof EntityMissileTier0.EntityMissileMicro
				|| entity instanceof EntityMissileTier4.EntityMissileDoomsday) return "NUCLEAR";
		return "UNKNOWN";
	}
}
