package com.hbm.saveddata.satellites.intel;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class IntelBlockClassifier {

	private final Map<String, BlockIntelProperties> cache = new HashMap<String, BlockIntelProperties>();

	public IntelClassification classifySurface(World world, int x, int y, int z) {
		return classify(properties(world, x, y, z));
	}

	public IntelClassification classifySubsurface(World world, int x, int y, int z) {
		return classify(properties(world, x, y, z));
	}

	private IntelClassification classify(BlockIntelProperties props) {
		if(props.launchInfrastructure) return IntelClassification.LAUNCH_INFRASTRUCTURE;
		if(props.communications) return IntelClassification.COMMUNICATIONS;
		if(props.power) return IntelClassification.POWER;
		if(props.machinery) return IntelClassification.MACHINERY;
		if(props.reinforced) return IntelClassification.REINFORCED_STRUCTURE;
		if(props.constructed) return IntelClassification.STRUCTURE;
		return IntelClassification.NATURAL;
	}

	public BlockIntelProperties properties(World world, int x, int y, int z) {
		Block block = world.getBlock(x, y, z);
		int meta = world.getBlockMetadata(x, y, z);
		String registryId = registryId(block);
		String key = registryId + "#" + meta;
		BlockIntelProperties base = cache.get(key);
		if(base == null) {
			base = buildBase(block, registryId, meta);
			cache.put(key, base);
		}
		BlockIntelProperties props = new BlockIntelProperties(base);
		if(block != null && block.hasTileEntity(meta)) {
			TileEntity tile = world.getTileEntity(x, y, z);
			if(tile != null) applyNameFlags(props, tile.getClass().getName().toLowerCase(Locale.US), true);
		}
		return props;
	}

	private BlockIntelProperties buildBase(Block block, String registryId, int meta) {
		BlockIntelProperties props = new BlockIntelProperties();
		props.registryId = registryId;
		props.metadata = meta;
		props.materialCategory = block == null || block.getMaterial() == null ? "" : block.getMaterial().toString();
		props.effectiveBlastResistance = effectiveBlastResistance(block);
		String names = (registryId + " " + (block == null ? "" : block.getClass().getName())).toLowerCase(Locale.US);
		props.constructed = containsAny(names, "concrete", "brick", "plating", "metal_block", "blockmetal", "machine", "vault", "bunker", "reinforced");
		props.reinforced = props.constructed && (props.effectiveBlastResistance >= 40F || containsAny(names, "reinforced", "bunker", "concrete", "vault", "plating"));
		applyNameFlags(props, names, false);
		return props;
	}

	private void applyNameFlags(BlockIntelProperties props, String names, boolean tileEntity) {
		if(tileEntity || names.contains("machine") || names.contains("tileentity")) {
			props.machinery = true;
			props.constructed = true;
		}
		if(containsAny(names, "launch", "launcher", "missile", "silo", "launchpad")) {
			props.launchInfrastructure = true;
			props.machinery = true;
			props.constructed = true;
		}
		if(containsAny(names, "radar", "satlink", "satellite", "radio", "antenna", "rtty", "communication")) {
			props.communications = true;
			props.machinery = true;
			props.constructed = true;
		}
		if(containsAny(names, "reactor", "generator", "battery", "transformer", "capacitor", "power", "turbine", "substation")) {
			props.power = true;
			props.machinery = true;
			props.constructed = true;
		}
	}

	private boolean containsAny(String value, String... needles) {
		for(String needle : needles) if(value.contains(needle)) return true;
		return false;
	}

	private String registryId(Block block) {
		if(block == null) return "minecraft:air";
		Object name = Block.blockRegistry.getNameForObject(block);
		if(name != null) return String.valueOf(name);
		return block.getClass().getName();
	}

	public float effectiveBlastResistance(Block block) {
		if(block == null) return 0F;
		try {
			return Math.max(0F, block.getExplosionResistance(null));
		} catch(Throwable ignored) { }
		for(String fieldName : new String[] {"blockResistance", "field_149781_w"}) {
			try {
				Field field = Block.class.getDeclaredField(fieldName);
				field.setAccessible(true);
				return Math.max(0F, field.getFloat(block) / 5F);
			} catch(Throwable ignored) { }
		}
		return 0F;
	}

	public static class BlockIntelProperties {
		public String registryId = "";
		public int metadata;
		public String materialCategory = "";
		public boolean constructed;
		public boolean reinforced;
		public boolean machinery;
		public boolean power;
		public boolean communications;
		public boolean launchInfrastructure;
		public float effectiveBlastResistance;

		public BlockIntelProperties() { }

		public BlockIntelProperties(BlockIntelProperties other) {
			this.registryId = other.registryId;
			this.metadata = other.metadata;
			this.materialCategory = other.materialCategory;
			this.constructed = other.constructed;
			this.reinforced = other.reinforced;
			this.machinery = other.machinery;
			this.power = other.power;
			this.communications = other.communications;
			this.launchInfrastructure = other.launchInfrastructure;
			this.effectiveBlastResistance = other.effectiveBlastResistance;
		}
	}
}
