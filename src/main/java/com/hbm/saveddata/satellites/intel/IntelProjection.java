package com.hbm.saveddata.satellites.intel;

import java.util.UUID;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.nbt.NBTTagCompound;

/** Immutable after capture: block palette/metadata plus conservative geometry for custom models. */
public final class IntelProjection {
	public final int originX, originZ, width, depth;
	public final byte[] cells, terrain, glazing, columns;
	public final char[] blockStates;
	public final List<String> blockPalette=new ArrayList<String>();
	private final Map<String,Integer> paletteIndices=new HashMap<String,Integer>();
	private final char[] capturedIds=new char[4096];
	public boolean hasBlockStates=true;
	public String id = UUID.randomUUID().toString();
	public int minY = 256, maxY = -1;
	public int topY=-1;
	public int minX=64, minZ=64, maxX=-1, maxZ=-1;

	public IntelProjection(int originX, int originZ, int width, int depth) {
		if(width < 1 || width > 64 || depth < 1 || depth > 64) throw new IllegalArgumentException("Invalid projection size");
		this.originX=originX; this.originZ=originZ; this.width=width; this.depth=depth;
		blockStates=new char[width*depth*256];
		cells=new byte[width*depth*256]; terrain=new byte[(cells.length+7)/8]; glazing=new byte[terrain.length]; columns=new byte[width*depth];
	}

	private int index(int x, int y, int z) { return (z*width+x)*256+y; }
	public int mask(int x, int y, int z) {
		if(x<0 || x>=width || z<0 || z>=depth || y<0 || y>255) return 0;
		return cells[index(x,y,z)] & 255;
	}
	public int blockState(int x,int y,int z) {
		return x<0 || x>=width || z<0 || z>=depth || y<0 || y>255?0:blockStates[index(x,y,z)];
	}
	public String blockName(int x,int y,int z) {
		int palette=blockState(x,y,z)>>>4;return palette==0?"minecraft:air":blockPalette.get(palette-1);
	}
	public int metadata(int x,int y,int z) { return blockState(x,y,z)&15; }
	private int paletteIndex(String name) {
		if("minecraft:air".equals(name)) return 0;
		Integer found=paletteIndices.get(name);if(found!=null) return found;
		if(blockPalette.size()>=4095 || name.isEmpty() || name.length()>256) throw new IllegalArgumentException("Invalid block palette");
		blockPalette.add(name);int next=blockPalette.size();paletteIndices.put(name,next);return next;
	}
	public void setBlock(int x,int y,int z,String name,int metadata) {
		blockStates[index(x,y,z)]=(char)((paletteIndex(name)<<4)|(metadata&15));
	}
	public void captureBlock(int x,int y,int z,int state) {
		int id=state>>>4;if(id==0 || id>=capturedIds.length) return;
		int palette=capturedIds[id];
		if(palette==0) {
			Object name=Block.blockRegistry.getNameForObject(Block.getBlockById(id));
			if(name==null) return;
			palette=paletteIndex(name.toString());capturedIds[id]=(char)palette;
		}
		blockStates[index(x,y,z)]=(char)((palette<<4)|(state&15));
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
		if(hasBlockStates) {
			byte[] states=new byte[blockStates.length*2];
			for(int i=0;i<blockStates.length;i++) { states[2*i]=(byte)(blockStates[i]>>>8);states[2*i+1]=(byte)blockStates[i]; }
			n.setByteArray("blockStates",states);NBTTagList palette=new NBTTagList();
			for(String name:blockPalette) palette.appendTag(new NBTTagString(name));
			n.setTag("blockPalette",palette);
		}
		return n;
	}
	public static IntelProjection readFromNBT(NBTTagCompound n) {
		int w=n.getInteger("width"), d=n.getInteger("depth");
		if(w<1 || w>64 || d<1 || d>64 || n.getString("id").length()!=36) return null;
		byte[] cells=n.getByteArray("cells"), terrain=n.getByteArray("terrain"), columns=n.getByteArray("columns");
		if(cells.length!=w*d*256 || terrain.length!=w*d*32 || columns.length!=w*d) return null;
		IntelProjection p=new IntelProjection(n.getInteger("x"),n.getInteger("z"),w,d);
		p.hasBlockStates=n.hasKey("blockStates") && n.hasKey("blockPalette");
		if(p.hasBlockStates) {
			byte[] states=n.getByteArray("blockStates");NBTTagList palette=n.getTagList("blockPalette",8);
			if(states.length!=p.blockStates.length*2 || palette.tagCount()>4095) return null;
			for(int i=0;i<palette.tagCount();i++) {
				String name=palette.getStringTagAt(i);
				if(name.isEmpty() || name.length()>256 || p.paletteIndices.containsKey(name) || "minecraft:air".equals(name)) return null;
				p.paletteIndex(name);
			}
			for(int i=0;i<p.blockStates.length;i++) {
				int state=((states[i*2]&255)<<8)|(states[i*2+1]&255);
				if((state>>>4)>palette.tagCount()) return null;
				p.blockStates[i]=(char)state;
			}
		}
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
