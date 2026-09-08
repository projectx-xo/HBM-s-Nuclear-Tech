package com.hbm.compat.teams;

import static org.junit.Assert.*;
import org.junit.Test;
import net.minecraft.nbt.NBTTagCompound;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;

public class TeamAssetIdentityTest {
	@Test public void labelsRejectControlFormattingAndOversize() {
		assertEquals("Radar-01",TeamAssetIdentity.validText("Radar-01",32));
		assertEquals("",TeamAssetIdentity.validText("bad\nlabel",32));
		assertEquals("",TeamAssetIdentity.validText("\u00a7cFake",32));
		assertEquals("",TeamAssetIdentity.validText("12345",4));
	}
	@Test public void satelliteRegistrationSurvivesNbtAndOldSavesRemainEmpty() {
		cpw.mods.fml.common.registry.GameRegistry.registerTileEntity(TileEntityMachineSatLink.class,"team_asset_test");
		TileEntityMachineSatLink original=new TileEntityMachineSatLink();
		NBTTagCompound identity=new NBTTagCompound();identity.setString("ownerName","Tester");identity.setString("label","INTEL-1");
		original.getTeamAssetData().setTag(TeamAssetIdentity.KEY,identity);
		NBTTagCompound saved=new NBTTagCompound();original.writeToNBT(saved);
		TileEntityMachineSatLink restored=new TileEntityMachineSatLink();restored.readFromNBT(saved);
		assertEquals("INTEL-1",restored.getTeamAssetData().getCompoundTag(TeamAssetIdentity.KEY).getString("label"));
		saved.removeTag("hbmAssetRegistration");restored.readFromNBT(saved);
		assertFalse(restored.getTeamAssetData().hasKey(TeamAssetIdentity.KEY));
	}
}
