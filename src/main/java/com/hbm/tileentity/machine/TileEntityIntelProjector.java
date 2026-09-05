package com.hbm.tileentity.machine;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.gui.GUIIntelProjector;
import com.hbm.packet.PacketDispatcher;
import com.hbm.packet.toclient.BufPacket;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.saveddata.SatelliteSavedData;
import com.hbm.saveddata.satellites.SatelliteBase;
import com.hbm.saveddata.satellites.SatelliteCombinedIntel;
import com.hbm.saveddata.satellites.intel.*;
import com.hbm.tileentity.IBufPacketReceiver;
import com.hbm.tileentity.IGUIProvider;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.SimpleComponent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTSizeTracker;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

@Optional.Interface(iface="li.cil.oc.api.network.SimpleComponent", modid="OpenComputers")
public class TileEntityIntelProjector extends TileEntity implements SimpleComponent, IBufPacketReceiver, IControlReceiver, IGUIProvider {
	public volatile IntelScanResult displayed;
	public final IntelProjectionView view=new IntelProjectionView();
	public volatile String sceneId="";
	public int frequency;
	private static final class EncodedSnapshot {
		final String id;final byte[] bytes;
		EncodedSnapshot(String id,byte[] bytes) { this.id=id;this.bytes=bytes; }
	}
	private volatile EncodedSnapshot encoded;
	private final AtomicReference<IntelScanResult> pending=new AtomicReference<IntelScanResult>();
	private final IntelProjectionTransfer transfer=new IntelProjectionTransfer();
	private int requestTicks, requestedOffset=-1;

	@Override public void updateEntity() {
		if(worldObj.isRemote) {
			acceptPendingSnapshot();
			if(!sceneId.isEmpty() && displayed==null) {
				int offset=transfer.offset(sceneId);
				if(offset!=requestedOffset || requestTicks--<=0) {
					requestedOffset=offset;requestTicks=100;
					NBTTagCompound request=new NBTTagCompound();request.setBoolean("snapshot",true);
					request.setString("sceneId",sceneId);request.setInteger("offset",offset);
					PacketDispatcher.wrapper.sendToServer(new NBTControlPacket(request,xCoord,yCoord,zCoord));
				}
			}
		}
	}
	public void acceptPendingSnapshot() {
		IntelScanResult result=pending.getAndSet(null);
		if(result!=null && result.projection!=null && result.projection.id.equals(sceneId)) displayed=result;
	}
	public NBTTagCompound state() {
		NBTTagCompound n=new NBTTagCompound();view.write(n);n.setString("sceneId",sceneId);n.setInteger("frequency",frequency);return n;
	}
	public void readState(NBTTagCompound n) {
		String next=n.getString("sceneId");
		if(!next.equals(sceneId)) { displayed=null;requestTicks=0;requestedOffset=-1;transfer.reset(); }
		sceneId=next;frequency=n.getInteger("frequency");view.read(n);
	}
	private void changed() { markDirty();if(worldObj!=null) worldObj.markBlockForUpdate(xCoord,yCoord,zCoord); }
	@Override public Packet getDescriptionPacket() { return new S35PacketUpdateTileEntity(xCoord,yCoord,zCoord,0,state()); }
	@Override public void onDataPacket(NetworkManager network,S35PacketUpdateTileEntity packet) { readState(packet.func_148857_g()); }

