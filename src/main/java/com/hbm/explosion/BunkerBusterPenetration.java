package com.hbm.explosion;

public class BunkerBusterPenetration {
	public static final float MAX_RESISTANCE = 100F;
	public static final float ARMING_RESISTANCE = 40F;
	public static final int MAX_DEPTH = 96;
	public static final float AIR = -1F;

	public interface BlockAccess {
		/** AIR for empty space, positive infinity for unbreakable or unavailable blocks. */
		float getResistance(int x, int y, int z);
		boolean removeBlock(int x, int y, int z);
	}

	public static class Result {
		public final double x, y, z;
		public final boolean armed;

		public Result(double x, double y, double z, boolean armed) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.armed = armed;
		}
	}

	public static Result penetrate(BlockAccess blocks, double x, double y, double z, double dx, double dy, double dz) {
		double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
		if(!(length > 0) || !Double.isFinite(length)) return new Result(x, y, z, false);
		dx /= length;
		dy /= length;
		dz /= length;

		// Start just inside the impacted voxel; distances still use the actual impact point.
		int bx = (int) Math.floor(x + dx * 0.00001);
		int by = (int) Math.floor(y + dy * 0.00001);
		int bz = (int) Math.floor(z + dz * 0.00001);
		int sx = (int) Math.signum(dx), sy = (int) Math.signum(dy), sz = (int) Math.signum(dz);
		double stepX = sx == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dx);
		double stepY = sy == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dy);
		double stepZ = sz == 0 ? Double.POSITIVE_INFINITY : Math.abs(1 / dz);
		double nextX = boundaryDistance(bx, sx, x, dx);
		double nextY = boundaryDistance(by, sy, y, dy);
		double nextZ = boundaryDistance(bz, sz, z, dz);
		boolean armed = false;
		Result result = new Result(x - dx * 0.01, y - dy * 0.01, z - dz * 0.01, false);

		for(double distance = 0; distance < MAX_DEPTH;) {
			float resistance = blocks.getResistance(bx, by, bz);
			if(resistance == AIR) {
				if(armed) return new Result(bx + 0.5, by + 0.5, bz + 0.5, true);
			} else {
				if(!Float.isFinite(resistance) || resistance < 0 || resistance > MAX_RESISTANCE) break;
				if(!blocks.removeBlock(bx, by, bz)) break;
				if(resistance >= ARMING_RESISTANCE) armed = true;
			}
			result = new Result(bx + 0.5, by + 0.5, bz + 0.5, armed);

			// Voxel traversal visits even very short diagonal intersections, once per block.
			distance = Math.min(nextX, Math.min(nextY, nextZ));
			if(distance >= MAX_DEPTH) break;
			if(nextX <= distance) { bx += sx; nextX += stepX; }
			if(nextY <= distance) { by += sy; nextY += stepY; }
			if(nextZ <= distance) { bz += sz; nextZ += stepZ; }
		}
		return result;
	}

	private static double boundaryDistance(int block, int step, double origin, double direction) {
		if(step == 0) return Double.POSITIVE_INFINITY;
		return (block + (step > 0 ? 1 : 0) - origin) / direction;
	}
}
