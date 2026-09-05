package com.hbm.render.tileentity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import com.hbm.saveddata.satellites.intel.*;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

public class RenderIntelProjector extends TileEntitySpecialRenderer {
	private static final class Cache {
		String key;
		IntelProjectionMesh.Builder builder;
		int lists;
		boolean ready;
		void dispose() { if(lists!=0) GL11.glDeleteLists(lists,3);lists=0; }
	}
	private final Map<TileEntityIntelProjector,Cache> caches=new LinkedHashMap<TileEntityIntelProjector,Cache>(8,.75F,true);

	@Override public void renderTileEntityAt(TileEntity entity,double x,double y,double z,float partial) {
		TileEntityIntelProjector tile=(TileEntityIntelProjector)entity;
		float lightX=OpenGlHelper.lastBrightnessX,lightY=OpenGlHelper.lastBrightnessY;
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);GL11.glPushMatrix();
		try {
			OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,240,240);
			GL11.glTranslated(x+.5,y,z+.5);
			GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_CULL_FACE);
			GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDisable(GL11.GL_ALPHA_TEST);
			// A dark emitter plate and a narrow illuminated rim on the physical block.
			GL11.glColor4f(.035F,.065F,.085F,1);GL11.glBegin(GL11.GL_QUADS);
			vertex(-.47,1.005,-.47);vertex(-.47,1.005,.47);vertex(.47,1.005,.47);vertex(.47,1.005,-.47);GL11.glEnd();
			GL11.glColor4f(.15F,.8F,1,.9F);GL11.glLineWidth(2);GL11.glBegin(GL11.GL_LINE_LOOP);
			vertex(-.43,1.008,-.43);vertex(-.43,1.008,.43);vertex(.43,1.008,.43);vertex(.43,1.008,-.43);GL11.glEnd();
			if(tile.displayed==null) { label(tile.status(),0,1.4,0,.012F,0x78DFF7);return; }
			IntelProjection p=tile.displayed.projection;IntelProjectionView v=tile.view;
			Cache cache=cache(tile,p,v);
			if(!cache.ready) { label("Building scan geometry...",0,1.4,0,.012F,0x78DFF7);return; }
			// These bounds never change with clipping or selection: coordinates stay aligned.
			double[] bounds=p.bounds(v.terrain);
			double minX=bounds[0],minY=bounds[1],minZ=bounds[2],maxX=bounds[3],maxY=bounds[4],maxZ=bounds[5];
			for(IntelFinding f:tile.displayed.findings) {
				minX=Math.min(minX,f.minX-p.originX);maxX=Math.max(maxX,f.maxX-p.originX+1);
				minZ=Math.min(minZ,f.minZ-p.originZ);maxZ=Math.max(maxZ,f.maxZ-p.originZ+1);
				minY=Math.min(minY,f.minY);maxY=Math.max(maxY,f.maxY+2);
			}
			double scale=v.size/Math.max(1,Math.max(maxY-minY,Math.max(maxX-minX,maxZ-minZ)));
			GL11.glTranslated(0,1.35,0);GL11.glRotatef(v.rotation,0,1,0);GL11.glScaled(scale,scale,scale);
			GL11.glTranslated(-(minX+maxX)/2,-minY,-(minZ+maxZ)/2);
			GL11.glDepthMask(false);GL11.glColor4f(.05F,.55F,.85F,.25F);GL11.glLineWidth(1);
			GL11.glBegin(GL11.GL_LINES);
			for(double a=minX;a<=maxX;a+=2) { vertex(a,minY-.15,minZ);vertex(a,minY-.15,maxZ); }
			for(double a=minZ;a<=maxZ;a+=2) { vertex(minX,minY-.15,a);vertex(maxX,minY-.15,a); }GL11.glEnd();
			// Depth prepass makes the exterior readable. Cutting geometry exposes the actual room surfaces.
			GL11.glDepthMask(true);GL11.glColorMask(false,false,false,false);GL11.glCallList(cache.lists);
			GL11.glColorMask(true,true,true,true);GL11.glDepthMask(false);GL11.glDepthFunc(GL11.GL_LEQUAL);
			GL11.glColor4f(.04F,.45F,.7F,.18F);GL11.glCallList(cache.lists);
			GL11.glColor4f(.1F,.7F,1,.08F);GL11.glCallList(cache.lists+1);
			GL11.glColor4f(.2F,.8F,1,.85F);GL11.glLineWidth(1.2F);GL11.glCallList(cache.lists+2);
			// Symbols are always readable, including coincident missile/launcher coordinates and clipped equipment.
			GL11.glDisable(GL11.GL_DEPTH_TEST);
			int index=0;
			for(IntelFinding f:tile.displayed.findings) {
				index++;
				boolean selected=v.selected==index, context=f.targetType.isEmpty();
				if(context && !selected) continue;
				int color=selected?0xFFFFFF:f.classification==IntelClassification.MISSILE?0xFF785F:
					f.classification==IntelClassification.SILO_HATCH?0xC2A0FF:0xFFC964;
				float alpha=v.selected==0 || selected?.95F:.3F;
				double mx=(f.minX+f.maxX)*.5-p.originX+.5,my=(f.minY+f.maxY)*.5+.5,mz=(f.minZ+f.maxZ)*.5-p.originZ+.5;
				GL11.glColor4f((color>>16&255)/255F,(color>>8&255)/255F,(color&255)/255F,alpha);
				GL11.glLineWidth(selected?2.5F:1.8F);GL11.glBegin(GL11.GL_LINES);
				if(f.classification==IntelClassification.MISSILE) {
					vertex(mx,my-1,mz);vertex(mx,my+1.5,mz);vertex(mx-.6,my+.5,mz);vertex(mx,my+1.5,mz);
					vertex(mx+.6,my+.5,mz);vertex(mx,my+1.5,mz);
				} else {
					double r=f.classification==IntelClassification.SILO_HATCH?.8:1.3;
					vertex(mx-r,my,mz);vertex(mx,my,mz+r);vertex(mx,my,mz+r);vertex(mx+r,my,mz);
					vertex(mx+r,my,mz);vertex(mx,my,mz-r);vertex(mx,my,mz-r);vertex(mx-r,my,mz);
				}
				GL11.glEnd();
				if(selected || v.selected==0) {
					// Separate labels only; the symbols themselves remain at their scan coordinates.
					double offset=f.classification==IntelClassification.MISSILE?2.4:f.classification==IntelClassification.SILO_HATCH?1.2:-1.3;
					label("#"+index+(selected?" "+f.classification.name():""),mx,my+offset,mz,.018F/scale,color,v.rotation);
				}
			}
			if(cache.builder.truncated) label("Geometry limit reached - use a floor/side cut",(minX+maxX)/2,maxY+1,(minZ+maxZ)/2,.015F/scale,0xFFC964,v.rotation);
		} finally { OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,lightX,lightY);GL11.glPopMatrix();GL11.glPopAttrib(); }
	}
	private Cache cache(TileEntityIntelProjector tile,IntelProjection p,IntelProjectionView v) {
		Iterator<Map.Entry<TileEntityIntelProjector,Cache>> it=caches.entrySet().iterator();
		while(it.hasNext()) { Map.Entry<TileEntityIntelProjector,Cache> e=it.next();
			if(e.getKey().isInvalid() || e.getKey().getWorldObj()!=tile.getWorldObj()) { e.getValue().dispose();it.remove(); }
		}
		Cache c=caches.get(tile);
		if(c==null) {
			if(caches.size()>=8) { it=caches.entrySet().iterator();Map.Entry<TileEntityIntelProjector,Cache> e=it.next();e.getValue().dispose();it.remove(); }
			c=new Cache();caches.put(tile,c);
		}
		String key=p.id+":"+v.floor+":"+v.cutAxis+":"+v.cut+":"+v.terrain;
		if(!key.equals(c.key)) { c.dispose();c.key=key;c.ready=false;c.builder=new IntelProjectionMesh.Builder(p,v.floor,v.cutAxis,v.cut,v.terrain); }
		if(!c.ready) {
			long deadline=System.nanoTime()+3000000L;
			do {
				if(c.builder.step(1)) {
					c.lists=GLAllocation.generateDisplayLists(3);
					for(int pass=0;pass<3;pass++) {
						GL11.glNewList(c.lists+pass,GL11.GL_COMPILE);GL11.glBegin(pass==2?GL11.GL_LINES:GL11.GL_QUADS);
						for(IntelProjectionMesh.Quad q:c.builder.quads) if(pass==2 || q.glass==(pass==1)) quad(q,pass==2);
						GL11.glEnd();GL11.glEndList();
					}
					c.ready=true;break;
				}
			} while(System.nanoTime()<deadline);
		}
		return c;
	}
	private static void quad(IntelProjectionMesh.Quad q,boolean edges) {
		int u=(q.axis+1)%3,v=(q.axis+2)%3;
		double[][] points={{q.x,q.y,q.z},{q.x,q.y,q.z},{q.x,q.y,q.z},{q.x,q.y,q.z}};
		points[1][u]+=q.width;points[2][u]+=q.width;points[2][v]+=q.height;points[3][v]+=q.height;
		for(int i=0;i<4;i++) { vertex(points[i][0],points[i][1],points[i][2]);
			if(edges) { double[] next=points[(i+1)%4];vertex(next[0],next[1],next[2]); }
		}
	}
	private static void vertex(double x,double y,double z) { GL11.glVertex3d(x,y,z); }
	private void label(String text,double x,double y,double z,double size,int color) {
		label(text,x,y,z,size,color,0);
	}
	private void label(String text,double x,double y,double z,double size,int color,float rotation) {
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT);GL11.glPushMatrix();
		GL11.glTranslated(x,y,z);
		GL11.glRotatef(-RenderManager.instance.playerViewY-rotation,0,1,0);GL11.glRotatef(RenderManager.instance.playerViewX,1,0,0);
		GL11.glScaled(-size,-size,size);GL11.glEnable(GL11.GL_TEXTURE_2D);
		Minecraft.getMinecraft().fontRenderer.drawString(text,-Minecraft.getMinecraft().fontRenderer.getStringWidth(text)/2,0,color);
		GL11.glPopMatrix();GL11.glPopAttrib();
	}
}
