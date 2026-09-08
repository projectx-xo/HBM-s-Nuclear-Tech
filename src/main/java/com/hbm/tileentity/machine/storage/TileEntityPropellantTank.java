package com.hbm.tileentity.machine.storage;

import com.hbm.blocks.machine.BlockPropellantTank;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.items.machine.IItemFluidIdentifier;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;

public class TileEntityPropellantTank extends TileEntityBarrel implements IFluidHandler {
	public static final int[] CAPACITIES = {64_000, 256_000, 1_024_000};
	public static final String[] FUEL_NAMES = {"peroxide", "ethanol", "kerosene", "oxygen", "kerosene_reform", "hydrogen", "xenon", "balefire"};
	private int tier;
	private int creativeFuel = -1;

	public static FluidType fuel(int index) {
		switch(index) {
			case 0: return Fluids.PEROXIDE;
			case 1: return Fluids.ETHANOL;
			case 2: return Fluids.KEROSENE;
			case 3: return Fluids.OXYGEN;
			case 4: return Fluids.KEROSENE_REFORM;
			case 5: return Fluids.HYDROGEN;
			case 6: return Fluids.XENON;
			case 7: return Fluids.BALEFIRE;
			default: return Fluids.NONE;
		}
	}
	public static int fuelIndex(FluidType type) {
		for(int i = 0; i < FUEL_NAMES.length; i++) if(type != null && type == fuel(i)) return i;
		return -1;
	}
	public static void registerForgeFluids() {
		for(int i = 0; i < FUEL_NAMES.length; i++) {
			String name = "hbm_propellant_" + FUEL_NAMES[i];
			if(!FluidRegistry.isFluidRegistered(name)) FluidRegistry.registerFluid(new Fluid(name));
		}
	}
	public static Fluid forgeFluid(FluidType type) {
		int index = fuelIndex(type);
		return index < 0 ? null : FluidRegistry.getFluid("hbm_propellant_" + FUEL_NAMES[index]);
	}
	public static FluidType hbmFluid(Fluid fluid) {
		for(int i = 0; i < FUEL_NAMES.length; i++) if(fluid != null && fluid == forgeFluid(fuel(i))) return fuel(i);
		return Fluids.NONE;
	}

	public TileEntityPropellantTank() { this(0, -1); }
	public TileEntityPropellantTank(int tier, int creativeFuel) {
		super();
		this.tier = Math.max(0, Math.min(2, tier));
		this.creativeFuel = creativeFuel >= 0 && creativeFuel < FUEL_NAMES.length ? creativeFuel : -1;
		tank = new PropellantContents();
		mode = (short) (isCreative() ? 2 : 0);
	}
	public boolean isCreative() { return creativeFuel >= 0; }
	@Override public String getName() { return "container.propellantTank"; }
	@Override public void checkFluidInteraction() { /* Only compatible missile propellants are accepted. */ }

	@Override public void updateEntity() {
		if(getBlockType() instanceof BlockPropellantTank) {
			BlockPropellantTank block = (BlockPropellantTank) getBlockType();
			tier = block.tier;
			creativeFuel = block.creativeFuel;
		}
		if(isCreative()) mode = 2;
		boolean hadInput = slots[0] != null || slots[2] != null || slots[4] != null;
		super.updateEntity();
		if(!worldObj.isRemote && hadInput) markDirty();
	}
	@Override public long getDemand(FluidType type, int pressure) {
		return isCreative() || fuelIndex(type) < 0 ? 0 : super.getDemand(type, pressure);
	}
	@Override public long transferFluid(FluidType type, int pressure, long amount) {
		if(amount <= 0) return amount;
		int accepted = (int) Math.min(amount, Math.max(0, getDemand(type, pressure)));
		if(accepted > 0) { tank.setFill(tank.getFill() + accepted); markDirty(); }
		return amount - accepted;
	}
	@Override public void useUpFluid(FluidType type, int pressure, long amount) {
		if(amount > 0 && type == tank.getTankType() && pressure == 0 && !isCreative()) {
			tank.setFill(tank.getFill() - (int) Math.min(amount, tank.getFill()));
			markDirty();
		}
	}
	@Override public long getProviderSpeed(FluidType type, int pressure) { return 16_000; }
	@Override public long getReceiverSpeed(FluidType type, int pressure) { return 16_000; }
	@Override public FluidTank[] getReceivingTanks() { return isCreative() ? FluidTank.EMPTY_ARRAY : super.getReceivingTanks(); }

