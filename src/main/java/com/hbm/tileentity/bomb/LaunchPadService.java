package com.hbm.tileentity.bomb;

import java.util.HashMap;
import java.util.Map;

import com.hbm.handler.MissileStruct;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.tileentity.machine.storage.TileEntityPropellantTank;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

/** Local, gated Forge access. Servicing never bypasses launch requirements or discards fuel. */
public class LaunchPadService implements IFluidHandler {
	public static final String OFF = "off", HOLD = "hold", DRAIN = "drain", FILL = "fill";
	private final TileEntity owner;
	private final FluidTank[] tanks;
	private String mode = OFF;
	private Requirements required = new Requirements();

	public LaunchPadService(TileEntity owner, FluidTank[] tanks) { this.owner = owner; this.tanks = tanks; }
	public String mode() { return mode; }
	public boolean active() { return !OFF.equals(mode); }
	public boolean draining() { return DRAIN.equals(mode); }
	public boolean filling() { return FILL.equals(mode); }
	public boolean empty() { return tanks[0].getFill() == 0 && tanks[1].getFill() == 0; }
	/** Validate a physical transposer core connection, rather than trusting an empty inventory match. */
	@cpw.mods.fml.common.Optional.Method(modid = "OpenComputers")
	public static boolean verifyTransposer(TileEntity pad, li.cil.oc.api.machine.Context context, String address, int side) {
		if(side < 0 || side > 5 || context.node().network() == null || pad.getWorldObj() == null || pad.isInvalid()
				|| !pad.getWorldObj().blockExists(pad.xCoord, pad.yCoord, pad.zCoord)
				|| pad.getWorldObj().getTileEntity(pad.xCoord, pad.yCoord, pad.zCoord) != pad) return false;
		li.cil.oc.api.network.Node node = context.node().network().node(address);
		if(node == null || !node.canBeReachedFrom(context.node())) return false;
		Object environment = node.host();
		// Only stationary block transposers have a stable, world-relative side mapping.
		if(!environment.getClass().getName().equals("li.cil.oc.server.component.Transposer$Block")) return false;
		try {
			Object host = environment.getClass().getMethod("host").invoke(environment);
			if(!(host instanceof TileEntity)) return false;
			TileEntity transposer = (TileEntity) host;
			ForgeDirection dir = ForgeDirection.getOrientation(side);
			return !transposer.isInvalid() && transposer.getWorldObj() == pad.getWorldObj()
					&& pad.getWorldObj().blockExists(transposer.xCoord, transposer.yCoord, transposer.zCoord)
					&& pad.getWorldObj().getTileEntity(transposer.xCoord, transposer.yCoord, transposer.zCoord) == transposer && transposer.xCoord + dir.offsetX == pad.xCoord
					&& transposer.yCoord + dir.offsetY == pad.yCoord && transposer.zCoord + dir.offsetZ == pad.zCoord;
		} catch(ReflectiveOperationException ex) { return false; }
	}
	@cpw.mods.fml.common.Optional.Method(modid = "OpenComputers")
	public static boolean verifyMEInterface(TileEntity pad, li.cil.oc.api.machine.Context context, String address, int padSide, int supplySide, String meAddress) {
		if(supplySide < 0 || supplySide > 5 || supplySide == padSide || !verifyTransposer(pad, context, address, padSide)) return false;
		li.cil.oc.api.network.Node node = context.node().network().node(meAddress);
		if(node == null || !node.canBeReachedFrom(context.node())) return false;
		Object environment = node.host();
		if(!environment.getClass().getName().equals("li.cil.oc.integration.appeng.DriverBlockInterface$Environment")) return false;
		try {
			Object host = environment.getClass().getMethod("tile").invoke(environment);
			if(!(host instanceof TileEntity)) return false;
			TileEntity me = (TileEntity) host;
			ForgeDirection fromPad = ForgeDirection.getOrientation(padSide), supply = ForgeDirection.getOrientation(supplySide);
			return !me.isInvalid() && me.getWorldObj() == pad.getWorldObj() && pad.getWorldObj().blockExists(me.xCoord, me.yCoord, me.zCoord)
					&& pad.getWorldObj().getTileEntity(me.xCoord, me.yCoord, me.zCoord) == me && me.xCoord == pad.xCoord - fromPad.offsetX + supply.offsetX
					&& me.yCoord == pad.yCoord - fromPad.offsetY + supply.offsetY && me.zCoord == pad.zCoord - fromPad.offsetZ + supply.offsetZ;
		} catch(ReflectiveOperationException ex) { return false; }
	}
	public void setMode(String next) {
		if(!OFF.equals(next) && !HOLD.equals(next) && !DRAIN.equals(next) && !FILL.equals(next)) throw new IllegalArgumentException("Expected off, hold, drain or fill");
		mode = next;
		owner.markDirty();
	}
	public void read(NBTTagCompound nbt) {
		// A saved transfer phase is never resumed by loading a chunk.
		mode = nbt.hasKey("siloService") && !OFF.equals(nbt.getString("siloService")) ? HOLD : OFF;
	}
	public void write(NBTTagCompound nbt) { nbt.setString("siloService", mode); }

