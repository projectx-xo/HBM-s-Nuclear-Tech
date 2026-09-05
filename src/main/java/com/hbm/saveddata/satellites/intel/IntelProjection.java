package com.hbm.saveddata.satellites.intel;

import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;

/** Scan-time geometry. Eight occupancy bits per block preserve half-block detail. */
public final class IntelProjection {
	public final int originX, originZ, width, depth;
	public final byte[] cells, terrain, glazing, columns;
	public String id = UUID.randomUUID().toString();
	public int minY = 256, maxY = -1;
	public int topY=-1;
	public int minX=64, minZ=64, maxX=-1, maxZ=-1;

	public IntelProjection(int originX, int originZ, int width, int depth) {
		if(width < 1 || width > 64 || depth < 1 || depth > 64) throw new IllegalArgumentException("Invalid projection size");
		this.originX=originX; this.originZ=originZ; this.width=width; this.depth=depth;
		cells=new byte[width*depth*256]; terrain=new byte[(cells.length+7)/8]; glazing=new byte[terrain.length]; columns=new byte[width*depth];
	}

	private int index(int x, int y, int z) { return (z*width+x)*256+y; }
	public int mask(int x, int y, int z) {
		if(x<0 || x>=width || z<0 || z>=depth || y<0 || y>255) return 0;
		return cells[index(x,y,z)] & 255;
	}
	public boolean natural(int x, int y, int z) {
		int i=index(x,y,z); return (terrain[i>>3] & (1<<(i&7))) != 0;
	}
	public void set(int x, int y, int z, int mask, boolean natural) {
		set(x,y,z,mask,natural,false);
	}
	public boolean glass(int x,int y,int z) {
		int i=index(x,y,z);return (glazing[i>>3] & (1<<(i&7)))!=0;
	}
	public void set(int x, int y, int z, int mask, boolean natural, boolean glass) {
		int i=index(x,y,z); cells[i]=(byte)mask;
		if(mask!=0) topY=Math.max(topY,y);
		if(natural) terrain[i>>3] |= 1<<(i&7);
		else terrain[i>>3] &= ~(1<<(i&7));
		if(glass) glazing[i>>3] |= 1<<(i&7);else glazing[i>>3] &= ~(1<<(i&7));
		if(mask!=0 && !natural) {
			minY=Math.min(minY,y); maxY=Math.max(maxY,y); minX=Math.min(minX,x); maxX=Math.max(maxX,x);
			minZ=Math.min(minZ,z); maxZ=Math.max(maxZ,z);
		}
	}
	public int coveredColumns() { int n=0; for(byte b:columns) if(b!=0) n++; return n; }
	public double[] bounds(boolean includeTerrain) {
		boolean full=includeTerrain || maxY<0;
		return new double[]{full?0:minX,full?0:minY,full?0:minZ,full?width:maxX+1,
				includeTerrain?Math.max(1,topY+1):Math.max(1,maxY+1),full?depth:maxZ+1};
	}
	public NBTTagCompound writeToNBT() {
		NBTTagCompound n=new NBTTagCompound();
		n.setString("id",id); n.setInteger("x",originX); n.setInteger("z",originZ);
		n.setInteger("width",width); n.setInteger("depth",depth);
		n.setInteger("minY",minY); n.setInteger("maxY",maxY);
		n.setByteArray("cells",cells); n.setByteArray("terrain",terrain); n.setByteArray("glazing",glazing); n.setByteArray("columns",columns);
		return n;
	}
	public static IntelProjection readFromNBT(NBTTagCompound n) {
		int w=n.getInteger("width"), d=n.getInteger("depth");
		if(w<1 || w>64 || d<1 || d>64 || n.getString("id").length()!=36) return null;
		byte[] cells=n.getByteArray("cells"), terrain=n.getByteArray("terrain"), columns=n.getByteArray("columns");
		if(cells.length!=w*d*256 || terrain.length!=w*d*32 || columns.length!=w*d) return null;
		IntelProjection p=new IntelProjection(n.getInteger("x"),n.getInteger("z"),w,d);
		p.id=n.getString("id"); p.minY=Math.max(0,Math.min(256,n.getInteger("minY")));
		p.maxY=Math.max(-1,Math.min(255,n.getInteger("maxY")));
		System.arraycopy(cells,0,p.cells,0,cells.length); System.arraycopy(terrain,0,p.terrain,0,terrain.length);
		System.arraycopy(columns,0,p.columns,0,columns.length);
		byte[] glazing=n.getByteArray("glazing");
		if(glazing.length==p.glazing.length) System.arraycopy(glazing,0,p.glazing,0,glazing.length);
		for(int x=0;x<w;x++) for(int z=0;z<d;z++) for(int y=0;y<256;y++) {
			if(p.mask(x,y,z)!=0) p.topY=Math.max(p.topY,y);
			if(p.mask(x,y,z)!=0 && !p.natural(x,y,z)) {
				p.minX=Math.min(p.minX,x);p.maxX=Math.max(p.maxX,x);p.minZ=Math.min(p.minZ,z);p.maxZ=Math.max(p.maxZ,z);
			}
		}
		return p;
	}
	public static boolean matches(IntelScanResult result,int dimension,String id) {
		return result!=null && result.mode==IntelScanMode.COMBINED && result.dimension==dimension
				&& result.projection!=null && result.projection.id.equals(id);
	}
}
