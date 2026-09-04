package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hbm.entity.missile.EntityMissileAntiBallistic;
import com.hbm.entity.missile.EntityMissileBaseNT;
import com.hbm.items.weapon.ItemCustomMissile;
import com.hbm.items.weapon.ItemMissile;
import com.hbm.tileentity.TileEntityProxyBase;
import com.hbm.tileentity.bomb.TileEntityLaunchPadRusted;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class IntelTargetDetector implements IntelTargetScanner.TargetAccess {
	private final World world;

	public IntelTargetDetector(World world) {
		this.world = world;
	}

	@Override
	public boolean isChunkLoaded(int x, int z) {
		return world.getChunkProvider().chunkExists(x, z);
	}

	@Override
	public Iterable<IntelFinding> targetsInChunk(int x, int z) {
		if(!isChunkLoaded(x, z)) return new ArrayList<IntelFinding>();
		return targetsInChunk(world.getChunkFromChunkCoords(x, z));
	}

	List<IntelFinding> targetsInChunk(Chunk chunk) {
		List<IntelFinding> targets = new ArrayList<IntelFinding>();
		for(Object raw : new ArrayList(chunk.chunkTileEntityMap.values())) {
			TileEntity tile = (TileEntity) raw;
			if(tile.isInvalid() || tile instanceof TileEntityProxyBase) continue;
			String id = String.valueOf(Block.blockRegistry.getNameForObject(tile.getBlockType()));
			String type = typeForBlock(id);
			if(!type.isEmpty()) {
				IntelClassification classification = "SILO_HATCH".equals(type) ? IntelClassification.SILO_HATCH
						: "RADAR".equals(type) ? IntelClassification.RADAR : IntelClassification.LAUNCH_INFRASTRUCTURE;
				targets.add(target(classification, type, id, 1, tile.xCoord, tile.yCoord, tile.zCoord));
			}
			boolean launcher = "LAUNCHPAD".equals(type) || "LAUNCH_TABLE".equals(type) || "COMPACT_LAUNCHER".equals(type);
			Map<String, Integer> missiles = new LinkedHashMap<String, Integer>();
			if(tile instanceof IInventory) {
				IInventory inventory = (IInventory) tile;
				for(int slot = 0; slot < inventory.getSizeInventory(); slot++) {
					ItemStack stack = inventory.getStackInSlot(slot);
					if(stack == null || stack.stackSize <= 0) continue;
					Item item = stack.getItem();
					if(!(item instanceof ItemMissile) && !(item instanceof ItemCustomMissile)) continue;
					String itemId = String.valueOf(Item.itemRegistry.getNameForObject(item));
					Integer count = missiles.get(itemId);
					missiles.put(itemId, (count == null ? 0 : count) + stack.stackSize);
				}
			}
			if(tile instanceof TileEntityLaunchPadRusted && ((TileEntityLaunchPadRusted) tile).missileLoaded && missiles.isEmpty()) {
				missiles.put("hbm:item.missile_doomsday_rusted", 1);
			}
			for(Map.Entry<String, Integer> missile : missiles.entrySet()) {
				targets.add(target(IntelClassification.MISSILE, launcher ? "LOADED_MISSILE" : "STORED_MISSILE",
						missile.getKey(), missile.getValue(), tile.xCoord, tile.yCoord, tile.zCoord));
			}
		}
		for(List entities : chunk.entityLists) {
			for(Object raw : entities) {
				if(!(raw instanceof EntityMissileBaseNT) && !(raw instanceof EntityMissileAntiBallistic)) continue;
				Entity entity = (Entity) raw;
				if(entity.isDead) continue;
				String id = EntityList.getEntityString(entity);
				if(id == null) id = entity.getClass().getName();
				IntelFinding finding = target(IntelClassification.MISSILE, "FLYING_MISSILE", id, 1,
						MathHelper.floor_double(entity.posX), MathHelper.floor_double(entity.posY), MathHelper.floor_double(entity.posZ));
				finding.sourceEntityId = entity.getUniqueID();
				targets.add(finding);
			}
		}
		return targets;
	}

	public static String typeForBlock(String registryId) {
		switch(registryId) {
			case "hbm:tile.launch_pad":
			case "hbm:tile.launch_pad_large":
			case "hbm:tile.launch_pad_rusted": return "LAUNCHPAD";
			case "hbm:tile.launch_table": return "LAUNCH_TABLE";
			case "hbm:tile.compact_launcher": return "COMPACT_LAUNCHER";
			case "hbm:tile.silo_hatch":
			case "hbm:tile.silo_hatch_large": return "SILO_HATCH";
			case "hbm:tile.machine_radar":
			case "hbm:tile.machine_radar_large": return "RADAR";
			case "hbm:tile.machine_missile_assembly": return "MISSILE_ASSEMBLY";
			default: return "";
		}
	}

	private IntelFinding target(IntelClassification classification, String type, String id, int count, int x, int y, int z) {
		IntelFinding finding = new IntelFinding();
		finding.classification = classification;
		finding.targetType = type;
		finding.targetId = id;
		finding.targetCount = count;
		finding.minX = finding.maxX = x;
		finding.minY = finding.maxY = y;
		finding.minZ = finding.maxZ = z;
		finding.confidence = 1F;
		finding.machinery = classification != IntelClassification.MISSILE;
		finding.launchInfrastructure = classification == IntelClassification.LAUNCH_INFRASTRUCTURE
				|| classification == IntelClassification.SILO_HATCH || "LOADED_MISSILE".equals(type);
		finding.communications = classification == IntelClassification.RADAR;
		finding.reinforced = classification == IntelClassification.SILO_HATCH;
		return finding;
	}
}
