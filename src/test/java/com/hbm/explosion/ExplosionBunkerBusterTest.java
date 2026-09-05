package com.hbm.explosion;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.minecraft.util.Vec3;

public class ExplosionBunkerBusterTest {
	@Test
	public void thermonuclearEffectFollowsPenetrationAndStartsInsideTheBunker() {
		Impact impact = new Impact();
		impact.blocks.put(99, 2F);
		impact.blocks.put(98, 6F);
		impact.blocks.put(97, 6F);
		impact.blocks.put(96, 98F);
		impact.blocks.put(95, 100F);
		impact.fire(true, 250F);
		assertTrue(impact.thermonuclear);
		assertEquals(250F, impact.strength, 0F);
		assertEquals(94.5, impact.y, 0.0001);
		assertEquals(0.5, impact.x, 0.0001);
		assertEquals(0.5, impact.z, 0.0001);
		assertEquals("remove:99,remove:98,remove:97,remove:96,remove:95,explode", String.join(",", impact.events));
	}

	@Test
	public void ordinaryGroundDoesNotTriggerThermonuclearYield() {
		Impact impact = new Impact();
		impact.blocks.put(99, 2F);
		impact.blocks.put(97, 6F);
		impact.blocks.put(95, 39.99F);
		impact.fire(true, 250F);
		assertFalse(impact.thermonuclear);
		assertEquals(4F, impact.strength, 0F);
	}

	@Test
	public void anInitialImpenetrableBlockOnlyGetsTheReducedImpactEffect() {
		Impact impact = new Impact();
		impact.blocks.put(99, 101F);
		impact.fire(true, 250F);
		assertFalse(impact.thermonuclear);
		assertEquals(4F, impact.strength, 0F);
		assertEquals(1, impact.events.size());
		assertTrue(impact.y >= 100);
		assertTrue(impact.blocks.containsKey(99));
	}

	@Test
	public void armedThermonuclearPayloadDetonatesAtLastReachablePointIfBlocked() {
		Impact impact = new Impact();
		impact.blocks.put(99, 100F);
		impact.blocks.put(98, 101F);
		impact.fire(true, 250F);
		assertTrue(impact.thermonuclear);
		assertEquals(99.5, impact.y, 0.0001);
		assertTrue(impact.blocks.containsKey(98));
	}

	@Test
	public void conventionalBunkerBusterKeepsItsExistingEffect() {
		Impact impact = new Impact();
		impact.blocks.put(99, 100F);
		impact.fire(false, 20F);
		assertFalse(impact.thermonuclear);
		assertEquals(20F, impact.strength, 0F);
		assertEquals(98.5, impact.y, 0.0001);
	}

	private static class Impact implements BunkerBusterPenetration.BlockAccess, ExplosionBunkerBuster.Detonation {
		final Map<Integer, Float> blocks = new HashMap<Integer, Float>();
		final List<String> events = new ArrayList<String>();
		double x, y, z;
		float strength;
		boolean thermonuclear;

		void fire(boolean thermonuclear, float strength) {
			ExplosionBunkerBuster.detonate(this, Vec3.createVectorHelper(0.5, 100, 0.5), Vec3.createVectorHelper(0, -2, 0), strength, thermonuclear, this);
		}

		@Override public float getResistance(int x, int y, int z) { return blocks.getOrDefault(y, BunkerBusterPenetration.AIR); }
		@Override public boolean removeBlock(int x, int y, int z) {
			blocks.remove(y);
			events.add("remove:" + y);
			return true;
		}
		@Override public void explode(double x, double y, double z, float strength, boolean thermonuclear) {
			events.add("explode");
			this.x = x; this.y = y; this.z = z;
			this.strength = strength;
			this.thermonuclear = thermonuclear;
		}
	}
}
