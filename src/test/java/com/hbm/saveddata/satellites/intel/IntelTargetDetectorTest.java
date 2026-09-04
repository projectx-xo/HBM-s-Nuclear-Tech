package com.hbm.saveddata.satellites.intel;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;
import java.lang.reflect.Field;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hbm.entity.missile.EntityMissileCustom;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.saveddata.satellites.SatelliteCombinedIntel;
import com.hbm.tileentity.TileEntityProxyInventory;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.RegistrySimple;
import net.minecraft.world.chunk.Chunk;

public class IntelTargetDetectorTest {
	private static final Block LAUNCHER = new Block(Material.iron) { };
	private static final Block STORAGE = new Block(Material.wood) { };
	private static final Block RUSTED = new Block(Material.iron) { };
	private static final Item MISSILE = new ItemCustomMissile();

	@BeforeClass
	public static void registerFixtures() throws Exception {
		// Populate only the name maps; full Forge registration requires LaunchWrapper.
		names(Block.blockRegistry).put("hbm:tile.launch_table", LAUNCHER);
		names(Block.blockRegistry).put("test:storage", STORAGE);
		names(Block.blockRegistry).put("hbm:tile.launch_pad_rusted", RUSTED);
		names(Item.itemRegistry).put("hbm:item.missile_custom", MISSILE);
	}

	@AfterClass
	public static void removeFixtures() throws Exception {
		names(Block.blockRegistry).remove("hbm:tile.launch_table");
		names(Block.blockRegistry).remove("test:storage");
		names(Block.blockRegistry).remove("hbm:tile.launch_pad_rusted");
		names(Item.itemRegistry).remove("hbm:item.missile_custom");
	}

	private static Map names(RegistrySimple registry) throws Exception {
		Field field = RegistrySimple.class.getDeclaredField("registryObjects");
		field.setAccessible(true);
		return (Map) field.get(registry);
	}

	@Test
	public void readsRealInventoryStacksAtUndergroundCoreAndSkipsProxyPorts() {
		Chunk chunk = new Chunk(null, 0, 0);
		TileEntityChest launcher = inventory(LAUNCHER);
		launcher.xCoord = 3;
		launcher.yCoord = 21;
		launcher.zCoord = 5;
		launcher.setInventorySlotContents(0, new ItemStack(MISSILE, 2));
		launcher.setInventorySlotContents(1, new ItemStack(MISSILE, 3));
		chunk.chunkTileEntityMap.put("core", launcher);
		chunk.chunkTileEntityMap.put("proxy", new TileEntityProxyInventory() {
			@Override public Block getBlockType() { throw new AssertionError("Proxy must be skipped"); }
		});
		List<IntelFinding> targets = new IntelTargetDetector(null).targetsInChunk(chunk);
		assertEquals(2, targets.size());
		IntelFinding missile = find(targets, "LOADED_MISSILE");
		assertEquals(5, missile.targetCount);
		assertEquals("hbm:item.missile_custom", missile.targetId);
		assertEquals(21, missile.minY);
		assertEquals(3, missile.minX);
		assertTrue(find(targets, "LAUNCH_TABLE").launchInfrastructure);
		assertEquals(2, launcher.getStackInSlot(0).stackSize);
		assertEquals(3, launcher.getStackInSlot(1).stackSize);
	}

	@Test
	public void detectsRustedPadsBuiltInMissile() {
		Chunk chunk = new Chunk(null, 0, 0);
		TileEntityLaunchPadRusted rusted = new TileEntityLaunchPadRusted() {
			@Override public Block getBlockType() { return RUSTED; }
		};
		rusted.missileLoaded = true;
		chunk.chunkTileEntityMap.put("core", rusted);
		IntelFinding missile = find(new IntelTargetDetector(null).targetsInChunk(chunk), "LOADED_MISSILE");
		assertEquals("hbm:item.missile_doomsday_rusted", missile.targetId);
		assertEquals(1, missile.targetCount);
	}

	@Test
	public void storedAndFlyingMissilesDoNotTurnAnUnrelatedBunkerIntoASilo() {
		Chunk chunk = new Chunk(null, 0, 0);
		TileEntityChest storage = inventory(STORAGE);
		storage.setInventorySlotContents(0, new ItemStack(MISSILE));
		chunk.chunkTileEntityMap.put("storage", storage);
		// Suppress only Forge's world/chunk-ticket initialization for this isolated entity.
		EntityMissileCustom flying = new EntityMissileCustom(null) {
			@Override protected void entityInit() { }
		};
		flying.posY = 300;
		chunk.entityLists[15].add(flying);
		List<IntelFinding> targets = new IntelTargetDetector(null).targetsInChunk(chunk);
		IntelFinding airborne = find(targets, "FLYING_MISSILE");
		assertEquals(300, airborne.minY);
		assertEquals(flying.getUniqueID(), airborne.sourceEntityId);
		assertFalse(airborne.launchInfrastructure);
		assertFalse(find(targets, "STORED_MISSILE").launchInfrastructure);
		IntelScanResult result = new IntelScanResult();
		result.mode = IntelScanMode.COMBINED;
		result.findings.addAll(targets);
		IntelFinding bunker = new IntelFinding();
		bunker.classification = IntelClassification.BUNKER;
		bunker.reinforced = true;
		result.findings.add(bunker);
		new SatelliteCombinedIntel().correlateCombinedFindings(result);
		for(IntelFinding finding : result.findings) assertNotEquals(IntelClassification.POSSIBLE_SILO, finding.classification);
		flying.isDead = true;
		assertEquals(1, new IntelTargetDetector(null).targetsInChunk(chunk).size());
	}

	private static TileEntityChest inventory(final Block block) {
		return new TileEntityChest() {
			@Override public Block getBlockType() { return block; }
		};
	}

	private static IntelFinding find(List<IntelFinding> targets, String type) {
		for(IntelFinding target : targets) if(type.equals(target.targetType)) return target;
		throw new AssertionError("Missing target " + type);
	}
}
