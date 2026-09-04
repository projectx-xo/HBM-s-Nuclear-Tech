package com.hbm.saveddata.satellites.intel;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

public class IntelSurfaceScanner {

	private final IntelBlockClassifier classifier;

	public IntelSurfaceScanner(IntelBlockClassifier classifier) {
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

			result.coveredColumns++;
			int y = Math.max(0, world.getHeightValue(x, z) - 1);
			while(y > 0) {
				Block block = world.getBlock(x, y, z);
				if(block != null && block != Blocks.air) break;
				y--;
			}

			IntelClassification classification = classifier.classifySurface(world, x, y, z).forMode(result.mode);
			IntelBlockClassifier.BlockIntelProperties props = classifier.properties(world, x, y, z);
			if(result.surfaceCells.size() < IntelScanResult.MAX_SURFACE_CELLS) {
				result.surfaceCells.add(new IntelSurfaceCell(x, y, z, classification, props.constructed || props.machinery));
			}
			if(classification != IntelClassification.NATURAL) addFinding(result, x, y, z, classification, props);
		}
		return consumed;
	}

	private void addFinding(IntelScanResult result, int x, int y, int z, IntelClassification classification, IntelBlockClassifier.BlockIntelProperties props) {
		for(IntelFinding existing : result.findings) {
			if(existing.classification != classification) continue;
			if(Math.abs(existing.minX - x) <= 4 && Math.abs(existing.minZ - z) <= 4) {
				existing.minX = Math.min(existing.minX, x);
				existing.maxX = Math.max(existing.maxX, x);
				existing.minY = Math.min(existing.minY, y);
				existing.maxY = Math.max(existing.maxY, y);
				existing.minZ = Math.min(existing.minZ, z);
				existing.maxZ = Math.max(existing.maxZ, z);
				mergeEvidence(existing, props, result.mode);
				return;
			}
		}
		if(result.findings.size() >= IntelScanResult.MAX_FINDINGS) return;
		IntelFinding finding = new IntelFinding();
		finding.classification = classification;
		finding.minX = finding.maxX = x;
		finding.minY = finding.maxY = y;
		finding.minZ = finding.maxZ = z;
		finding.confidence = props.machinery || props.launchInfrastructure ? 0.9F : props.reinforced ? 0.75F : 0.6F;
		mergeEvidence(finding, props, result.mode);
		result.findings.add(finding);
	}

	private void mergeEvidence(IntelFinding finding, IntelBlockClassifier.BlockIntelProperties props, IntelScanMode mode) {
		finding.reinforced |= props.reinforced;
		finding.machinery |= props.machinery;
		finding.power |= props.power;
		finding.launchInfrastructure |= mode == IntelScanMode.COMBINED && props.launchInfrastructure;
		finding.communications |= props.communications;
	}
}
