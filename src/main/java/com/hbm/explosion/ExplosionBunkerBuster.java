package com.hbm.explosion;

import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ExplosionBunkerBuster {
	interface Detonation {
		void explode(double x, double y, double z, float strength, boolean thermonuclear);
	}

	static void detonate(BunkerBusterPenetration.BlockAccess blocks, Vec3 impact, Vec3 direction, float strength, boolean thermonuclear, Detonation effect) {
		BunkerBusterPenetration.Result result = BunkerBusterPenetration.penetrate(blocks,
				impact.xCoord, impact.yCoord, impact.zCoord, direction.xCoord, direction.yCoord, direction.zCoord);
		effect.explode(result.x, result.y, result.z, result.armed ? strength : 4F, result.armed && thermonuclear);
	}

	public static void detonate(final World world, MovingObjectPosition hit, Vec3 direction, float strength) {
		detonate(world, hit, direction, strength, false);
	}

	public static void detonate(final World world, MovingObjectPosition hit, Vec3 direction, float strength, boolean thermonuclear) {
		if(world.isRemote) return;

		detonate(new BunkerBusterPenetration.BlockAccess() {
			@Override
			public float getResistance(int x, int y, int z) {
				if(y < 0 || y >= world.getHeight() || !world.blockExists(x, y, z)) return Float.POSITIVE_INFINITY;
				Block block = world.getBlock(x, y, z);
				if(block.isAir(world, x, y, z)) return BunkerBusterPenetration.AIR;
				if(block.getBlockHardness(world, x, y, z) < 0) return Float.POSITIVE_INFINITY;
				// Same effective value shown by HBM's ItemBlockBlastInfo tooltip, not setResistance's raw input.
				return block.getExplosionResistance(null);
			}

			@Override
			public boolean removeBlock(int x, int y, int z) {
				return world.setBlockToAir(x, y, z);
			}
		}, hit.hitVec, direction, strength, thermonuclear, new Detonation() {
			@Override
			public void explode(double x, double y, double z, float yield, boolean thermonuclear) {
				if(thermonuclear) {
					world.spawnEntityInWorld(EntityNukeExplosionMK5.statFac(world, (int) yield, x, y, z));
					EntityNukeTorex.statFacStandard(world, x, y, z, yield);
				} else {
					ExplosionLarge.explode(world, x, y, z, yield, true, false, true);
				}
			}
		});
	}
}
