package com.hbm.inventory.gui;

import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.saveddata.satellites.intel.IntelProjectionView;
import com.hbm.tileentity.machine.TileEntityIntelProjector;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;

public class GUIIntelProjector extends GuiContainer {
	private final TileEntityIntelProjector tile;
	public GUIIntelProjector(EntityPlayer player,TileEntityIntelProjector tile) {
		super(tile.provideContainer(0,player,tile.getWorldObj(),tile.xCoord,tile.yCoord,tile.zCoord));
		this.tile=tile;xSize=344;ySize=228;
	}
	@Override public void initGui() {
		super.initGui();buttonList.clear();
		String[] labels={"Exterior","Interior","Cutaway","Floor -","Floor +","Rotate <","Rotate >","Smaller","Larger","Terrain",
				"Previous finding","Next finding","All findings","Close","Cut -","Cut +","Cut axis"};
		int[][] layout={{0,0},{1,0},{2,0},{0,1},{1,1},{0,2},{1,2},{0,3},{1,3},{2,3},{0,4},{1,4},{2,4},{2,5},{0,5},{1,5},{2,1}};
		for(int i=0;i<labels.length;i++) buttonList.add(new GuiButton(i,guiLeft+12+layout[i][0]*108,guiTop+52+layout[i][1]*23,104,20,labels[i]));
	}
	@Override protected void actionPerformed(GuiButton button) {
		if(button.id==13) { mc.thePlayer.closeScreen();return; }
		if(tile.displayed==null) return;
		IntelProjectionView v=tile.view;String action="",value="";int count=tile.displayed.findings.size();
		switch(button.id) {
		case 0:case 1:case 2:action="view";value=new String[]{"exterior","interior","cutaway"}[button.id];break;
		case 3:case 4:action="floor";value=""+Math.max(0,Math.min(255,v.floor+(button.id==3?-1:1)));break;
		case 5:case 6:action="rotate";value=""+(((int)v.rotation+(button.id==5?-15:15))%360);break;
		case 7:case 8:action="scale";value=""+Math.max(2,Math.min(12,(int)v.size+(button.id==7?-1:1)));break;
		case 9:action="terrain";value=v.terrain?"off":"on";break;
		case 10:case 11:if(count==0)return;action="select";value=""+(button.id==10?(v.selected<=1?count:v.selected-1):(v.selected>=count?1:v.selected+1));break;
		case 12:action="select";value="all";break;
		case 14:case 15:action="cut";value=(v.cutAxis==0?"x:":"z:")+(v.cut+(button.id==14?-1:1));break;
		case 16:action="cut";value=v.cutAxis==0?"z:"+tile.displayed.targetZ:"x:"+tile.displayed.targetX;break;
		}
		NBTTagCompound n=new NBTTagCompound();n.setString("action",action);n.setString("value",value);
		PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(n,tile.xCoord,tile.yCoord,tile.zCoord));
	}
	@Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
		drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xEF101C26);
		drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+2,0xFF47CFEA);
		fontRendererObj.drawString("INTELLIGENCE PROJECTION TABLE",guiLeft+12,guiTop+12,0x75E3FF);
		fontRendererObj.drawString(tile.view.mode+" | floor "+tile.view.floor+" | terrain "+(tile.view.terrain?"on":"off"),guiLeft+12,guiTop+29,0xD8E4E9);
		String detail=tile.displayed==null?tile.status():tile.view.selected==0?"All findings | numbers match STRATCOM scan results":tile.finding(tile.view.selected);
		fontRendererObj.drawSplitString(detail,guiLeft+12,guiTop+194,xSize-24,0xD8E4E9);
	}
	@Override protected void drawGuiContainerForegroundLayer(int x,int y) { }
}
