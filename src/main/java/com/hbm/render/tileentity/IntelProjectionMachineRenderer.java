package com.hbm.render.tileentity;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL11;
import com.hbm.saveddata.satellites.intel.*;
import com.hbm.tileentity.DoorDecl;
import com.hbm.tileentity.bomb.*;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;

/** Whitelisted visual-only tiles. They have no World, never tick, and cannot read the live base. */
final class IntelProjectionMachineRenderer {
	private static final class Model {
		IntelProjectionMachine snapshot;TileEntity tile;TileEntitySpecialRenderer renderer;
	}
	private final List<Model> models=new ArrayList<Model>();
	private final IntelProjection p;
	private final IntelProjectionView view;
	private final DoubleBuffer plane=GLAllocation.createDirectByteBuffer(32).asDoubleBuffer();
	private final RenderDoorGeneric doorTransforms=new RenderDoorGeneric();
	IntelProjectionMachineRenderer(IntelProjection p,IntelProjectionView view) {
		this.p=p;this.view=view;
		for(final IntelProjectionMachine s:p.machines.values()) {
			Model m=new Model();m.snapshot=s;
			if(s.kind==3) {
				TileEntityLaunchPad t=new TileEntityLaunchPad() { @Override public int getBlockMetadata() { return s.meta; } };
				t.toRender=s.missile();m.tile=t;m.renderer=new RenderLaunchPad();
			} else if(s.kind==4) {
				TileEntityLaunchPadLarge t=new TileEntityLaunchPadLarge() { @Override public int getBlockMetadata() { return s.meta; } };
				t.toRender=s.missile();t.formFactor=s.visual.getInteger("form");
				t.erected=s.visual.getBoolean("erected");t.readyToLoad=s.visual.getBoolean("ready");
				t.lift=t.prevLift=s.visual.getFloat("lift");t.erector=t.prevErector=s.visual.getFloat("angle");
				m.tile=t;m.renderer=new RenderLaunchPadLarge();
			} else if(s.kind==5) {
				TileEntityLaunchTable t=new TileEntityLaunchTable() { @Override public int getBlockMetadata() { return s.meta; } };
				t.padSize=PartSize.values()[s.visual.getInteger("size")];t.height=s.visual.getInteger("height");t.load=s.multipart();
				m.tile=t;m.renderer=new RenderLaunchTable();
			}
			if(m.renderer!=null) m.renderer.func_147497_a(TileEntityRendererDispatcher.instance);
			models.add(m);
		}
	}
	void draw() {
		if(models.isEmpty()) return;
		GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
		try {
			GL11.glEnable(GL11.GL_TEXTURE_2D);GL11.glEnable(GL11.GL_NORMALIZE);GL11.glColor4f(1,1,1,1);
			RenderHelper.enableStandardItemLighting();
			for(Model m:models) {
				IntelProjectionMachine s=m.snapshot;
				double low=0,high=Math.min(256,view.floor+1);
				int ground=p.groundLevel(s.x,s.z);
				if("surface".equals(view.mode)) low=ground;
				if("subsurface".equals(view.mode)) high=Math.min(high,ground);
				if(high<=low) continue;
				// Clip extended models too, even when their core is outside the selected section.
				clip(0,1,0,0,0);clip(1,-1,0,0,Math.min(p.width,view.cutAxis==0?view.cut-p.originX+1:p.width));
				clip(2,0,0,1,0);clip(3,0,0,-1,Math.min(p.depth,view.cutAxis==2?view.cut-p.originZ+1:p.depth));
				clip(4,0,1,0,-low);clip(5,0,-1,0,high);
				if(m.renderer!=null) m.renderer.renderTileEntityAt(m.tile,s.x,s.y,s.z,0);
				else hatch(s);
			}
		} finally { GL11.glPopAttrib(); }
	}
	private void clip(int index,double a,double b,double c,double d) {
		plane.clear();plane.put(new double[]{a,b,c,d});plane.flip();
		GL11.glClipPlane(GL11.GL_CLIP_PLANE0+index,plane);GL11.glEnable(GL11.GL_CLIP_PLANE0+index);
	}
	private void hatch(IntelProjectionMachine s) {
		DoorDecl door=s.kind==1?DoorDecl.SILO_HATCH:DoorDecl.SILO_HATCH_LARGE;
		GL11.glPushMatrix();
		try {
			GL11.glTranslated(s.x+.5,s.y,s.z+.5);
			int direction=s.meta-10;
			GL11.glRotatef(direction==2?90:direction==4?180:direction==3?270:0,0,1,0);
			door.doOffsetTransform();
			for(String part:door.getModel().getPartNames()) {
				if(!door.doesRender(part,false)) continue;
				GL11.glPushMatrix();
				Minecraft.getMinecraft().getTextureManager().bindTexture(door.getTextureForPart(0,part));
				doorTransforms.doPartTransform(door,part,s.visual.getInteger("open"),false);
				door.getModel().renderPart(part);GL11.glPopMatrix();
			}
		} finally { GL11.glPopMatrix(); }
	}
}
