package com.hbm.saveddata.satellites.intel;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.util.ForgeDirection;

/** Local X/Z, original Y. Rendering can only consult the captured snapshot, never remote chunks. */
public final class IntelProjectionBlockAccess implements IBlockAccess {
	public final IntelProjection projection;
	private final Block[] palette;
	private final int floor,cutAxis,cut;
	private final boolean terrain;

	public IntelProjectionBlockAccess(IntelProjection projection,IntelProjectionView view) {
		this.projection=projection;floor=view.floor;cutAxis=view.cutAxis;cut=view.cut;terrain=view.terrain;
		palette=new Block[projection.blockPalette.size()+1];palette[0]=Blocks.air;
		for(int i=1;i<palette.length;i++) {
			Block b=Block.getBlockFromName(projection.blockPalette.get(i-1));palette[i]=b==null?Blocks.air:b;
		}
	}
	public boolean visible(int x,int y,int z) {
		return y<=floor && projection.mask(x,y,z)!=0 && (terrain || !projection.natural(x,y,z))
				&& (cutAxis!=0 || projection.originX+x<=cut) && (cutAxis!=2 || projection.originZ+z<=cut);
	}
	@Override public Block getBlock(int x,int y,int z) {
		return visible(x,y,z)?palette[projection.blockState(x,y,z)>>>4]:Blocks.air;
	}
	@Override public int getBlockMetadata(int x,int y,int z) { return visible(x,y,z)?projection.metadata(x,y,z):0; }
	public boolean opaque(int x,int y,int z) { Block b=getBlock(x,y,z);return b!=null && b.isOpaqueCube(); }
	public boolean enclosed(int x,int y,int z) {
		return opaque(x-1,y,z) && opaque(x+1,y,z) && opaque(x,y-1,z) && opaque(x,y+1,z) && opaque(x,y,z-1) && opaque(x,y,z+1);
	}
	@Override public TileEntity getTileEntity(int x,int y,int z) { return null; }
	@Override public int getLightBrightnessForSkyBlocks(int x,int y,int z,int minimum) { return 0xF000F0; }
	@Override public int isBlockProvidingPowerTo(int x,int y,int z,int side) { return 0; }
	@Override public boolean isAirBlock(int x,int y,int z) { return getBlock(x,y,z)==Blocks.air; }
	@Override public BiomeGenBase getBiomeGenForCoords(int x,int z) { return BiomeGenBase.plains; }
	@Override public int getHeight() { return 256; }
	@Override public boolean extendedLevelsInChunkCache() { return false; }
	@Override public boolean isSideSolid(int x,int y,int z,ForgeDirection side,boolean fallback) { return opaque(x,y,z); }
}
