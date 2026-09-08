package com.hbm.tileentity.bomb;

import com.hbm.blocks.BlockDummyable;
import cpw.mods.fml.common.Optional;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

public final class LaunchPadInventoryMapping {
	private LaunchPadInventoryMapping() { }
	@Optional.Method(modid = "OpenComputers")
	public static Object[] find(TileEntity pad, Context context, String address) {
		Node computer = context.node();
		Node controller = computer.network() == null ? null : computer.network().node(address);
		if(controller == null || !context.canInteract(address)) return new Object[] {null, "CONTROLLER_UNAVAILABLE"};
		Object environment = controller.host();
		// Only stationary adapter inventory controllers use these absolute world sides.
		if(environment == null || !environment.getClass().getName().equals("li.cil.oc.server.component.UpgradeInventoryController$Adapter"))
			return new Object[] {null, "UNSUPPORTED_CONTROLLER"};
		try {
			EnvironmentHost host = (EnvironmentHost) environment.getClass().getMethod("host").invoke(environment);
			if(host.world() != pad.getWorldObj()) return new Object[] {null, "DIFFERENT_WORLD"};
			int x = MathHelper.floor_double(host.xPosition()), y = MathHelper.floor_double(host.yPosition()), z = MathHelper.floor_double(host.zPosition());
			Integer found = null;
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
				int px=x+side.offsetX, py=y+side.offsetY, pz=z+side.offsetZ;
				if(!host.world().blockExists(px,py,pz)) continue;
				TileEntity adjacent = host.world().getTileEntity(px,py,pz);
				boolean match = adjacent == pad;
				Block block = host.world().getBlock(px,py,pz);
				if(!match && adjacent instanceof net.minecraft.inventory.IInventory && block instanceof BlockDummyable) {
					int[] core=((BlockDummyable)block).findCore(host.world(),px,py,pz);
					match=core!=null && core[0]==pad.xCoord && core[1]==pad.yCoord && core[2]==pad.zCoord;
				}
				if(match) {
					if(found != null) return new Object[] {null, "AMBIGUOUS_SIDES"};
					found = side.ordinal();
				}
			}
			return new Object[] {found, found == null ? "NOT_ADJACENT" : "MATCH"};
		} catch(ReflectiveOperationException unavailable) {
			return new Object[] {null, "UNSUPPORTED_CONTROLLER"};
		}
	}
}
