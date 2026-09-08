package com.hbm.render.block;

import com.hbm.blocks.machine.BlockIntelProjector;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

/** Shared solid geometry for the placed table and its inventory model. */
public class RenderProjectorTable implements ISimpleBlockRenderingHandler {
	private static final double[][] PARTS={
		{.125,0,.125,.875,.125,.875}, // plinth
		{.25,.125,.25,.75,.6875,.75}, // recessed pedestal
		{.1875,.1875,.1875,.8125,.25,.8125}, // lower service collar
		{.0625,.6875,.0625,.9375,.8125,.9375}, // table apron
		{.125,.8125,.125,.875,.84375,.875}, // recessed bed
		{0,.8125,0,1,.9375,.125},{0,.8125,.875,1,.9375,1},
		{0,.8125,.125,.125,.9375,.875},{.875,.8125,.125,1,.9375,.875},
		{0,.9375,0,.1875,1,.1875},{.8125,.9375,0,1,1,.1875},
		{0,.9375,.8125,.1875,1,1},{.8125,.9375,.8125,1,1,1},
		{.3125,.375,.235,.6875,.5625,.25}, // front service panel
		{.3125,.375,.75,.6875,.5625,.765625}
	};
	@Override public boolean renderWorldBlock(IBlockAccess world,int x,int y,int z,Block block,int modelId,RenderBlocks renderer) {
		boolean all=renderer.renderAllFaces;renderer.renderAllFaces=true;
		try {
			for(double[] p:PARTS) { bounds(renderer,p);renderer.renderStandardBlock(block,x,y,z); }
		} finally { renderer.renderAllFaces=all;renderer.setRenderBounds(0,0,0,1,1,1); }
		return true;
	}
	@Override public void renderInventoryBlock(Block block,int metadata,int modelId,RenderBlocks renderer) {
		GL11.glPushMatrix();GL11.glTranslated(-.5,-.5,-.5);
		Tessellator t=Tessellator.instance;
		try {
			for(double[] p:PARTS) {
				bounds(renderer,p);
				for(int side=0;side<6;side++) {
					t.startDrawingQuads();
					switch(side) {
					case 0:t.setNormal(0,-1,0);renderer.renderFaceYNeg(block,0,0,0,block.getIcon(side,metadata));break;
					case 1:t.setNormal(0,1,0);renderer.renderFaceYPos(block,0,0,0,block.getIcon(side,metadata));break;
					case 2:t.setNormal(0,0,-1);renderer.renderFaceZNeg(block,0,0,0,block.getIcon(side,metadata));break;
					case 3:t.setNormal(0,0,1);renderer.renderFaceZPos(block,0,0,0,block.getIcon(side,metadata));break;
					case 4:t.setNormal(-1,0,0);renderer.renderFaceXNeg(block,0,0,0,block.getIcon(side,metadata));break;
					case 5:t.setNormal(1,0,0);renderer.renderFaceXPos(block,0,0,0,block.getIcon(side,metadata));break;
					}
					t.draw();
				}
			}
		} finally { renderer.setRenderBounds(0,0,0,1,1,1);GL11.glPopMatrix(); }
	}
	private static void bounds(RenderBlocks r,double[] p) { r.setRenderBounds(p[0],p[1],p[2],p[3],p[4],p[5]); }
	@Override public boolean shouldRender3DInInventory(int modelId) { return true; }
	@Override public int getRenderId() { return BlockIntelProjector.renderID; }
}
