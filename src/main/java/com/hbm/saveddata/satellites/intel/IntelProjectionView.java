package com.hbm.saveddata.satellites.intel;

import net.minecraft.nbt.NBTTagCompound;

public final class IntelProjectionView {
	public String mode="exterior";
	public int floor=255, cutAxis=-1, cut, selected;
	public float rotation, size=6;
	public boolean terrain;

	public void configure(String action,String value,IntelProjection p,int findings) {
		if("view".equals(action)) {
			if(!"exterior".equals(value) && !"interior".equals(value) && !"cutaway".equals(value))
				throw new IllegalArgumentException("Use exterior, interior or cutaway");
			mode=value; floor="interior".equals(value)?Math.max(0,p.maxY-1):255;
			cutAxis="cutaway".equals(value)?2:-1; cut=p.originZ+(p.minZ+p.maxZ)/2;
		} else if("floor".equals(action)) {
			floor="all".equals(value)?255:integer(value,0,255); mode="interior";
		} else if("cut".equals(action)) {
			if("none".equals(value)) { cutAxis=-1; return; }
			if(!value.matches("[xz]:-?[0-9]+")) throw new IllegalArgumentException("Use x:coordinate, z:coordinate or none");
			int coordinate=integer(value.substring(2),-30000000,30000000);
			cutAxis=value.charAt(0)=='x'?0:2; cut=coordinate; mode="cutaway";
		} else if("select".equals(action)) selected="all".equals(value)?0:integer(value,1,findings);
		else if("rotate".equals(action)) rotation=integer(value,-360,360);
		else if("scale".equals(action)) size=integer(value,2,12);
		else if("terrain".equals(action)) {
			if(!"on".equals(value) && !"off".equals(value)) throw new IllegalArgumentException("Use on or off");
			terrain="on".equals(value);
		} else throw new IllegalArgumentException("Unknown projector control");
	}
	private static int integer(String value,int min,int max) {
		int n; try { n=Integer.parseInt(value); } catch(Exception e) { throw new IllegalArgumentException("Expected an integer"); }
		if(n<min || n>max) throw new IllegalArgumentException("Expected "+min+".."+max);
		return n;
	}
	public void write(NBTTagCompound n) {
		n.setString("view",mode);n.setInteger("floor",floor);n.setInteger("cutAxis",cutAxis);n.setInteger("cut",cut);
		n.setInteger("selected",selected);n.setFloat("rotation",rotation);n.setFloat("size",size);n.setBoolean("terrain",terrain);
	}
	public void read(NBTTagCompound n) {
		mode=n.getString("view");floor=Math.max(0,Math.min(255,n.getInteger("floor")));
		cutAxis=n.getInteger("cutAxis");if(cutAxis!=0 && cutAxis!=2) cutAxis=-1;
		cut=n.getInteger("cut");selected=Math.max(0,Math.min(128,n.getInteger("selected")));
		rotation=n.getFloat("rotation");if(!Float.isFinite(rotation)) rotation=0;
		size=n.getFloat("size");if(!Float.isFinite(size)) size=6;size=Math.max(2,Math.min(12,size));terrain=n.getBoolean("terrain");
	}
}
