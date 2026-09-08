package com.hbm.tileentity.bomb;

import static org.junit.Assert.*;
import org.junit.Test;
import com.hbm.entity.missile.EntityMissileStealth;
import com.hbm.entity.missile.EntityMissileAntiBallistic;
import net.minecraft.entity.Entity;

public class TrackedTargetTest {
    @Test public void missRequiresEndedExactInterceptorAndLivingExactTarget() {
        Interceptor abm = new Interceptor();
        Target target = new Target();
        target.dimension = 0;
        String shot = abm.getUniqueID().toString();
        String uuid = target.getUniqueID().toString();
        int id = target.getEntityId();
        assertEquals("IN_FLIGHT", TileEntityLaunchPadBase.interceptorOutcome(abm, shot, target, id, uuid, 0));
        abm.isDead = true;
        assertEquals("MISS", TileEntityLaunchPadBase.interceptorOutcome(abm, shot, target, id, uuid, 0));
        assertEquals("UNKNOWN", TileEntityLaunchPadBase.interceptorOutcome(abm, "old-shot", target, id, uuid, 0));
        assertEquals("UNKNOWN", TileEntityLaunchPadBase.interceptorOutcome(null, shot, target, id, uuid, 0));
        assertEquals("TARGET_UNAVAILABLE", TileEntityLaunchPadBase.interceptorOutcome(abm, shot, null, id, uuid, 0));
        target.isDead = true;
        assertEquals("TARGET_UNAVAILABLE", TileEntityLaunchPadBase.interceptorOutcome(abm, shot, target, id, uuid, 0));
    }
	private static class Target extends EntityMissileStealth {
		Target() { super(null); }
		@Override protected void entityInit() { }
	}

	private static class Interceptor extends EntityMissileAntiBallistic {
		Interceptor() { super(null); }
		@Override protected void entityInit() { }
	}
	private static class Pad extends TileEntityLaunchPad {
		Interceptor interceptor = new Interceptor();
		Entity spawned;
		@Override public boolean canLaunch() { return true; }
		@Override public Entity instantiateMissile(int x, int z) { return interceptor; }
		@Override public void finalizeLaunch(Entity entity) { spawned = entity; }
	}
	@Test public void nativeEntityHandoffAssignsExactDistantTargetBeforeSpawning() {
		Pad pad = new Pad();
		Target target = new Target();
		target.setPosition(5000, 2000, 0);
		assertTrue(pad.sendCommandEntity(target));
		assertSame(pad.interceptor, pad.spawned);
		assertSame(target, pad.interceptor.tracking);
	}

	@Test public void targetIdentityRejectsDeadReusedAndOtherDimensionEntities() {
		Target target = new Target();
		target.dimension = 0;
		int id = target.getEntityId();
		String uuid = target.getUniqueID().toString();
		assertTrue(TileEntityLaunchPadBase.matchesTrackedTarget(target, id, uuid, 0));
		assertFalse(TileEntityLaunchPadBase.matchesTrackedTarget(null, id, uuid, 0));
		assertFalse(TileEntityLaunchPadBase.matchesTrackedTarget(target, id + 1, uuid, 0));
		assertFalse(TileEntityLaunchPadBase.matchesTrackedTarget(target, id, "old-uuid", 0));
		assertFalse(TileEntityLaunchPadBase.matchesTrackedTarget(target, id, uuid, 1));
		target.isDead = true;
		assertFalse(TileEntityLaunchPadBase.matchesTrackedTarget(target, id, uuid, 0));
	}
}