	private void pack() {
		IntelScanResult scene=displayed;if(scene==null) { encoded=null;return; }
		NBTTagCompound n=new NBTTagCompound();scene.writeToNBT(n);
		try {
			byte[] bytes=CompressedStreamTools.compress(n);
			if(bytes.length>IntelProjectionTransfer.MAX_BYTES) throw new IllegalStateException("Projection packet too large");
			encoded=new EncodedSnapshot(scene.projection.id,bytes);
		} catch(IOException e) { throw new IllegalStateException("Could not encode projection",e); }
	}
	@Override public void serialize(ByteBuf buf) { serializeChunk(buf,0); }
	public void serializeChunk(ByteBuf buf,int offset) {
		EncodedSnapshot snapshot=encoded;IntelProjectionTransfer.write(buf,snapshot.id,snapshot.bytes,offset);
	}
	@Override public void deserialize(ByteBuf buf) {
		String expected=sceneId;byte[] bytes=transfer.accept(buf,expected);if(bytes==null) return;
		try {
			IntelScanResult result=IntelScanResult.readFromNBT(CompressedStreamTools.func_152457_a(bytes,new NBTSizeTracker(16*1024*1024)));
			if(result.projection==null || !result.projection.id.equals(expected)) { transfer.reset();return; }
			pending.set(result);
		}
		catch(IOException e) { transfer.reset();throw new IllegalArgumentException("Invalid compressed projection",e); }
	}
	@Override public void writeToNBT(NBTTagCompound n) {
		super.writeToNBT(n);n.setTag("controls",state());
		if(displayed!=null) { NBTTagCompound scene=new NBTTagCompound();displayed.writeToNBT(scene);n.setTag("display",scene); }
	}
	@Override public void readFromNBT(NBTTagCompound n) {
		super.readFromNBT(n);
		if(n.hasKey("controls")) readState(n.getCompoundTag("controls"));
		if(n.hasKey("display")) {
			displayed=IntelScanResult.readFromNBT(n.getCompoundTag("display"));
			if(displayed.projection==null) displayed=null;
		}
		sceneId=displayed==null?"":displayed.projection.id;pack();
	}
	@Override public boolean hasPermission(EntityPlayer player) {
		return player.worldObj==worldObj && player.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64*64;
	}
	@Override public void receiveControl(NBTTagCompound n) { }
	@Override public void receiveControl(EntityPlayer player,NBTTagCompound n) {
		if(n.getBoolean("snapshot")) {
			final int offset=n.getInteger("offset");final EncodedSnapshot snapshot=encoded;
			if(snapshot!=null && snapshot.id.equals(n.getString("sceneId")) && offset>=0 && offset<snapshot.bytes.length
					&& offset%IntelProjectionTransfer.CHUNK_SIZE==0) {
				PacketDispatcher.wrapper.sendTo(new BufPacket(xCoord,yCoord,zCoord,new IBufPacketReceiver() {
					public void serialize(ByteBuf buf) { IntelProjectionTransfer.write(buf,snapshot.id,snapshot.bytes,offset); }
					public void deserialize(ByteBuf buf) { }
				}),(EntityPlayerMP)player);
			}
		} else if(player.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64) {
			try { control(n.getString("action"),n.getString("value")); }
			catch(IllegalArgumentException e) { player.addChatMessage(new ChatComponentText(e.getMessage())); }
		}
	}
	private void control(String action,String value) {
		if(displayed==null) throw new IllegalArgumentException("Waiting for a completed combined scan from STRATCOM");
		view.configure(action,value,displayed.projection,displayed.findings.size());changed();
	}
	public String status() {
		IntelScanResult scene=displayed;
		if(scene==null) return sceneId.isEmpty()?"Waiting for a completed combined scan":"Receiving scan geometry";
		IntelProjection p=scene.projection;
		if(!p.hasBlockStates) return "Rescan to display block textures";
		return "DISPLAYED | "+view.mode+" | X="+scene.targetX+" Z="+scene.targetZ+" | "+scene.findings.size()
				+" findings | geometry "+p.coveredColumns()+"/"+(p.width*p.depth)+" columns | floor="+view.floor;
	}
	public String finding(int index) {
		IntelScanResult scene=displayed;
		if(scene==null || index<1 || index>scene.findings.size()) throw new IllegalArgumentException("Finding out of range");
		IntelFinding f=scene.findings.get(index-1);
		return "#"+index+" "+f.classification.name()+" | "+f.minX+","+f.minY+","+f.minZ+" to "+f.maxX+","+f.maxY+","+f.maxZ
				+" | "+f.targetType+" | confidence="+Math.round(f.confidence*100)+"%";
	}
	@Override @Optional.Method(modid="OpenComputers") public String getComponentName() { return "ntm_intel_projector"; }
	@Callback(doc="function(frequency:number, dimension:number, snapshot:string):boolean,string -- Display an exact completed combined scan")
	@Optional.Method(modid="OpenComputers")
	public Object[] showScan(Context context,Arguments args) {
		int freq=args.checkInteger(0), dimension=args.checkInteger(1);String id=args.checkString(2);
		World source=DimensionManager.getWorld(dimension);
		if(source==null) return new Object[]{false,"Scan dimension is not loaded"};
		SatelliteBase satellite=SatelliteSavedData.getData(source).getSatFromFreq(freq);
		if(!(satellite instanceof SatelliteCombinedIntel)) return new Object[]{false,"Combined intelligence satellite required"};
		IntelScanResult result=((SatelliteCombinedIntel)satellite).getLastResult();
		if(!IntelProjection.matches(result,dimension,id) || !result.projection.hasBlockStates) return new Object[]{false,"Scan changed or lacks block textures; run a new combined scan"};
		if(sceneId.equals(id) && frequency==freq) return new Object[]{true,status()};
		// Retain only geometry and findings, not the old sparse terrain pages.
		IntelScanResult scene=new IntelScanResult();scene.mode=IntelScanMode.COMBINED;scene.projection=result.projection;
		scene.targetX=result.targetX;scene.targetZ=result.targetZ;scene.dimension=result.dimension;scene.completedAt=result.completedAt;
		scene.findings.addAll(result.findings);displayed=scene;sceneId=id;frequency=freq;
		view.selected=0;view.configure("view","exterior",scene.projection,scene.findings.size());pack();changed();
		return new Object[]{true,status()};
	}
	@Callback(doc="function(action:string,value:string):boolean,string -- view, floor, cut, select, rotate, scale, terrain")
	@Optional.Method(modid="OpenComputers")
	public Object[] configure(Context context,Arguments args) {
		try { control(args.checkString(0),args.checkString(1));return new Object[]{true,status()}; }
		catch(IllegalArgumentException e) { return new Object[]{false,e.getMessage()}; }
	}
	@Callback(direct=true,doc="function():string,number -- Display status and finding count") @Optional.Method(modid="OpenComputers")
	public Object[] getStatus(Context context,Arguments args) { IntelScanResult scene=displayed;return new Object[]{status(),scene==null?0:scene.findings.size()}; }
	@Callback(direct=true,doc="function(index:number):string -- Finding number, type and original coordinates") @Optional.Method(modid="OpenComputers")
	public Object[] getFinding(Context context,Arguments args) { return new Object[]{finding(args.checkInteger(0))}; }
	@Callback(doc="function():boolean -- Clear the projection") @Optional.Method(modid="OpenComputers")
	public Object[] clear(Context context,Arguments args) { displayed=null;sceneId="";encoded=null;changed();return new Object[]{true}; }

	@Override public Container provideContainer(int id,EntityPlayer player,World world,int x,int y,int z) {
		return new Container() { @Override public boolean canInteractWith(EntityPlayer p) { return !isInvalid() && p.getDistanceSq(xCoord+.5,yCoord+.5,zCoord+.5)<=64; } };
	}
	@Override @SideOnly(Side.CLIENT) public Object provideGUI(int id,EntityPlayer player,World world,int x,int y,int z) {
		return new GUIIntelProjector(player,this);
	}
	@Override @SideOnly(Side.CLIENT) public AxisAlignedBB getRenderBoundingBox() {
		return AxisAlignedBB.getBoundingBox(xCoord-13,yCoord,zCoord-13,xCoord+14,yCoord+15,zCoord+14);
	}
	@Override @SideOnly(Side.CLIENT) public double getMaxRenderDistanceSquared() { return 64*64; }
	@Override @SideOnly(Side.CLIENT) public boolean shouldRenderInPass(int pass) { return pass==1; }
}
