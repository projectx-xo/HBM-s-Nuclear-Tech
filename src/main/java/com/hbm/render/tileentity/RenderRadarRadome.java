package com.hbm.render.tileentity;

import org.lwjgl.opengl.GL11;
import com.hbm.blocks.ModBlocks;
import com.hbm.render.item.ItemRenderBase;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.IItemRenderer;

/** Stationary panelled radome; shared geometry for the placed machine and item. */
public class RenderRadarRadome extends TileEntitySpecialRenderer implements IItemRendererProvider {
	private static final int SEGMENTS = 24;
	private static final int RINGS = 12;
	private static final double DOME_RADIUS = 3.45;
	private static final double DOME_CENTER_Y = 8.15 + (3 - DOME_RADIUS) * Math.cos(2.15);
	private static final double[][][] PANELS = buildPanels();

	private static double[] point(int ring, int segment) {
		double theta = ring * 2.15 / RINGS;
		double phi = (segment + (ring % 2) * 0.5) * Math.PI * 2 / SEGMENTS;
		return new double[] {DOME_RADIUS * Math.sin(theta) * Math.cos(phi), DOME_CENTER_Y + DOME_RADIUS * Math.cos(theta), DOME_RADIUS * Math.sin(theta) * Math.sin(phi)};
	}
	private static double[][][] buildPanels() {
		double[][][] panels = new double[RINGS * SEGMENTS * 2][][];
		int n = 0;
		for(int r = 0; r < RINGS; r++) for(int j = 0; j < SEGMENTS; j++) {
			double[] a = point(r,j), b = point(r,j+1), c = point(r+1,j), d = point(r+1,j+1);
			panels[n++] = new double[][] {a,c,b};
			panels[n++] = new double[][] {b,c,d};
		}
		return panels;
	}
	private static void vertex(double[] v) { Tessellator.instance.addVertex(v[0],v[1],v[2]); }
	private static void cylinder(double radius, double low, double high, int color) {
		Tessellator t = Tessellator.instance;
		t.startDrawingQuads();
		for(int i=0;i<SEGMENTS;i++) {
			double a=i*Math.PI*2/SEGMENTS,b=(i+1)*Math.PI*2/SEGMENTS;
			t.setColorOpaque_I(color); t.setNormal((float)Math.cos((a+b)/2),0,(float)Math.sin((a+b)/2));
			t.addVertex(radius*Math.cos(a),low,radius*Math.sin(a)); t.addVertex(radius*Math.cos(a),high,radius*Math.sin(a));
			t.addVertex(radius*Math.cos(b),high,radius*Math.sin(b)); t.addVertex(radius*Math.cos(b),low,radius*Math.sin(b));
			t.setNormal(0,1,0);
			t.addVertex(0,high,0);t.addVertex(radius*Math.cos(a),high,radius*Math.sin(a));
			t.addVertex(radius*Math.cos(b),high,radius*Math.sin(b));t.addVertex(0,high,0);
		}
		t.draw();
	}
	private static void line(double x,double y,double z,double a,double b,double c) {
		GL11.glVertex3d(x,y,z); GL11.glVertex3d(a,b,c);
	}
	private static void model() {
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT | GL11.GL_LIGHTING_BIT | GL11.GL_POLYGON_BIT);
		GL11.glDisable(GL11.GL_TEXTURE_2D); GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		cylinder(2.42,0,0.18,0x454b4c);
		cylinder(2.17,0.18,6.35,0x898d86);
		cylinder(3.25,6.35,6.48,0x50595b);
		cylinder(2.98,6.48,6.58,0xb2b6b2);
		Tessellator t=Tessellator.instance;
		GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);GL11.glPolygonOffset(1F,1F);
		GL11.glShadeModel(GL11.GL_SMOOTH);
		t.startDrawing(GL11.GL_TRIANGLES);
		for(int i=0;i<PANELS.length;i++) {
			double[][] p=PANELS[i];
			t.setColorOpaque(235,235,231);
			for(double[] v:p) {
				t.setNormal((float)(v[0]/DOME_RADIUS),(float)((v[1]-DOME_CENTER_Y)/DOME_RADIUS),(float)(v[2]/DOME_RADIUS));
				vertex(v);
			}
		}
		t.draw();
		GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
		t.startDrawingQuads();
		for(int i=0;i<8;i++) {
			double angle=i*Math.PI/4, nx=Math.cos(angle), nz=Math.sin(angle);
			t.setNormal((float)nx,0,(float)nz);t.setColorOpaque_I(0x283c43);
			for(double[] v:new double[][] {{-0.36,4.6},{-0.36,5.45},{0.36,5.45},{0.36,4.6}})
				t.addVertex(2.18*nx-v[0]*nz,v[1],2.18*nz+v[0]*nx);
		}
		t.draw();

		// A faint dark joint over the lit shell, rather than an unlit wireframe.
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		GL11.glDisable(GL11.GL_LIGHTING);GL11.glDisable(GL11.GL_ALPHA_TEST);
		GL11.glEnable(GL11.GL_BLEND);GL11.glBlendFunc(GL11.GL_SRC_ALPHA,GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glDepthMask(false);GL11.glLineWidth(1F);
		GL11.glColor4f(0.15F,0.17F,0.16F,0.035F);GL11.glBegin(GL11.GL_LINES);
		for(double[][] p:PANELS) for(int i=0;i<3;i++) {
			double[] a=p[i],b=p[(i+1)%3];
			line(a[0]*1.0003,DOME_CENTER_Y+(a[1]-DOME_CENTER_Y)*1.0003,a[2]*1.0003,b[0]*1.0003,DOME_CENTER_Y+(b[1]-DOME_CENTER_Y)*1.0003,b[2]*1.0003);
		}
		GL11.glEnd();
		GL11.glPopAttrib();GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glColor3f(0.25F,0.29F,0.30F);GL11.glLineWidth(2F);GL11.glBegin(GL11.GL_LINES);
		for(int i=0;i<SEGMENTS;i++) {
			double a=i*Math.PI*2/SEGMENTS,b=(i+1)*Math.PI*2/SEGMENTS;
			double x=3.18*Math.cos(a),z=3.18*Math.sin(a),xx=3.18*Math.cos(b),zz=3.18*Math.sin(b);
			line(x,6.48,z,x,7.02,z);line(x,7.02,z,xx,7.02,zz);line(x,6.75,z,xx,6.75,zz);
			line(2.18*Math.cos(a),0.2,2.18*Math.sin(a),2.18*Math.cos(a),6.33,2.18*Math.sin(a));
		}
		// Front access ladder and roof lightning rod.
		line(-0.23,0,3.2,-0.23,6.6,3.2);line(0.23,0,3.2,0.23,6.6,3.2);
		for(double y=0.15;y<6.6;y+=0.2) line(-0.23,y,3.2,0.23,y,3.2);
		for(double y=1;y<6;y+=2) { line(-0.23,y,2.18,-0.23,y,3.2);line(0.23,y,2.18,0.23,y,3.2); }
		line(0,DOME_CENTER_Y+DOME_RADIUS-0.02,0,0,DOME_CENTER_Y+DOME_RADIUS+0.45,0);
		GL11.glEnd();
		cylinder(0.07,DOME_CENTER_Y+DOME_RADIUS-0.01,DOME_CENTER_Y+DOME_RADIUS+0.1,0xb84035);
		GL11.glPopAttrib();
	}
	@Override public void renderTileEntityAt(TileEntity tile,double x,double y,double z,float partial) {
		GL11.glPushMatrix();GL11.glTranslated(x+0.5,y,z+0.5);model();GL11.glPopMatrix();
	}
	@Override public Item getItemForRenderer() { return Item.getItemFromBlock(ModBlocks.machine_radar_radome); }
	@Override public IItemRenderer getRenderer() {
		return new ItemRenderBase() {
			@Override public void renderInventory() { GL11.glTranslated(0,-5,0);GL11.glScaled(1.1,1.1,1.1); }
			@Override public void renderNonInv() { GL11.glScaled(0.4,0.4,0.4); }
			@Override public void renderCommonWithStack(ItemStack stack) { model(); }
		};
	}
}
