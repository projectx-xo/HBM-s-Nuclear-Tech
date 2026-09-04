package com.hbm.explosion;

import net.minecraft.block.Block;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class ExplosionBunkerBuster {

	public static void detonate(final World world, MovingObjectPosition hit, Vec3 direction, float strength) {
		if(world.isRemote) return;

		BunkerBusterPenetration.Result result = BunkerBusterPenetration.penetrate(new BunkerBusterPenetration.BlockAccess() {
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
		}, hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord, direction.xCoord, direction.yCoord, direction.zCoord);

		ExplosionLarge.explode(world, result.x, result.y, result.z, result.armed ? strength : 4F, true, false, true);
	}
}
