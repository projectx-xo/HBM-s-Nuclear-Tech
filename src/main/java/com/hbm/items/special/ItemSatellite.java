package com.hbm.items.special;

import java.util.List;

import com.hbm.items.ISatChip;
import com.hbm.items.ItemEnumMulti;
import com.hbm.util.i18n.I18nUtil;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

public class ItemSatellite extends ItemEnumMulti implements ISatChip {

	public ItemSatellite() {
		super(EnumSatType.class, true, true);
	}

	public static enum EnumSatType {
		SPY,
		SCANNER,
		RADAR,
		MINER_ASTRO,
		MINER_LUNAR,
		PRECISION_LASER,
		DEATH_RAY,
		XENIUM_RESONATOR,
		RELAY,
		DETECTOR,
		RAY_SCAN,
		SCIENCE,
		SCIENCE_ASSEMBLER,
		SCIENCE_SENSOR,
		SURFACE_RECON,
		SUBSURFACE_INTEL,
		COMBINED_INTEL,
		SATCOM_RELAY,
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		this.icons = new net.minecraft.util.IIcon[EnumSatType.values().length];
		for(EnumSatType type : EnumSatType.values()) {
			String texture = type.name().toLowerCase(java.util.Locale.US);
			if(type == EnumSatType.SURFACE_RECON || type == EnumSatType.COMBINED_INTEL) texture = "spy";
			if(type == EnumSatType.SUBSURFACE_INTEL) texture = "scanner";
			if(type == EnumSatType.SATCOM_RELAY) texture = "detector";
			this.icons[type.ordinal()] = reg.registerIcon(this.getIconString() + "." + texture);
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		EnumSatType type = EnumSatType.values()[Math.max(0, Math.min(stack.getItemDamage(), EnumSatType.values().length - 1))];
		if(type == EnumSatType.SURFACE_RECON) return "Surface Reconnaissance Satellite";
		if(type == EnumSatType.SUBSURFACE_INTEL) return "Subsurface Intelligence Satellite";
		if(type == EnumSatType.COMBINED_INTEL) return "Combined Intelligence Satellite";
		return super.getItemStackDisplayName(stack);
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean bool) {
		list.add(EnumChatFormatting.AQUA + I18nUtil.resolveKey("satchip.frequency") + ": " + getFreq(stack));
	}
}
