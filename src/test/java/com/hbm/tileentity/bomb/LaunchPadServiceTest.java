package com.hbm.tileentity.bomb;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTank;
import com.hbm.tileentity.machine.storage.PropellantTankTest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

public class LaunchPadServiceTest {
	private FluidTank[] tanks;
	private LaunchPadService service;
	@BeforeClass public static void fluids() { PropellantTankTest.fluids(); }
	@Before public void create() {
		tanks = new FluidTank[] {new FluidTank(Fluids.NONE, 24000), new FluidTank(Fluids.NONE, 24000)};
		service = new LaunchPadService(new TileEntity() { @Override public void markDirty() {} }, tanks);
	}
	private LaunchPadService.Requirements requirements() {
		LaunchPadService.Requirements r = new LaunchPadService.Requirements();
		r.valid = true; r.fuel = Fluids.KEROSENE; r.oxidizer = Fluids.PEROXIDE; r.amount = 2000;
		return r;
	}
	@Test public void changingPayloadRetainsIncompatibleFuelUntilRecovered() {
		tanks[0].setTankType(Fluids.ETHANOL); tanks[0].setFill(1500);
		service.configure(requirements());
		assertSame(Fluids.ETHANOL, tanks[0].getTankType()); assertEquals(1500, tanks[0].getFill());
		assertFalse(service.fueled(0));
		tanks[0].setFill(0); service.configure(requirements());
		assertSame(Fluids.KEROSENE, tanks[0].getTankType()); assertEquals(0, tanks[0].getFill());
	}
	@Test public void requirementsCheckTypesAmountsAndSolidFuel() {
		LaunchPadService.Requirements r = requirements(); r.solid = 500;
		service.configure(r); tanks[0].setFill(2000); tanks[1].setFill(1999);
		assertFalse(service.fueled(500)); tanks[1].setFill(2000);
		assertFalse(service.fueled(499)); assertTrue(service.fueled(500));
		tanks[0].setTankType(Fluids.ETHANOL); tanks[0].setFill(2000);
		assertFalse(service.fueled(500));
	}
	@Test public void unusedTankAndInvalidPayloadDoNotGrantReadiness() {
		LaunchPadService.Requirements r = requirements(); r.oxidizer = Fluids.NONE;
		service.configure(r); tanks[0].setFill(2000); assertTrue(service.fueled(0));
		r.valid = false; service.configure(r); assertFalse(service.fueled(0));
	}
	@Test public void savedTransferPhasesReloadAsHoldAndOldWorldsRemainUnmanaged() {
		for(String mode : new String[] {"fill", "drain", "hold"}) {
			service.setMode(mode); NBTTagCompound nbt = new NBTTagCompound(); service.write(nbt);
			service.read(nbt); assertEquals("hold", service.mode()); assertTrue(service.active());
			assertFalse(service.filling()); assertFalse(service.draining());
		}
		service.read(new NBTTagCompound()); assertEquals("off", service.mode()); assertFalse(service.active());
	}
	@Test public void invalidModeDoesNotUnlockHardware() {
		service.setMode("hold");
		try { service.setMode("invalid"); fail("Invalid mode accepted"); } catch(IllegalArgumentException expected) {}
		assertEquals("hold", service.mode());
	}
}
