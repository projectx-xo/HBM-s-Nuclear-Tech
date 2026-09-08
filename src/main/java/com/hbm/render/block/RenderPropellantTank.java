package com.hbm.render.block;

import com.hbm.blocks.machine.BlockPropellantTank;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/** A horizontal pressure vessel baked into the chunk mesh, including inventory rendering. */
public class RenderPropellantTank extends TileEntitySpecialRenderer implements ISimpleBlockRenderingHandler {
	@Override public void renderTileEntityAt(TileEntity te, double x, double y, double z, float partial) { }
	@Override public int getRenderId() { return BlockPropellantTank.tankRenderID; }
	@Override public boolean shouldRender3DInInventory(int id) { return true; }
	@Override public void renderInventoryBlock(Block block, int meta, int id, RenderBlocks renderer) {
		GL11.glPushMatrix();
		GL11.glTranslated(-0.5, -0.5, -0.5);
		Tessellator.instance.startDrawingQuads();
		draw(block, block.getIcon(0, 0), 0);
		Tessellator.instance.draw();
		GL11.glPopMatrix();
	}
	@Override public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int id, RenderBlocks renderer) {
		Tessellator t = Tessellator.instance;
		t.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
		t.addTranslation(x, y, z);
		draw(block, renderer.hasOverrideBlockTexture() ? renderer.overrideBlockTexture : block.getIcon(0, 0), world.getBlockMetadata(x, y, z));
		t.addTranslation(-x, -y, -z);
		return true;
	}
	private void draw(Block block, IIcon icon, int facing) {
		// Narrow rings at the ends approximate domed caps; the central shell is cylindrical.
		double[] x = {0.02, 0.06, 0.13, 0.21, 0.79, 0.87, 0.94, 0.98};
		double[] r = {0, 0.12, 0.20, 0.25, 0.25, 0.20, 0.12, 0};
		for(int ring = 0; ring < x.length - 1; ring++) cylinder(icon, facing, x[ring], x[ring + 1], r[ring], r[ring + 1], 0.88F);
		cylinder(icon, facing, .24, .28, .265, .265, .55F);
		cylinder(icon, facing, .72, .76, .265, .265, .55F);
		box(icon, facing, .20, .04, .18, .30, .27, .82, .45F);
		box(icon, facing, .70, .04, .18, .80, .27, .82, .45F);
		box(icon, facing, .48, .73, .44, .56, .82, .56, .5F);
		BlockPropellantTank tank = (BlockPropellantTank) block;
		int bars = tank.creativeFuel >= 0 ? 4 : tank.tier + 1;
		for(int i = 0; i < bars; i++) {
			box(icon, facing, .38 + i * .06, .42, .242, .405 + i * .06, .55, .252, .18F);
			box(icon, facing, .38 + i * .06, .42, .748, .405 + i * .06, .55, .758, .18F);
		}
	}
	private void cylinder(IIcon icon, int facing, double a, double b, double ra, double rb, float shade) {
		for(int i = 0; i < 16; i++) {
			double p = i * Math.PI / 8, q = (i + 1) * Math.PI / 8;
			quad(icon, facing, new double[][] {{a,.48+ra*Math.cos(p),.5+ra*Math.sin(p)}, {a,.48+ra*Math.cos(q),.5+ra*Math.sin(q)}, {b,.48+rb*Math.cos(q),.5+rb*Math.sin(q)}, {b,.48+rb*Math.cos(p),.5+rb*Math.sin(p)}}, shade);
		}
	}
	private void box(IIcon icon, int facing, double a, double b, double c, double d, double e, double f, float shade) {
		double[][] v = {{a,b,c},{d,b,c},{d,e,c},{a,e,c},{a,b,f},{d,b,f},{d,e,f},{a,e,f}};
		int[][] faces = {{0,3,2,1},{4,5,6,7},{0,4,7,3},{1,2,6,5},{3,7,6,2},{0,1,5,4}};
		for(int[] face : faces) quad(icon, facing, new double[][] {v[face[0]],v[face[1]],v[face[2]],v[face[3]]}, shade);
	}
	private void quad(IIcon icon, int facing, double[][] v, float shade) {
		Tessellator t = Tessellator.instance;
		double ux=v[2][0]-v[0][0], uy=v[2][1]-v[0][1], uz=v[2][2]-v[0][2];
		double vx=v[3][0]-v[1][0], vy=v[3][1]-v[1][1], vz=v[3][2]-v[1][2];
		double nx=uy*vz-uz*vy, ny=uz*vx-ux*vz, nz=ux*vy-uy*vx;
		double length=Math.sqrt(nx*nx+ny*ny+nz*nz);
		if(length == 0) return;
		double angle=(facing & 3)*Math.PI/2, cos=Math.cos(angle), sin=Math.sin(angle);
		t.setNormal((float)((nx*cos-nz*sin)/length), (float)(ny/length), (float)((nx*sin+nz*cos)/length));
		float light=(float)(.75+.25*Math.max(0,ny/length));
		t.setColorOpaque_F(shade*light, shade*light, shade*light);
		for(int i=0;i<4;i++) {
			double x=v[i][0]-.5,z=v[i][2]-.5;
			t.addVertexWithUV(.5+x*cos-z*sin,v[i][1],.5+x*sin+z*cos, i==0||i==3?icon.getMinU():icon.getMaxU(),i<2?icon.getMinV():icon.getMaxV());
		}
	}
}
