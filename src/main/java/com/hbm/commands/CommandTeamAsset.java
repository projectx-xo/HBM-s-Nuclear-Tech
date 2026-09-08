package com.hbm.commands;

import com.hbm.blocks.BlockDummyable;
import com.hbm.compat.teams.TeamAssetIdentity;
import com.hbm.tileentity.bomb.TileEntityLaunchPadBase;
import com.hbm.tileentity.bomb.TileEntityLaunchTable;
import com.hbm.tileentity.machine.TileEntityMachineRadarNT;
import com.hbm.tileentity.machine.TileEntityMachineSatLink;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

public class CommandTeamAsset extends CommandBase {
	@Override public String getCommandName() { return "hbmasset"; }
	@Override public String getCommandUsage(ICommandSender sender) { return "/hbmasset <x> <y> <z> <label>"; }
	@Override public int getRequiredPermissionLevel() { return 0; }
	@Override public void processCommand(ICommandSender sender, String[] args) {
		EntityPlayerMP player = getCommandSenderAsPlayer(sender);
		if(args.length != 4) { sender.addChatMessage(new ChatComponentText(getCommandUsage(sender))); return; }
		int x=parseInt(sender,args[0]), y=parseInt(sender,args[1]), z=parseInt(sender,args[2]);
		String team=TeamAssetIdentity.teamFor(player.getCommandSenderName());
		String label=TeamAssetIdentity.validText(args[3],32);
		if(team.isEmpty() || label.isEmpty()) { sender.addChatMessage(new ChatComponentText("Join a BaseCenter team first; label must be 1-32 printable characters.")); return; }
		if(player.getDistanceSq(x+.5,y+.5,z+.5)>256 || !player.worldObj.blockExists(x,y,z)) {
			sender.addChatMessage(new ChatComponentText("Stand within 16 blocks of the loaded asset.")); return;
		}
		if(player.worldObj.getBlock(x,y,z) instanceof BlockDummyable) {
			int[] core=((BlockDummyable)player.worldObj.getBlock(x,y,z)).findCore(player.worldObj,x,y,z);
			if(core!=null) { x=core[0]; y=core[1]; z=core[2]; }
		}
		if(!player.worldObj.blockExists(x,y,z)) return;
		TileEntity tile=player.worldObj.getTileEntity(x,y,z);
		if(!(tile instanceof TileEntityLaunchPadBase) && !(tile instanceof TileEntityLaunchTable)
			&& !(tile instanceof TileEntityMachineRadarNT) && !(tile instanceof TileEntityMachineSatLink)) {
			sender.addChatMessage(new ChatComponentText("Select an HBM launch pad, radar or satellite ground station.")); return;
		}
		NBTTagCompound previous=((com.hbm.compat.teams.TeamAssetHost)tile).getTeamAssetData().getCompoundTag(TeamAssetIdentity.KEY);
		String owner=previous.getString("ownerUUID");
		if(!owner.isEmpty() && !owner.equals(player.getUniqueID().toString()) && !player.canCommandSenderUseCommand(2,getCommandName())) {
			sender.addChatMessage(new ChatComponentText("Only the registering owner or an operator can relabel this asset.")); return;
		}
		NBTTagCompound data=new NBTTagCompound();
		data.setString("ownerUUID",player.getUniqueID().toString());
		data.setString("ownerName",player.getCommandSenderName());data.setString("label",label);
		((com.hbm.compat.teams.TeamAssetHost)tile).getTeamAssetData().setTag(TeamAssetIdentity.KEY,data);tile.markDirty();
		sender.addChatMessage(new ChatComponentText(label+" registered to "+team+". This does not protect it from damage."));
	}
}
