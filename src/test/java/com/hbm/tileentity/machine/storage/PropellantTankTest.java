package com.hbm.tileentity.machine.storage;

import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.util.EnumSymbol;
import net.minecraft.nbt.NBTTagCompound;

public class PropellantTankTest {
	@BeforeClass public static void fluids() {
		// Minimal HBM registry; Forge transfers are verified in a running game.
		Fluids.NONE = new FluidType("NONE", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.PEROXIDE = new FluidType("PEROXIDE", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.ETHANOL = new FluidType("ETHANOL", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.KEROSENE = new FluidType("KEROSENE", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.OXYGEN = new FluidType("OXYGEN", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.KEROSENE_REFORM = new FluidType("KEROSENE_REFORM", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.HYDROGEN = new FluidType("HYDROGEN", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.XENON = new FluidType("XENON", 0, 0, 0, 0, EnumSymbol.NONE);
		Fluids.BALEFIRE = new FluidType("BALEFIRE", 0, 0, 0, 0, EnumSymbol.NONE);
	}
	@Test public void survivalCapacityAndHbmExtractionConserveFluid() {
		TileEntityPropellantTank tank = new TileEntityPropellantTank(0, -1);
		assertEquals(64000, tank.getDemand(Fluids.PEROXIDE, 0));
		assertEquals(36000, tank.transferFluid(Fluids.PEROXIDE, 0, 100000));
		assertEquals(64000, tank.tank.getFill());
		assertEquals(100L, tank.transferFluid(Fluids.PEROXIDE, 0, 100));
		tank.mode = 2;
		assertEquals(64000, tank.getFluidAvailable(Fluids.PEROXIDE, 0));
		tank.useUpFluid(Fluids.PEROXIDE, 0, 16000);
		assertEquals(48000, tank.tank.getFill());
		tank.useUpFluid(Fluids.PEROXIDE, 0, Long.MAX_VALUE);
		assertEquals(0, tank.tank.getFill());
	}
	@Test public void creativeSupplyNeverDepletes() {
		for(int fuel = 0; fuel < TileEntityPropellantTank.FUEL_NAMES.length; fuel++) {
			TileEntityPropellantTank tank = new TileEntityPropellantTank(2, fuel);
			for(int i = 0; i < 5; i++) {
				assertEquals(1024000, tank.getFluidAvailable(TileEntityPropellantTank.fuel(fuel), 0));
				tank.useUpFluid(TileEntityPropellantTank.fuel(fuel), 0, Long.MAX_VALUE);
				assertEquals(1024000, tank.tank.getFill());
			}
			assertEquals(0, tank.getDemand(TileEntityPropellantTank.fuel(fuel), 0));
			assertEquals(100, tank.transferFluid(TileEntityPropellantTank.fuel(fuel), 0, 100));
			tank.tank.setTankType(Fluids.ETHANOL);
			assertSame(TileEntityPropellantTank.fuel(fuel), tank.tank.getTankType());
		}
	}
	@Test public void nonemptyTanksRejectMixingTypeChangesAndInvalidInputs() {
		TileEntityPropellantTank tank = new TileEntityPropellantTank();
		tank.transferFluid(Fluids.PEROXIDE, 0, 1000);
		tank.tank.setTankType(Fluids.ETHANOL);
		assertSame(Fluids.PEROXIDE, tank.tank.getTankType());
		assertEquals(100, tank.transferFluid(Fluids.NONE, 0, 100));
		assertEquals(100, tank.transferFluid(Fluids.ETHANOL, 0, 100));
		assertEquals(100, tank.transferFluid(Fluids.PEROXIDE, 1, 100));
		tank.useUpFluid(Fluids.PEROXIDE, 0, -10);
		assertEquals(1000, tank.tank.getFill());
		tank.mode = 3;
		assertEquals(0, tank.getDemand(Fluids.PEROXIDE, 0));
		assertEquals(0, tank.getFluidAvailable(Fluids.PEROXIDE, 0));
	}
	@Test public void worldAndItemPersistenceKeepFluidCapacityAndMode() {
		TileEntityPropellantTank source = new TileEntityPropellantTank(1, -1);
		source.tank.setTankType(Fluids.ETHANOL);
		source.transferFluid(Fluids.ETHANOL, 0, 100000);
		source.mode = 2;
		NBTTagCompound world = new NBTTagCompound();
		source.tank.writeToNBT(world, "tank");
		world.setInteger("propellantTier", 1);
		world.setInteger("creativeFuel", -1);
		world.setShort("mode", source.mode);
		TileEntityPropellantTank restored = new TileEntityPropellantTank();
		restored.readFromNBT(world);
		assertEquals(256000, restored.tank.getMaxFill());
		assertEquals(100000, restored.tank.getFill());
		assertSame(Fluids.ETHANOL, restored.tank.getTankType());
		assertEquals(2, restored.mode);
		NBTTagCompound item = new NBTTagCompound();
		source.writeNBT(item);
		TileEntityPropellantTank placed = new TileEntityPropellantTank(1, -1);
		placed.readNBT(item);
		assertEquals(100000, placed.tank.getFill());
		assertEquals(2, placed.mode);
		assertSame(Fluids.ETHANOL, placed.tank.getTankType());
	}
	@Test public void itemDataCannotGrantCreativeOrEnlargeASurvivalTank() {
		TileEntityPropellantTank source = new TileEntityPropellantTank(2, 0);
		NBTTagCompound item = new NBTTagCompound(); source.writeNBT(item);
		item.setInteger("creativeFuel", 0);
		item.setInteger("propellantTier", 2);
		TileEntityPropellantTank placed = new TileEntityPropellantTank(0, -1);
		placed.readNBT(item);
		assertFalse(placed.isCreative());
		assertEquals(64000, placed.tank.getMaxFill());
		assertEquals(64000, placed.tank.getFill());
		placed.mode = 2;
		placed.useUpFluid(Fluids.PEROXIDE, 0, 64000);
		assertEquals(0, placed.tank.getFill());
	}
}
