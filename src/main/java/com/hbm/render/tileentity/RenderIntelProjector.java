package com.hbm.render.tileentity;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.lwjgl.opengl.GL11;
import com.hbm.saveddata.satellites.intel.*;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import java.nio.FloatBuffer;
import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;

public class RenderIntelProjector extends TileEntitySpecialRenderer {
	private static final class Cache {
		String key;
		IntelProjectionBlockRenderer builder;
		void dispose() { if(builder!=null) builder.dispose(); }
	}
	private int textureGeneration;
	private final FloatBuffer matrixBuffer=BufferUtils.createFloatBuffer(16);
	private final Matrix4f modelView=new Matrix4f();
	private final Map<TileEntityIntelProjector,Cache> caches=new LinkedHashMap<TileEntityIntelProjector,Cache>(8,.75F,true);

	public RenderIntelProjector() { MinecraftForge.EVENT_BUS.register(this); }
	@SubscribeEvent public void texturesReloaded(TextureStitchEvent.Post event) { textureGeneration++; }

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
			if(tile.displayed==null || !tile.displayed.projection.hasBlockStates) { label(tile.status(),0,1.4,0,.012F,0x78DFF7);return; }
			IntelProjection p=tile.displayed.projection;IntelProjectionView v=tile.view;
			Cache cache=cache(tile,p,v);
			if(!cache.builder.ready) { label("Building block model...",0,1.4,0,.012F,0x78DFF7);return; }
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
			bindTexture(TextureMap.locationBlocksTexture);GL11.glEnable(GL11.GL_TEXTURE_2D);
			GL11.glEnable(GL11.GL_ALPHA_TEST);GL11.glAlphaFunc(GL11.GL_GREATER,.1F);GL11.glEnable(GL11.GL_DEPTH_TEST);
			GL11.glDepthFunc(GL11.GL_LEQUAL);GL11.glDepthMask(true);GL11.glDisable(GL11.GL_BLEND);
			GL11.glColor4f(1,1,1,1);cache.builder.drawOpaque();
			GL11.glEnable(GL11.GL_BLEND);GL11.glDepthMask(false);
			// Invert the actual camera transform, including third-person offset and the miniature's rotation/scale.
			matrixBuffer.clear();GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX,matrixBuffer);modelView.load(matrixBuffer);
			Matrix4f.invert(modelView,modelView);cache.builder.drawTransparent(modelView.m30,modelView.m31,modelView.m32);
			GL11.glDisable(GL11.GL_TEXTURE_2D);GL11.glDisable(GL11.GL_ALPHA_TEST);
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
			if(cache.builder.truncated) label("Block limit reached - use a floor/side cut",(minX+maxX)/2,maxY+1,(minZ+maxZ)/2,.015F/scale,0xFFC964,v.rotation);
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
		String key=p.id+":"+v.floor+":"+v.cutAxis+":"+v.cut+":"+v.terrain+":"+textureGeneration;
		if(!key.equals(c.key)) { c.dispose();c.key=key;c.builder=new IntelProjectionBlockRenderer(p,v); }
		c.builder.step();
		return c;
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
