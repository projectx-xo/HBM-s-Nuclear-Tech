package com.hbm.tileentity.bomb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.hbm.items.ModItems;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class LaunchPadPayloadIdentityTest {

	@Test
	public void ordinaryPadDistinguishesAbmFromOtherPayloads() {
		ModItems.missile_anti_ballistic = new Item();
		TileEntityLaunchPad pad = new TileEntityLaunchPad();
		assertEquals("empty", pad.getPayloadIdentity(null, null)[0]);

		pad.setInventorySlotContents(0, new ItemStack(ModItems.missile_anti_ballistic));
		assertEquals("anti_ballistic", pad.getPayloadIdentity(null, null)[0]);

		pad.setInventorySlotContents(0, new ItemStack(new Item()));
		assertEquals("other", pad.getPayloadIdentity(null, null)[0]);
	}

	@Test
	public void customPadIsNeverAnAbmSite() {
		TileEntityLaunchTable pad = new TileEntityLaunchTable();
		assertEquals("empty", pad.getPayloadIdentity(null, null)[0]);
		pad.setInventorySlotContents(0, new ItemStack(new Item()));
		assertEquals("other", pad.getPayloadIdentity(null, null)[0]);
	}
}