	public static class Requirements {
		public FluidType fuel = Fluids.NONE, oxidizer = Fluids.NONE;
		public int amount, solid;
		public boolean valid;
	}
	public static Requirements requirements(ItemStack stack) {
		Requirements r = new Requirements();
		if(stack == null) return r;
		if(stack.getItem() instanceof ItemMissile) {
			ItemMissile missile = (ItemMissile) stack.getItem();
			r.valid = missile.launchable;
			r.amount = Math.max(0, missile.fuelCap);
			switch(missile.fuel) {
				case ETHANOL_PEROXIDE: r.fuel = Fluids.ETHANOL; r.oxidizer = Fluids.PEROXIDE; break;
				case KEROSENE_PEROXIDE: r.fuel = Fluids.KEROSENE; r.oxidizer = Fluids.PEROXIDE; break;
				case KEROSENE_LOXY: r.fuel = Fluids.KEROSENE; r.oxidizer = Fluids.OXYGEN; break;
				case JETFUEL_LOXY: r.fuel = Fluids.KEROSENE_REFORM; r.oxidizer = Fluids.OXYGEN; break;
				case SOLID: r.amount = 0; break;
			}
		} else if(stack.getItem() instanceof ItemCustomMissile) {
			try {
				MissileStruct parts = ItemCustomMissile.getStruct(stack);
				if(parts == null || !(parts.fuselage instanceof ItemCustomMissilePart)) return r;
				ItemCustomMissilePart fuselage = (ItemCustomMissilePart) parts.fuselage;
				float amount = (Float) fuselage.attributes[1];
				if(Float.isNaN(amount) || Float.isInfinite(amount) || amount < 0 || amount > 100000) return r;
				r.amount = (int) Math.ceil(amount);
				switch((ItemCustomMissilePart.FuelType) fuselage.attributes[0]) {
					case KEROSENE: r.fuel = Fluids.KEROSENE; r.oxidizer = Fluids.PEROXIDE; break;
					case HYDROGEN: r.fuel = Fluids.HYDROGEN; r.oxidizer = Fluids.OXYGEN; break;
					case XENON: r.fuel = Fluids.XENON; break;
					case BALEFIRE: r.fuel = Fluids.BALEFIRE; r.oxidizer = Fluids.PEROXIDE; break;
					case SOLID: r.solid = r.amount; r.amount = 0; break;
					default: return r;
				}
				r.valid = true;
			} catch(RuntimeException ex) { r.valid = false; }
		}
		return r;
	}
	public void refreshRequirements(ItemStack stack) { required = requirements(stack); }
	public void configure(ItemStack stack) { configure(requirements(stack)); }
	public void configure(Requirements r) {
		required = r;
		if(!r.valid) return;
		select(tanks[0], r.fuel); select(tanks[1], r.oxidizer);
	}
	private void select(FluidTank tank, FluidType type) {
		// A manual payload change also retains the previous contents until recovered.
		if(tank.getFill() == 0 && tank.getTankType() != type) { tank.setTankType(type); owner.markDirty(); }
	}
	public boolean fueled(int solid) {
		return required.valid && solid >= required.solid && enough(0, required.fuel) && enough(1, required.oxidizer);
	}
	private boolean enough(int index, FluidType type) {
		return type == Fluids.NONE || (tanks[index].getTankType() == type && tanks[index].getFill() >= required.amount);
	}
	public Map<String, Object> info(boolean valid, boolean ready, int solid) {
		Map<String, Object> result = new HashMap<>();
		result.put("version", 1); result.put("mode", mode);
		result.put("valid", valid && required.valid); result.put("ready", ready);
		result.put("fuel", name(required.fuel)); result.put("oxidizer", name(required.oxidizer));
		result.put("fuelRequired", required.fuel == Fluids.NONE ? 0 : required.amount);
		result.put("oxidizerRequired", required.oxidizer == Fluids.NONE ? 0 : required.amount);
		result.put("fuelStored", tanks[0].getFill()); result.put("oxidizerStored", tanks[1].getFill());
		result.put("fuelType", name(tanks[0].getTankType())); result.put("oxidizerType", name(tanks[1].getTankType()));
		result.put("solid", solid); result.put("solidRequired", required.solid);
		return result;
	}
	private static String name(FluidType type) {
		Fluid fluid = TileEntityPropellantTank.forgeFluid(type);
		return fluid == null ? "" : fluid.getName();
	}
	@Override public int fill(ForgeDirection side, FluidStack resource, boolean execute) {
		if(!filling() || !required.valid || resource == null || resource.amount <= 0) return 0;
		FluidType type = TileEntityPropellantTank.hbmFluid(resource.getFluid());
		for(int i = 0; i < 2; i++) {
			FluidType expected = i == 0 ? required.fuel : required.oxidizer;
			if(type != Fluids.NONE && type == expected && (tanks[i].getTankType() == type || tanks[i].getFill() == 0)) {
				int amount = Math.min(resource.amount, Math.max(0, Math.min(tanks[i].getMaxFill(), required.amount) - tanks[i].getFill()));
				if(execute && amount > 0) { tanks[i].setTankType(type); tanks[i].setFill(tanks[i].getFill() + amount); owner.markDirty(); }
				return amount;
			}
		}
		return 0;
	}
	@Override public FluidStack drain(ForgeDirection side, FluidStack resource, boolean execute) {
		if(!draining() || resource == null || resource.amount <= 0) return null;
		for(FluidTank tank : tanks) {
			if(resource.getFluid() == TileEntityPropellantTank.forgeFluid(tank.getTankType()) && tank.getFill() > 0) {
				int amount = Math.min(resource.amount, tank.getFill());
				if(execute) { tank.setFill(tank.getFill() - amount); owner.markDirty(); }
				return new FluidStack(resource.getFluid(), amount);
			}
		}
		return null;
	}
	@Override public FluidStack drain(ForgeDirection side, int amount, boolean execute) {
		if(amount <= 0) return null;
		for(FluidTank tank : tanks) {
			Fluid fluid = TileEntityPropellantTank.forgeFluid(tank.getTankType());
			if(tank.getFill() > 0 && fluid != null) return drain(side, new FluidStack(fluid, amount), execute);
		}
		return null;
	}
	@Override public boolean canFill(ForgeDirection side, Fluid fluid) { return fluid != null && fill(side, new FluidStack(fluid, 1), false) > 0; }
	@Override public boolean canDrain(ForgeDirection side, Fluid fluid) { return fluid != null && drain(side, new FluidStack(fluid, 1), false) != null; }
	@Override public FluidTankInfo[] getTankInfo(ForgeDirection side) {
		FluidTankInfo[] info = new FluidTankInfo[2];
		for(int i = 0; i < 2; i++) {
			Fluid fluid = TileEntityPropellantTank.forgeFluid(tanks[i].getTankType());
			info[i] = new FluidTankInfo(fluid == null ? null : new FluidStack(fluid, tanks[i].getFill()), tanks[i].getMaxFill());
		}
		return info;
	}
}
