package com.hbm.blocks.machine;

import com.hbm.main.MainRegistry;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockIntelProjector extends BlockContainer {
	public BlockIntelProjector() { super(Material.iron); }
	@Override public TileEntity createNewTileEntity(World world,int meta) { return new TileEntityIntelProjector(); }
	@Override public boolean onBlockActivated(World world,int x,int y,int z,EntityPlayer player,int side,float hitX,float hitY,float hitZ) {
		if(player.isSneaking()) return false;
		if(!world.isRemote) FMLNetworkHandler.openGui(player,MainRegistry.instance,0,world,x,y,z);
		return true;
	}
}
