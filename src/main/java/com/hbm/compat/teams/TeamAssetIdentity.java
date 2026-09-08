package com.hbm.compat.teams;

import java.util.Map;
import cpw.mods.fml.common.Loader;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Read-only BaseCenter bridge. Asset labels confer no protection or firing restrictions. */
public final class TeamAssetIdentity {
	public static final String KEY = "hbmTeamAsset";
	private TeamAssetIdentity() { }
	public static String teamFor(String username) {
		if(!Loader.isModLoaded("hbmbasecenter")) return "";
		try {
			Class<?> type = Class.forName("com.undaub.hbmbasecenter.logic.Team");
			Object team = ((Map<?, ?>) type.getField("usernameTeams").get(null)).get(username);
			return team == null ? "" : validText(String.valueOf(type.getField("name").get(team)), 64);
		} catch(ReflectiveOperationException | LinkageError | ClassCastException unavailable) {
			return "";
		}
	}
	public static String validText(String value, int limit) {
		if(value == null || value.isEmpty() || value.length() > limit) return "";
		for(int i=0;i<value.length();i++) if(Character.isISOControl(value.charAt(i)) || value.charAt(i)=='\u00a7') return "";
		return value;
	}
	public static Object[] read(TileEntity tile) {
		NBTTagCompound data = ((TeamAssetHost)tile).getTeamAssetData().getCompoundTag(KEY);
		String owner = data.getString("ownerName");
		String team = teamFor(owner);
		return new Object[] {team, validText(data.getString("label"),32), tile.xCoord, tile.yCoord, tile.zCoord,
			tile.getWorldObj().provider.dimensionId, team.isEmpty() ? "UNKNOWN" : "BASECENTER", owner};
	}
}
