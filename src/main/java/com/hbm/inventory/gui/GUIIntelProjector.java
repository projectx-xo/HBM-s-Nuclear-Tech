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
		this.tile=tile;xSize=300;ySize=232;
	}
	@Override public void initGui() {
		super.initGui();buttonList.clear();
		add(0,12,38,90,"Surface");add(1,105,38,90,"Subsurface");add(2,198,38,90,"Combined");
		add(5,92,72,22,"<");add(6,164,72,22,">");
		add(7,92,96,22,"-");add(8,164,96,22,"+");add(9,200,96,88,"Terrain");
		add(3,92,120,22,"-");add(4,164,120,22,"+");
		add(14,78,144,22,"-");add(15,176,144,22,"+");add(16,200,72,88,"Section: off");
		add(17,200,120,88,"All floors");
		add(10,200,174,22,"<");add(11,226,174,22,">");add(12,252,174,36,"All");
		add(13,270,10,18,"x");
	}
	private void add(int id,int x,int y,int width,String label) {
		buttonList.add(new PanelButton(id,guiLeft+x,guiTop+y,width,label));
	}
	@Override public void updateScreen() {
		super.updateScreen();refreshControls();
	}
	private void refreshControls() {
		boolean ready=tile.displayed!=null,cut=tile.view.cutAxis!=-1;
		int count=tile.visibleFindingCount();
		for(Object entry:buttonList) {
			PanelButton button=(PanelButton)entry;
			button.enabled=button.id==13 || ready;
			button.visible=true;button.selected=false;
			if(button.id<3) button.selected=tile.view.mode.equals(new String[]{"surface","subsurface","combined"}[button.id]);
			if(button.id==14 || button.id==15) button.visible=cut;
			if(button.id>=10 && button.id<=12) button.enabled=count>0;
			if(button.id==9) { button.selected=tile.view.terrain;button.displayString="Terrain: "+(tile.view.terrain?"on":"off"); }
			if(button.id==16) { button.selected=cut;button.displayString="Section: "+(!cut?"off":tile.view.cutAxis==0?"X":"Z"); }
			if(button.id==17) button.enabled=ready && tile.view.floor!=255;
			if(button.id==3 && tile.view.floor<=0 || button.id==4 && tile.view.floor>=255
				|| button.id==7 && tile.view.size<=2 || button.id==8 && tile.view.size>=12) button.enabled=false;
		}
	}
	@Override protected void actionPerformed(GuiButton button) {
		if(button.id==13) { mc.thePlayer.closeScreen();return; }
		if(tile.displayed==null) return;
		IntelProjectionView v=tile.view;String action="",value="";int count=tile.visibleFindingCount();
		switch(button.id) {
		case 0:case 1:case 2:action="view";value=new String[]{"surface","subsurface","combined"}[button.id];break;
		case 3:case 4:action="floor";value=""+Math.max(0,Math.min(255,(v.floor==255 && button.id==3?tile.displayed.projection.highestVisibleLevel(v.mode,v.terrain):v.floor)+(button.id==3?-1:1)));break;
		case 5:case 6:action="rotate";value=""+(((int)v.rotation+(button.id==5?-15:15))%360);break;
		case 7:case 8:action="scale";value=""+Math.max(2,Math.min(12,(int)v.size+(button.id==7?-1:1)));break;
		case 9:action="terrain";value=v.terrain?"off":"on";break;
		case 10:case 11:if(count==0)return;action="select";value=""+tile.nextVisibleFinding(button.id==10?-1:1);break;
		case 12:action="select";value="all";break;
		case 14:case 15:action="cut";value=(v.cutAxis==0?"x:":"z:")+(v.cut+(button.id==14?-1:1));break;
		case 16:action="cut";value=v.cutAxis==-1?"x:"+tile.displayed.targetX:v.cutAxis==0?"z:"+tile.displayed.targetZ:"none";break;
		case 17:action="floor";value="all";break;
		}
		NBTTagCompound n=new NBTTagCompound();n.setString("action",action);n.setString("value",value);
		PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(n,tile.xCoord,tile.yCoord,tile.zCoord));
	}
	@Override protected void drawGuiContainerBackgroundLayer(float partial,int mouseX,int mouseY) {
		refreshControls();
		drawRect(guiLeft,guiTop,guiLeft+xSize,guiTop+ySize,0xFF171B1D);
		drawRect(guiLeft+1,guiTop+1,guiLeft+xSize-1,guiTop+ySize-1,0xFF717777);
		drawRect(guiLeft+3,guiTop+3,guiLeft+xSize-3,guiTop+ySize-3,0xFF303637);
		text("PROJECTION TABLE",16,11,0xE4E5DB);
		text("STRATCOM / SCENE CONTROLS",16,23,0x9DA9A6);
		drawRect(guiLeft+12,guiTop+64,guiLeft+288,guiTop+65,0xFF525B5B);
		text("Rotation",16,77,0xBDC6C3);text("Scale",16,101,0xBDC6C3);
		boolean cut=tile.view.cutAxis!=-1,ready=tile.displayed!=null;
		text("Floor",16,125,0xBDC6C3);
		if(cut) { text("Section",16,149,0xBDC6C3);center(ready?Integer.toString(tile.view.cut):"--",139,149); }
		else text("Section off / full depth",16,149,0x9DA9A6);
		center(ready?Integer.toString(((int)tile.view.rotation+360)%360):"--",139,77);
		center(ready?(int)tile.view.size+"m":"--",139,101);
		center(ready?(tile.view.floor==255?"All":Integer.toString(tile.view.floor)):"--",139,125);
		text("FINDINGS",16,180,0x9DA9A6);
		int count=tile.visibleFindingCount();
		text(count==0?"None":tile.view.selected==0?count+" total":"#"+tile.view.selected,88,180,0xE4E5DB);
		drawRect(guiLeft+12,guiTop+198,guiLeft+288,guiTop+224,0xFF1D2425);
		String detail=!ready?(tile.sceneId.isEmpty()?"No scan loaded. Send a combined scan from STRATCOM.":"Receiving scan geometry..."):
			!tile.displayed.projection.hasBlockStates?"Legacy scan. Rescan to display block textures.":
			tile.view.selected>0 && tile.view.selected<=tile.displayed.findings.size()?tile.finding(tile.view.selected):"X "+tile.displayed.targetX+" / Z "+tile.displayed.targetZ+"   |   "+count+" findings in view";
		java.util.List lines=fontRendererObj.listFormattedStringToWidth(detail,264);
		for(int i=0;i<Math.min(2,lines.size());i++) text((String)lines.get(i),18,202+i*10,0xB9CAC5);
		if(lines.size()>2 && mouseX>=guiLeft+12 && mouseX<guiLeft+288 && mouseY>=guiTop+198 && mouseY<guiTop+224)
			drawHoveringText(lines,mouseX,mouseY,fontRendererObj);
	}
	private void text(String value,int x,int y,int color) { fontRendererObj.drawString(value,guiLeft+x,guiTop+y,color); }
	private void center(String value,int x,int y) { text(value,x-fontRendererObj.getStringWidth(value)/2,y,0xE4E5DB); }
	@Override protected void drawGuiContainerForegroundLayer(int x,int y) { }

	private static class PanelButton extends GuiButton {
		boolean selected;
		PanelButton(int id,int x,int y,int width,String label) { super(id,x,y,width,18,label); }
		@Override public void drawButton(net.minecraft.client.Minecraft mc,int mouseX,int mouseY) {
			if(!visible) return;
			field_146123_n=mouseX>=xPosition && mouseY>=yPosition && mouseX<xPosition+width && mouseY<yPosition+height;
			int fill=!enabled?0xFF343B3C:field_146123_n?0xFF5B6E6C:selected?0xFF455E5B:0xFF444D4E;
			drawRect(xPosition,yPosition,xPosition+width,yPosition+height,0xFF171D1E);
			drawRect(xPosition+1,yPosition+1,xPosition+width-1,yPosition+height-1,fill);
			drawRect(xPosition+1,yPosition+1,xPosition+width-1,yPosition+2,selected?0xFF91D1C5:0xFF687371);
			mc.fontRenderer.drawString(displayString,xPosition+(width-mc.fontRenderer.getStringWidth(displayString))/2,yPosition+5,!enabled?0x717C79:0xE4E5DB);
		}
	}
}
