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
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister reg) {
		super.registerIcons(reg);
		// Temporary artwork reuse until dedicated intelligence satellite textures are added.
		this.icons[EnumSatType.SURFACE_RECON.ordinal()] = reg.registerIcon(this.getIconString() + ".spy");
		this.icons[EnumSatType.SUBSURFACE_INTEL.ordinal()] = reg.registerIcon(this.getIconString() + ".scanner");
		this.icons[EnumSatType.COMBINED_INTEL.ordinal()] = reg.registerIcon(this.getIconString() + ".spy");
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
