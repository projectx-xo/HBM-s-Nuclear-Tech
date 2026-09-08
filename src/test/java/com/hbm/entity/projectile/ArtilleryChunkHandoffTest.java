package com.hbm.entity.projectile;

import static org.junit.Assert.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.hbm.entity.logic.IChunkLoader;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.entity.missile.EntityMissileAntiBallistic;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.*;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.SaveHandlerMP;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import net.minecraftforge.common.ForgeChunkManager.Type;

public class ArtilleryChunkHandoffTest {
	@Test
	public void missileKeepsTickingAcrossAbsentChunks() throws Exception {
		TestWorld world = new TestWorld();
		checkFlight(world, new TestMissile(world));
	}

	@Test
	public void interceptorKeepsTickingAcrossAbsentChunks() throws Exception {
		TestWorld world = new TestWorld();
		checkFlight(world, new TestInterceptor(world));
	}

	@Test
	public void rocketKeepsTickingAcrossAbsentChunks() throws Exception {
		TestWorld world = new TestWorld();
		checkFlight(world, new TestRocket(world));
	}

	@Test
	public void shellKeepsTickingAcrossAbsentChunks() throws Exception {
		TestWorld world = new TestWorld();
		checkFlight(world, new TestShell(world));
	}

	private void checkFlight(TestWorld world, Entity projectile) throws Exception {
		Constructor<Ticket> constructor = Ticket.class.getDeclaredConstructor(String.class, Type.class, World.class);
		constructor.setAccessible(true);
		Ticket ticket = constructor.newInstance("hbm-handoff-test", Type.ENTITY, world);
		Multimap<String, Ticket> tickets = ArrayListMultimap.create();
		tickets.put("hbm-handoff-test", ticket);
		Map ticketWorlds = forgeMap("tickets");
		Map forcedWorlds = forgeMap("forcedChunks");
		ticketWorlds.put(world, tickets);
		forcedWorlds.put(world, ImmutableSetMultimap.of());
		try {
			projectile.setPosition(842, 265, 1876);
			world.getChunkFromChunkCoords(52, 117).addEntity(projectile);
			((IChunkLoader) projectile).init(ticket);
			for(int i = 0; i < 20; i++) {
				// There are no players and only the old membership chunk exists.
				world.chunks.keySet().retainAll(java.util.Collections.singleton(
					new ChunkCoordIntPair(projectile.chunkCoordX, projectile.chunkCoordZ)));
				world.updateEntityWithOptionalForce(projectile, true);
				assertTrue("Projectile lost chunk membership at tick " + i, projectile.addedToChunk);
				assertEquals(i + 1, projectile.ticksExisted);
				assertEquals((int) Math.floor(projectile.posX / 16), projectile.chunkCoordX);
				assertEquals((int) Math.floor(projectile.posZ / 16), projectile.chunkCoordZ);
			}
		} finally {
			ForgeChunkManager.releaseTicket(ticket);
			ticketWorlds.remove(world);
			forcedWorlds.remove(world);
		}
	}

	private static Map forgeMap(String name) throws Exception {
		Field field = ForgeChunkManager.class.getDeclaredField(name);
		field.setAccessible(true);
		return (Map) field.get(null);
	}

	// Stub only flight physics and registration; exercise the production loader
	// and the real Forge-patched World membership/tick gates.
	private static class TestRocket extends EntityArtilleryRocket {
		TestRocket(World world) { super(world); }
		@Override protected void entityInit() { }
		@Override public void onUpdate() {
			setPosition(posX + 25, posY, posZ + 18);
			loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));
		}
	}

	private static class TestShell extends EntityArtilleryShell {
		TestShell(World world) { super(world); }
		@Override protected void entityInit() { }
		@Override public void onUpdate() {
			setPosition(posX + 25, posY, posZ + 18);
			loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));
		}
	}

	private static class TestMissile extends EntityMissileBaseNT {
		TestMissile(World world) { super(world); }
		@Override protected void entityInit() { }
		@Override public ItemStack getMissileItemForInfo() { return null; }
		@Override public void onMissileImpact(net.minecraft.util.MovingObjectPosition mop) { }
		@Override public java.util.List<ItemStack> getDebris() { return java.util.Collections.emptyList(); }
		@Override public ItemStack getDebrisRareDrop() { return null; }
		@Override public void onUpdate() {
			setPosition(posX + 25, posY, posZ + 18);
			loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));
		}
	}

	private static class TestInterceptor extends EntityMissileAntiBallistic {
		TestInterceptor(World world) { super(world); }
		@Override protected void entityInit() { }
		@Override public void onUpdate() {
			setPosition(posX + 25, posY, posZ + 18);
			loadNeighboringChunks((int) Math.floor(posX / 16), (int) Math.floor(posZ / 16));
		}
	}

	private static class TestWorld extends World {
		final Map<ChunkCoordIntPair, Chunk> chunks = new HashMap<ChunkCoordIntPair, Chunk>();
		TestWorld() {
			super(new SaveHandlerMP(), "handoff", new WorldSettings(1, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT), new WorldProviderSurface(), new Profiler());
		}
		@Override protected IChunkProvider createChunkProvider() { return null; }
		@Override protected int func_152379_p() { return 0; }
		@Override public Entity getEntityByID(int id) { return null; }
		@Override protected boolean chunkExists(int x, int z) { return chunks.containsKey(new ChunkCoordIntPair(x, z)); }
		@Override public Chunk getChunkFromChunkCoords(int x, int z) {
			ChunkCoordIntPair key = new ChunkCoordIntPair(x, z);
			Chunk chunk = chunks.get(key);
			if(chunk == null) { chunk = new Chunk(this, x, z); chunks.put(key, chunk); }
			return chunk;
		}
	}
}