	@Override public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		if(resource == null || resource.amount <= 0 || isCreative() || (mode != 0 && mode != 1)) return 0;
		FluidType type = hbmFluid(resource.getFluid());
		if(fuelIndex(type) < 0 || (tank.getFill() > 0 && tank.getTankType() != type)) return 0;
		int accepted = Math.min(resource.amount, tank.getMaxFill() - tank.getFill());
		if(doFill && accepted > 0) { tank.setTankType(type); tank.setFill(tank.getFill() + accepted); markDirty(); }
		return accepted;
	}
	@Override public FluidStack drain(ForgeDirection from, int amount, boolean doDrain) {
		if(amount <= 0 || (mode != 1 && mode != 2) || tank.getFill() <= 0) return null;
		Fluid fluid = forgeFluid(tank.getTankType());
		if(fluid == null) return null;
		int drained = Math.min(amount, tank.getFill());
		if(doDrain) useUpFluid(tank.getTankType(), 0, drained);
		return new FluidStack(fluid, drained);
	}
	@Override public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return resource == null || resource.getFluid() != forgeFluid(tank.getTankType()) ? null : drain(from, resource.amount, doDrain);
	}
	@Override public boolean canFill(ForgeDirection from, Fluid fluid) {
		return fluid != null && fill(from, new FluidStack(fluid, 1), false) > 0;
	}
	@Override public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return fluid == forgeFluid(tank.getTankType()) && drain(from, 1, false) != null;
	}
	@Override public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		Fluid fluid = forgeFluid(tank.getTankType());
		return new FluidTankInfo[] {new FluidTankInfo(fluid == null ? null : new FluidStack(fluid, tank.getFill()), tank.getMaxFill())};
	}

	@Override public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		tank.writeToNBT(nbt, "tank");
		nbt.setShort("mode", mode);
		nbt.setInteger("propellantTier", tier);
		nbt.setInteger("creativeFuel", creativeFuel);
	}
	@Override public void readFromNBT(NBTTagCompound nbt) {
		tier = Math.max(0, Math.min(2, nbt.getInteger("propellantTier")));
		creativeFuel = nbt.hasKey("creativeFuel") ? nbt.getInteger("creativeFuel") : -1;
		if(creativeFuel < 0 || creativeFuel >= FUEL_NAMES.length) creativeFuel = -1;
		super.readFromNBT(nbt);
		tank.readFromNBT(nbt, "tank");
		mode = nbt.getShort("mode");
		mode = (short) (isCreative() ? 2 : Math.max(0, Math.min(3, mode)));
	}
	@Override public void writeNBT(NBTTagCompound nbt) {
		NBTTagCompound data = new NBTTagCompound();
		tank.writeToNBT(data, "tank");
		data.setShort("mode", mode);
		nbt.setTag(NBT_PERSISTENT_KEY, data);
	}
	@Override public void readNBT(NBTTagCompound nbt) {
		if(!nbt.hasKey(NBT_PERSISTENT_KEY)) return;
		NBTTagCompound data = nbt.getCompoundTag(NBT_PERSISTENT_KEY);
		tank.readFromNBT(data, "tank");
		mode = (short) (isCreative() ? 2 : Math.max(0, Math.min(3, data.getShort("mode"))));
	}
	@Override public void serialize(ByteBuf buf) {
		buf.writeByte(tier); buf.writeByte(creativeFuel);
		super.serialize(buf);
	}
	@Override public void deserialize(ByteBuf buf) {
		tier = Math.max(0, Math.min(2, buf.readByte()));
		creativeFuel = buf.readByte();
		if(creativeFuel < 0 || creativeFuel >= FUEL_NAMES.length) creativeFuel = -1;
		super.deserialize(buf);
	}

	private class PropellantContents extends FluidTank {
		PropellantContents() { super(Fluids.PEROXIDE, CAPACITIES[tier]); normalize(); }
		private void normalize() {
			maxFluid = CAPACITIES[tier]; pressure = 0;
			if(isCreative()) { type = fuel(creativeFuel); fluid = maxFluid; }
			else { if(fuelIndex(type) < 0) { type = Fluids.PEROXIDE; fluid = 0; } fluid = Math.max(0, Math.min(maxFluid, fluid)); }
		}
		@Override public int getFill() { normalize(); return fluid; }
		@Override public int getMaxFill() { return CAPACITIES[tier]; }
		@Override public FluidType getTankType() { normalize(); return type; }
		@Override public void setFill(int amount) { fluid = amount; normalize(); }
		@Override public void setTankType(FluidType next) {
			if(!isCreative() && getFill() == 0 && fuelIndex(next) >= 0) super.setTankType(next);
		}
		@Override public boolean setType(int in, int out, ItemStack[] slots) {
			if(isCreative() || getFill() > 0 || slots[in] == null || !(slots[in].getItem() instanceof IItemFluidIdentifier)) return false;
			FluidType next = ((IItemFluidIdentifier) slots[in].getItem()).getType(null, 0, 0, 0, slots[in]);
			return fuelIndex(next) >= 0 && super.setType(in, out, slots);
		}
		@Override public boolean loadTank(int in, int out, ItemStack[] slots) { return !isCreative() && super.loadTank(in, out, slots); }
		@Override public void readFromNBT(NBTTagCompound nbt, String key) { super.readFromNBT(nbt, key); normalize(); }
		@Override public void writeToNBT(NBTTagCompound nbt, String key) { normalize(); super.writeToNBT(nbt, key); nbt.setString(key + "_type", type.getName()); }
		@Override public void serialize(ByteBuf buf) { normalize(); super.serialize(buf); }
		@Override public void deserialize(ByteBuf buf) { super.deserialize(buf); normalize(); }
	}
}
