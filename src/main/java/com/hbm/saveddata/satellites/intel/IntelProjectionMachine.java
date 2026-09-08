package com.hbm.saveddata.satellites.intel;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.tileentity.TileEntityDoorGeneric;
import com.hbm.tileentity.bomb.*;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

/** Visual fields only: no machine inventory, control state, targets or executable tile NBT. */
public final class IntelProjectionMachine {
	public final int x,y,z,meta,kind;
	public final NBTTagCompound visual;
	public IntelProjectionMachine(int x,int y,int z,int meta,int kind,NBTTagCompound visual) {
		this.x=x;this.y=y;this.z=z;this.meta=meta;this.kind=kind;this.visual=visual;
	}
	public static int kind(Block b) {
		if(b==null) return 0;
		return b==ModBlocks.silo_hatch?1:b==ModBlocks.silo_hatch_large?2:b==ModBlocks.launch_pad?3:
			b==ModBlocks.launch_pad_large?4:b==ModBlocks.launch_table?5:0;
	}
	private static String name(Item item) {
		Object name=item==null?null:Item.itemRegistry.getNameForObject(item);return name==null?"":name.toString();
	}
	public static IntelProjectionMachine capture(int x,int y,int z,int meta,int kind,TileEntity tile) {
		NBTTagCompound n=new NBTTagCompound();
		if((kind==1 || kind==2) && tile instanceof TileEntityDoorGeneric) {
			n.setInteger("open",Math.max(0,Math.min(60,((TileEntityDoorGeneric)tile).openTicks)));
		} else if((kind==3 || kind==4) && tile instanceof TileEntityLaunchPadBase) {
			ItemStack stack=((TileEntityLaunchPadBase)tile).getStackInSlot(0);
			if(stack!=null) { n.setString("missile",name(stack.getItem()));n.setInteger("damage",stack.getItemDamage()); }
			if(kind==4 && tile instanceof TileEntityLaunchPadLarge) {
				TileEntityLaunchPadLarge pad=(TileEntityLaunchPadLarge)tile;
				n.setInteger("form",pad.formFactor);n.setBoolean("erected",pad.erected);n.setBoolean("ready",pad.readyToLoad);
				n.setFloat("lift",pad.lift);n.setFloat("angle",pad.erector);
			}
		} else if(kind==5 && tile instanceof TileEntityLaunchTable) {
			TileEntityLaunchTable pad=(TileEntityLaunchTable)tile;
			n.setInteger("size",pad.padSize.ordinal());n.setInteger("height",pad.height);
			ItemStack stack=pad.getStackInSlot(0);
			if(stack!=null && stack.getItem() instanceof ItemCustomMissile) {
				// Registry names survive numeric item-ID changes between saves.
				for(String key:new String[]{"warhead","fuselage","stability","thruster"}) {
					Item item=stack.hasTagCompound()?Item.getItemById(stack.getTagCompound().getInteger(key)):null;
					if(item instanceof ItemCustomMissilePart) n.setString(key,name(item));
				}
			}
		} else return null;
		return new IntelProjectionMachine(x,y,z,meta,kind,n);
	}
	public ItemStack missile() {
		Item item=(Item)Item.itemRegistry.getObject(visual.getString("missile"));
		return item==null?null:new ItemStack(item,1,visual.getInteger("damage"));
	}
	public MissileStruct multipart() {
		MissileStruct s=new MissileStruct(part("warhead"),part("fuselage"),part("stability"),part("thruster"));
		return s.warhead==null || s.fuselage==null || s.thruster==null?null:s;
	}
	private Item part(String key) {
		Item item=(Item)Item.itemRegistry.getObject(visual.getString(key));
		if(!(item instanceof ItemCustomMissilePart)) return null;
		ItemCustomMissilePart.PartType expected="warhead".equals(key)?ItemCustomMissilePart.PartType.WARHEAD:
			"fuselage".equals(key)?ItemCustomMissilePart.PartType.FUSELAGE:"stability".equals(key)?ItemCustomMissilePart.PartType.FINS:ItemCustomMissilePart.PartType.THRUSTER;
		return ((ItemCustomMissilePart)item).type==expected?item:null;
	}
	public NBTTagCompound write() {
		NBTTagCompound n=(NBTTagCompound)visual.copy();
		n.setInteger("x",x);n.setInteger("y",y);n.setInteger("z",z);n.setInteger("meta",meta);n.setInteger("kind",kind);return n;
	}
	public static IntelProjectionMachine read(NBTTagCompound n,int width,int depth) {
		int x=n.getInteger("x"),y=n.getInteger("y"),z=n.getInteger("z"),meta=n.getInteger("meta"),kind=n.getInteger("kind");
		if(x<0 || x>=width || z<0 || z>=depth || y<0 || y>255 || meta<0 || meta>15 || kind<1 || kind>5) return null;
		NBTTagCompound v=new NBTTagCompound();
		for(String key:new String[]{"missile","warhead","fuselage","stability","thruster"}) {
			String value=n.getString(key);if(value.length()>256) return null;v.setString(key,value);
		}
		for(String key:new String[]{"open","damage","form","size","height"}) v.setInteger(key,n.getInteger(key));
		if(v.getInteger("open")<0 || v.getInteger("open")>60 || v.getInteger("height")<0 || v.getInteger("height")>256
			|| v.getInteger("size")<0 || v.getInteger("size")>=ItemCustomMissilePart.PartSize.values().length) return null;
		for(String key:new String[]{"lift","angle"}) {
			float f=n.getFloat(key);if(!Float.isFinite(f) || Math.abs(f)>256) return null;v.setFloat(key,f);
		}
		v.setBoolean("erected",n.getBoolean("erected"));v.setBoolean("ready",n.getBoolean("ready"));
		return new IntelProjectionMachine(x,y,z,meta,kind,v);
	}
}
