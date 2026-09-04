package com.hbm.saveddata.satellites.intel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class IntelSubsurfaceScanner {

	public static final int Y_STRIDE = 2;
	private final IntelBlockClassifier classifier;

	public IntelSubsurfaceScanner(IntelBlockClassifier classifier) {
		this.classifier = classifier;
	}

	public int process(World world, IntelScanJob job, IntelScanResult result, int budget) {
		int consumed = 0;
		while(consumed < budget && job.phaseCursor < 64 * 64) {
			int index = job.phaseCursor++;
			int x = result.targetX - 32 + (index % 64);
			int z = result.targetZ - 32 + (index / 64);
			consumed++;
			job.processedWork++;

			if(!world.getChunkProvider().chunkExists(x >> 4, z >> 4)) {
				job.missingColumns++;
				continue;
			}

			if(result.mode == IntelScanMode.SUBSURFACE) result.coveredColumns++;
			int surface = Math.max(1, world.getHeightValue(x, z) - 1);
			for(int y = surface - Y_STRIDE; y > 0; y -= Y_STRIDE) {
				if(result.subsurfaceCells.size() >= IntelScanResult.MAX_SUBSURFACE_CELLS) break;
				Block block = world.getBlock(x, y, z);
				if(block == null || block == Blocks.air) {
					result.subsurfaceCells.add(new IntelSurfaceCell(x, y, z, IntelClassification.CAVITY, false));
					continue;
				}
				IntelBlockClassifier.BlockIntelProperties props = classifier.properties(world, x, y, z);
				if(props.reinforced || props.machinery || props.power || props.communications || props.launchInfrastructure) {
					IntelClassification classification = classifier.classifySubsurface(world, x, y, z).forMode(result.mode);
					result.subsurfaceCells.add(new IntelSurfaceCell(x, y, z, classification, true));
				}
			}
		}
		return consumed;
	}

	public void finalizeFindings(IntelScanResult result) {
		Map<String, IntelSurfaceCell> cavities = new HashMap<String, IntelSurfaceCell>();
		List<IntelSurfaceCell> evidence = new ArrayList<IntelSurfaceCell>();
		for(IntelSurfaceCell cell : result.subsurfaceCells) {
			if(cell.classification == IntelClassification.CAVITY) cavities.put(key(cell.x, cell.y, cell.z), cell);
			else evidence.add(cell);
		}

		Set<String> visited = new HashSet<String>();
		for(IntelSurfaceCell seed : cavities.values()) {
			String seedKey = key(seed.x, seed.y, seed.z);
			if(!visited.add(seedKey)) continue;
			ArrayDeque<IntelSurfaceCell> queue = new ArrayDeque<IntelSurfaceCell>();
			queue.add(seed);
			int minX = seed.x, maxX = seed.x, minY = seed.y, maxY = seed.y, minZ = seed.z, maxZ = seed.z;
			int count = 0;
			while(!queue.isEmpty() && count < IntelScanResult.MAX_SUBSURFACE_CELLS) {
				IntelSurfaceCell cell = queue.removeFirst();
				count++;
				minX = Math.min(minX, cell.x); maxX = Math.max(maxX, cell.x);
				minY = Math.min(minY, cell.y); maxY = Math.max(maxY, cell.y);
				minZ = Math.min(minZ, cell.z); maxZ = Math.max(maxZ, cell.z);
				visitNeighbor(cavities, visited, queue, cell.x + 1, cell.y, cell.z);
				visitNeighbor(cavities, visited, queue, cell.x - 1, cell.y, cell.z);
				visitNeighbor(cavities, visited, queue, cell.x, cell.y, cell.z + 1);
				visitNeighbor(cavities, visited, queue, cell.x, cell.y, cell.z - 1);
				visitNeighbor(cavities, visited, queue, cell.x, cell.y + Y_STRIDE, cell.z);
				visitNeighbor(cavities, visited, queue, cell.x, cell.y - Y_STRIDE, cell.z);
			}
			if(count < 4 || result.findings.size() >= IntelScanResult.MAX_FINDINGS) continue;

			boolean reinforced = false, machinery = false, launch = false, power = false, communications = false;
			for(IntelSurfaceCell cell : evidence) {
				if(cell.x < minX - 2 || cell.x > maxX + 2 || cell.y < minY - 2 || cell.y > maxY + 2 || cell.z < minZ - 2 || cell.z > maxZ + 2) continue;
				reinforced |= cell.classification == IntelClassification.REINFORCED_STRUCTURE;
				machinery |= cell.classification == IntelClassification.MACHINERY;
				launch |= cell.classification == IntelClassification.LAUNCH_INFRASTRUCTURE;
				power |= cell.classification == IntelClassification.POWER;
				communications |= cell.classification == IntelClassification.COMMUNICATIONS;
			}

			int dx = maxX - minX + 1;
			int dy = maxY - minY + Y_STRIDE;
			int dz = maxZ - minZ + 1;
			IntelClassification classification;
			if(reinforced && launch) classification = IntelClassification.POSSIBLE_SILO;
			else if(reinforced && count >= 24) classification = IntelClassification.BUNKER;
			else if((dx >= dz * 3 || dz >= dx * 3) && Math.min(dx, dz) <= 6) classification = IntelClassification.TUNNEL;
			else classification = IntelClassification.CAVITY;

			float confidence = 0.15F;
			if(reinforced) confidence += 0.25F;
			if(count >= 24) confidence += 0.20F;
			if(machinery) confidence += 0.20F;
			if(launch) confidence += 0.25F;
			if(power) confidence += 0.10F;
			confidence = Math.max(0F, Math.min(1F, confidence));

			IntelFinding finding = new IntelFinding();
			finding.classification = classification.forMode(result.mode);
			finding.minX = minX; finding.maxX = maxX;
			finding.minY = minY; finding.maxY = maxY;
			finding.minZ = minZ; finding.maxZ = maxZ;
			finding.confidence = confidence;
			finding.reinforced = reinforced;
			finding.machinery = machinery;
			finding.launchInfrastructure = result.mode == IntelScanMode.COMBINED && launch;
			finding.power = power;
			finding.communications = communications;
			result.findings.add(finding);
		}

		addEvidenceFinding(result, evidence, IntelClassification.MACHINERY);
		addEvidenceFinding(result, evidence, IntelClassification.POWER);
		addEvidenceFinding(result, evidence, IntelClassification.COMMUNICATIONS);
	}

	private void addEvidenceFinding(IntelScanResult result, List<IntelSurfaceCell> evidence, IntelClassification type) {
		if(result.findings.size() >= IntelScanResult.MAX_FINDINGS) return;
		int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
		int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
		int count = 0;
		for(IntelSurfaceCell cell : evidence) {
			if(cell.classification != type) continue;
			count++;
			minX = Math.min(minX, cell.x); maxX = Math.max(maxX, cell.x);
			minY = Math.min(minY, cell.y); maxY = Math.max(maxY, cell.y);
			minZ = Math.min(minZ, cell.z); maxZ = Math.max(maxZ, cell.z);
		}
		if(count == 0) return;
		IntelFinding finding = new IntelFinding();
		finding.classification = type;
		finding.minX = minX; finding.maxX = maxX;
		finding.minY = minY; finding.maxY = maxY;
		finding.minZ = minZ; finding.maxZ = maxZ;
		finding.confidence = Math.min(0.95F, 0.45F + count * 0.03F);
		finding.machinery = type == IntelClassification.MACHINERY;
		finding.power = type == IntelClassification.POWER;
		finding.communications = type == IntelClassification.COMMUNICATIONS;
		result.findings.add(finding);
	}

	private void visitNeighbor(Map<String, IntelSurfaceCell> cavities, Set<String> visited, ArrayDeque<IntelSurfaceCell> queue, int x, int y, int z) {
		String key = key(x, y, z);
		IntelSurfaceCell cell = cavities.get(key);
		if(cell != null && visited.add(key)) queue.addLast(cell);
	}

	private String key(int x, int y, int z) {
		return x + ":" + y + ":" + z;
	}
}
