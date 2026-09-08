package com.hbm.blocks.machine;

import com.hbm.tileentity.TileEntityProxyCombo;
import com.hbm.tileentity.machine.TileEntityMachineRadarRadome;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.IBlockAccess;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import net.minecraftforge.common.util.ForgeDirection;

public class MachineRadarRadome extends MachineRadarLarge {
	public MachineRadarRadome(Material material) { super(material); }
	@Override public TileEntity createNewTileEntity(World world, int meta) {
		if(meta >= 12) return new TileEntityMachineRadarRadome();
		if(meta >= 6) return new TileEntityProxyCombo().power();
		return null;
	}
	@Override public int[] getDimensions() { return new int[] {12, 0, 3, 3, 3, 3}; }
	@Override public int getOffset() { return 3; }
	@Override public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		int[] core = findCore(world, x, y, z);
		if(core == null) return 0;
		TileEntity tile = world.getTileEntity(core[0], core[1], core[2]);
		return tile instanceof TileEntityMachineRadarNT ? ((TileEntityMachineRadarNT) tile).getRedPower() : 0;
	}
	@Override public void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int offset) {
		super.fillSpace(world, x, y, z, dir, offset);
		x += dir.offsetX * offset;
		z += dir.offsetZ * offset;
		makeExtra(world, x + 3, y, z);
		makeExtra(world, x - 3, y, z);
		makeExtra(world, x, y, z + 3);
		makeExtra(world, x, y, z - 3);
	}
}
