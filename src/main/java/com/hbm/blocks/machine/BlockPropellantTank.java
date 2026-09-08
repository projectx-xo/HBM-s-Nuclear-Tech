package com.hbm.blocks.machine;

import java.util.List;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.tileentity.machine.storage.TileEntityPropellantTank;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockPropellantTank extends BlockFluidBarrel {
	public static int tankRenderID;
	public final int tier;
	public final int creativeFuel;
	public BlockPropellantTank(int tier, int creativeFuel) {
		super(Material.iron, TileEntityPropellantTank.CAPACITIES[tier]);
		this.tier = tier;
		this.creativeFuel = creativeFuel;
	}
	@Override public TileEntity createNewTileEntity(World world, int meta) { return new TileEntityPropellantTank(tier, creativeFuel); }
	@Override public int getRenderType() { return tankRenderID; }
	@Override public void setBlockBoundsBasedOnState(IBlockAccess world, int x, int y, int z) { setBlockBounds(0, 0, 0, 1, 1, 1); }
	@Override public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
	}
	@Override public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack stack) {
		world.setBlockMetadataWithNotify(x, y, z, MathHelper.floor_double(player.rotationYaw * 4 / 360 + 0.5) & 3, 2);
		super.onBlockPlacedBy(world, x, y, z, player, stack);
	}
	@Override public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hx, float hy, float hz) {
		if(player.isSneaking() && player.getHeldItem() != null && player.getHeldItem().getItem() instanceof IItemFluidIdentifier) {
			if(world.isRemote) return true;
			TileEntity te = world.getTileEntity(x, y, z);
			if(!(te instanceof TileEntityPropellantTank)) return false;
			TileEntityPropellantTank tank = (TileEntityPropellantTank) te;
			FluidType next = ((IItemFluidIdentifier) player.getHeldItem().getItem()).getType(world, x, y, z, player.getHeldItem());
			if(tank.isCreative() || tank.tank.getFill() > 0 || TileEntityPropellantTank.fuelIndex(next) < 0) {
				player.addChatComponentMessage(new ChatComponentTranslation("propellantTank.typeLocked"));
			} else { tank.tank.setTankType(next); tank.markDirty(); }
			return true;
		}
		return super.onBlockActivated(world, x, y, z, player, side, hx, hy, hz);
	}
	@Override public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean ext) {
		if(creativeFuel >= 0) list.add("Infinite " + TileEntityPropellantTank.fuel(creativeFuel).getLocalizedName());
		else list.add("Capacity: " + TileEntityPropellantTank.CAPACITIES[tier] + " mB");
		list.add("Missile propellant storage; HBM pipes / Forge fluid conduits");
		list.add("Select fluid while empty; use the barrel GUI to choose transfer mode");
	}
}
