package com.hbm.render.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;
import org.lwjgl.opengl.GL11;
import com.hbm.saveddata.satellites.intel.*;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;

/** Compiles bounded batches once per snapshot/cut. Rotation and scale reuse the same block geometry. */
final class IntelProjectionBlockRenderer {
	private static final int MAX_BLOCKS=65536;
	private final IntelProjectionBlockAccess access;
	private final RenderBlocks renderer;
	private static final class TransparentBlock {
		final int list;final double x,y,z;double distance;
		TransparentBlock(int list,int x,int y,int z) { this.list=list;this.x=x+.5;this.y=y+.5;this.z=z+.5; }
	}
	private final List<Integer> opaque=new ArrayList<Integer>();
	private final List<TransparentBlock> transparent=new ArrayList<TransparentBlock>();
	private double cameraX=Double.NaN,cameraY,cameraZ;
	private int cursor,pass,rendered;
	boolean ready,truncated;

	IntelProjectionBlockRenderer(IntelProjection p,IntelProjectionView view) {
		access=new IntelProjectionBlockAccess(p,view);renderer=new RenderBlocks(access);
	}
	void step() {
		if(ready) return;
		long deadline=System.nanoTime()+3000000L;
		int list=0,count=0,visited=0;
		Tessellator tess=Tessellator.instance;
		try {
			while(cursor<access.projection.cells.length && count<256 && visited<16384) {
				if((visited++&63)==0 && System.nanoTime()>=deadline) break;
				int i=cursor++,y=i&255,column=i>>>8,x=column%access.projection.width,z=column/access.projection.width;
				Block block=access.getBlock(x,y,z);
				if(block==Blocks.air || (block.getRenderBlockPass()==1?1:0)!=pass || access.enclosed(x,y,z)) continue;
				if(rendered>=MAX_BLOCKS) { truncated=true;ready=true;break; }
				if(pass==0) {
					if(list==0) {
						list=GLAllocation.generateDisplayLists(1);opaque.add(list);
						GL11.glNewList(list,GL11.GL_COMPILE);tess.startDrawingQuads();tess.setTranslation(0,0,0);
					}
					render(block,x,y,z);
				} else {
					int glass=GLAllocation.generateDisplayLists(1);transparent.add(new TransparentBlock(glass,x,y,z));
					GL11.glNewList(glass,GL11.GL_COMPILE);tess.startDrawingQuads();tess.setTranslation(0,0,0);
					try { render(block,x,y,z); } finally { tess.draw();tess.setTranslation(0,0,0);GL11.glEndList(); }
				}
				count++;rendered++;
			}
		} finally {
			if(list!=0) { tess.draw();tess.setTranslation(0,0,0);GL11.glEndList(); }
		}
		if(cursor==access.projection.cells.length) { cursor=0;pass++;if(pass==2) ready=true; }
	}
	private void render(Block block,int x,int y,int z) {
		double a=block.getBlockBoundsMinX(),b=block.getBlockBoundsMinY(),c=block.getBlockBoundsMinZ();
		double d=block.getBlockBoundsMaxX(),e=block.getBlockBoundsMaxY(),f=block.getBlockBoundsMaxZ();
		try {
			int type=block.getRenderType();
			// Vanilla renderers understand snapshot-only neighbors. Custom ISBRH/TE renderers may require a live World.
			if(block.getClass().getName().startsWith("net.minecraft.block.") && type>=0 && type<=41 && type!=22) {
				renderer.renderBlockByRenderType(block,x,y,z);
			} else {
				renderer.enableAO=false;int mask=access.projection.mask(x,y,z);
				if(mask==255) box(block,x,y,z,0,0,0,1,1,1);
				else for(int hy=0;hy<2;hy++) for(int hz=0;hz<2;hz++) for(int hx=0;hx<2;hx++)
					if((mask&(1<<(hx+2*hz+4*hy)))!=0) box(block,x,y,z,hx*.5,hy*.5,hz*.5,(hx+1)*.5,(hy+1)*.5,(hz+1)*.5);
			}
		} finally {
			block.setBlockBounds((float)a,(float)b,(float)c,(float)d,(float)e,(float)f);
			renderer.setRenderBounds(0,0,0,1,1,1);
		}
	}
	private void box(Block block,int x,int y,int z,double a,double b,double c,double d,double e,double f) {
		renderer.setRenderBounds(a,b,c,d,e,f);Tessellator tess=Tessellator.instance;tess.setBrightness(0xF000F0);
		int meta=access.getBlockMetadata(x,y,z);
		for(int side=0;side<6;side++) {
			if(side==0 && b==0 && access.opaque(x,y-1,z) || side==1 && e==1 && access.opaque(x,y+1,z)
					|| side==2 && c==0 && access.opaque(x,y,z-1) || side==3 && f==1 && access.opaque(x,y,z+1)
					|| side==4 && a==0 && access.opaque(x-1,y,z) || side==5 && d==1 && access.opaque(x+1,y,z)) continue;
			float shade=side==0?.5F:side==1?1F:side<=3?.8F:.6F;tess.setColorOpaque_F(shade,shade,shade);
			IIcon icon=renderer.getBlockIconFromSideAndMetadata(block,side,meta);
			switch(side) {
			case 0: renderer.renderFaceYNeg(block,x,y,z,icon);break;
			case 1: renderer.renderFaceYPos(block,x,y,z,icon);break;
			case 2: renderer.renderFaceZNeg(block,x,y,z,icon);break;
			case 3: renderer.renderFaceZPos(block,x,y,z,icon);break;
			case 4: renderer.renderFaceXNeg(block,x,y,z,icon);break;
			case 5: renderer.renderFaceXPos(block,x,y,z,icon);break;
			}
		}
	}
	void drawOpaque() { for(int list:opaque) GL11.glCallList(list); }
	void drawTransparent(double x,double y,double z) {
		if(Double.isNaN(cameraX) || Math.abs(x-cameraX)+Math.abs(y-cameraY)+Math.abs(z-cameraZ)>.25) {
			cameraX=x;cameraY=y;cameraZ=z;
			for(TransparentBlock block:transparent) block.distance=(block.x-x)*(block.x-x)+(block.y-y)*(block.y-y)+(block.z-z)*(block.z-z);
			Collections.sort(transparent,new Comparator<TransparentBlock>() {
				public int compare(TransparentBlock a,TransparentBlock b) { return Double.compare(b.distance,a.distance); }
			});
		}
		for(TransparentBlock block:transparent) GL11.glCallList(block.list);
	}
	void dispose() {
		for(int list:opaque) GL11.glDeleteLists(list,1);for(TransparentBlock block:transparent) GL11.glDeleteLists(block.list,1);
		opaque.clear();transparent.clear();
	}
}
