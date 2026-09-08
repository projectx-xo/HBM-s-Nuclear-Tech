package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.hbm.blocks.generic.BlockBedrockOreTE;
import com.hbm.blocks.generic.BlockCluster;
import com.hbm.blocks.generic.BlockDepthOre;
import com.hbm.blocks.generic.BlockKeyhole;
import com.hbm.blocks.generic.BlockResourceStone;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

public final class IntelProjectionScanner {
	public interface Access {
		boolean loaded(int x, int z);
		/** Low eight bits: geometry; bit eight: natural terrain; bit nine: glazing; remaining bits: block ID and metadata. */
		int cell(int x, int y, int z);
		default void captureModel(IntelProjection p,int x,int y,int z) {}
	}
	public void process(Access world, IntelScanJob job, IntelScanResult result, int columns) {
		if(result.mode!=IntelScanMode.COMBINED) return;
		if(result.projection==null) result.projection=new IntelProjection(result.targetX-result.width/2,
				result.targetZ-result.depth/2,result.width,result.depth);
		IntelProjection p=result.projection;
		for(int n=0;n<columns && job.phaseCursor<p.width*p.depth;n++) {
			int i=job.phaseCursor++, x=i%p.width, z=i/p.width;
			if(world.loaded(p.originX+x,p.originZ+z)) {
				p.columns[i]=1;
				for(int y=0;y<256;y++) {
					int cell=world.cell(p.originX+x,y,p.originZ+z);
					p.set(x,y,z,cell&255,(cell&256)!=0,(cell&512)!=0);
					p.captureBlock(x,y,z,cell>>>10);
					world.captureModel(p,x,y,z);
				}
			}
			job.processedWork++;
		}
	}
	public static int mask(double x1,double y1,double z1,double x2,double y2,double z2) {
		int bits=0;
		for(int y=0;y<2;y++) for(int z=0;z<2;z++) for(int x=0;x<2;x++) {
			if(x1<(x+1)*.5 && x2>x*.5 && y1<(y+1)*.5 && y2>y*.5 && z1<(z+1)*.5 && z2>z*.5)
				bits |= 1<<(x+2*z+4*y);
		}
		return bits;
	}
	public static Access access(final World world) {
		return new Access() {
			private int lastX=Integer.MIN_VALUE,lastZ;
			private boolean neighbors;
			public boolean loaded(int x,int z) { return world.getChunkProvider().chunkExists(x>>4,z>>4); }
			public void captureModel(IntelProjection p,int x,int y,int z) {
				int cx=p.originX+x,cy=y,cz=p.originZ+z;
				Block block=world.getBlock(cx,cy,cz);
				int kind=IntelProjectionMachine.kind(block);
				if(kind==0) {
					if(block!=com.hbm.blocks.ModBlocks.dummy_plate_launch_table && block!=com.hbm.blocks.ModBlocks.dummy_port_launch_table) return;
					net.minecraft.tileentity.TileEntity te=world.getTileEntity(cx,cy,cz);
					if(!(te instanceof com.hbm.tileentity.machine.TileEntityDummy)) return;
					com.hbm.tileentity.machine.TileEntityDummy d=(com.hbm.tileentity.machine.TileEntityDummy)te;
					cx=d.targetX;cy=d.targetY;cz=d.targetZ;kind=5;
				} else if(kind!=5) {
					// Follow only this machine's dummy chain, bounded and loaded-chunk-only.
					for(int steps=0;steps<64;steps++) {
						if(!inside(p,cx,cy,cz) || !loaded(cx,cz) || world.getBlock(cx,cy,cz)!=block) return;
						int meta=world.getBlockMetadata(cx,cy,cz);if(meta>=12) break;
						net.minecraftforge.common.util.ForgeDirection dir=net.minecraftforge.common.util.ForgeDirection.getOrientation(meta%6).getOpposite();
						cx+=dir.offsetX;cy+=dir.offsetY;cz+=dir.offsetZ;
						if(steps==63) return;
					}
				}
				if(!inside(p,cx,cy,cz) || !loaded(cx,cz) || IntelProjectionMachine.kind(world.getBlock(cx,cy,cz))!=kind) return;
				int key=((cz-p.originZ)*p.width+cx-p.originX)*256+cy;
				if(!p.machines.containsKey(key)) {
					if(p.machines.size()>=256) return; // Uncaptured machines retain conservative block geometry.
					IntelProjectionMachine m=IntelProjectionMachine.capture(cx-p.originX,cy,cz-p.originZ,
						world.getBlockMetadata(cx,cy,cz),kind,world.getTileEntity(cx,cy,cz));
					if(m==null) return;p.machines.put(key,m);
				}
				p.setModelCell(x,y,z);
			}
			private boolean inside(IntelProjection p,int x,int y,int z) {
				return x>=p.originX && x<p.originX+p.width && z>=p.originZ && z<p.originZ+p.depth && y>=0 && y<256;
			}
			public int cell(int x,int y,int z) {
				Block b=world.getBlock(x,y,z);
				if(b==Blocks.air || b.getMaterial()==Material.air) return 0;
				if(x!=lastX || z!=lastZ) {
					lastX=x;lastZ=z;neighbors=true;
					for(int dx=-2;dx<=2;dx+=2) for(int dz=-2;dz<=2;dz+=2) neighbors &= loaded(x+dx,z+dz);
				}
				int meta=world.getBlockMetadata(x,y,z);
				return shape(b,world,x,y,z,neighbors) | (natural(b,meta)?256:0)
						| (b.getMaterial()==Material.glass?512:0) | (((Block.getIdFromBlock(b)<<4)|(meta&15))<<10);
			}
		};
	}
	static int shape(Block b,World world,int x,int y,int z,boolean neighborsLoaded) {
		// Never call arbitrary mod bounds/connection handlers: cable bounds can follow a proxy into an unloaded core chunk.
		if(b instanceof BlockSlab) return b.isOpaqueCube()?255:(world.getBlockMetadata(x,y,z)&8)==0?15:240;
		Class<?> type=b.getClass();
		if(!neighborsLoaded || !(type==BlockStairs.class || type==BlockFence.class
				|| type==BlockFenceGate.class || type==BlockDoor.class || type==BlockTrapDoor.class
				|| type==BlockSnow.class || type==BlockCarpet.class || type==BlockLadder.class
				|| type==BlockWall.class || type==BlockBed.class || type==BlockChest.class)) return 255;
		double a=b.getBlockBoundsMinX(), c=b.getBlockBoundsMinY(), d=b.getBlockBoundsMinZ();
		double e=b.getBlockBoundsMaxX(), f=b.getBlockBoundsMaxY(), g=b.getBlockBoundsMaxZ();
		try {
			if(type==BlockStairs.class) {
				List<AxisAlignedBB> boxes=new ArrayList<AxisAlignedBB>();
				b.addCollisionBoxesToList(world,x,y,z,AxisAlignedBB.getBoundingBox(x,y,z,x+1,y+1,z+1),boxes,null);
				int bits=0;
				for(AxisAlignedBB box:boxes) bits |= mask(box.minX-x,box.minY-y,box.minZ-z,box.maxX-x,box.maxY-y,box.maxZ-z);
				return bits;
			}
			b.setBlockBoundsBasedOnState(world,x,y,z);
			return mask(b.getBlockBoundsMinX(),b.getBlockBoundsMinY(),b.getBlockBoundsMinZ(),
					b.getBlockBoundsMaxX(),b.getBlockBoundsMaxY(),b.getBlockBoundsMaxZ());
		} finally { b.setBlockBounds((float)a,(float)c,(float)d,(float)e,(float)f,(float)g); }
	}
	static boolean natural(Block b,int meta) {
		if(b instanceof BlockBedrockOreTE) return true;
		if(b.hasTileEntity(meta)) return false;
		if(b instanceof BlockCluster || b instanceof BlockDepthOre || b instanceof BlockResourceStone || b instanceof BlockKeyhole) return true;
		Material m=b.getMaterial();
		if(m.isLiquid() || m==Material.leaves || m==Material.plants || m==Material.vine || m==Material.snow) return true;
		if(b==Blocks.stone || b==Blocks.dirt || b==Blocks.grass || b==Blocks.sand || b==Blocks.gravel
				|| b==Blocks.bedrock || b==Blocks.netherrack || b==Blocks.end_stone || b==Blocks.soul_sand) return true;
		String name=String.valueOf(Block.blockRegistry.getNameForObject(b)).toLowerCase(Locale.US);
		return name.contains("ore_") || name.contains("_ore") || name.contains("stone_depth") || name.contains("stone_porous");
	}
}
