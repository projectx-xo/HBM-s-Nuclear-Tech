package com.hbm.saveddata.satellites.intel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.World;

public class IntelStructuralAnalyzer {

	private final IntelBlockClassifier classifier;

	public IntelStructuralAnalyzer(IntelBlockClassifier classifier) {
		this.classifier = classifier;
	}

	public int process(World world, IntelScanJob job, IntelScanResult result, int budget) {
		if(result.mode != IntelScanMode.COMBINED) return 0;
		List<IntelSurfaceCell> candidates = collectCandidates(result);
		int consumed = 0;
		while(consumed < budget && job.phaseCursor < candidates.size()) {
			IntelSurfaceCell source = candidates.get(job.phaseCursor++);
			consumed++;
			job.processedWork++;
			if(result.structuralCells.size() >= IntelScanResult.MAX_STRUCTURAL_CELLS) continue;
			if(!world.getChunkProvider().chunkExists(source.x >> 4, source.z >> 4)) continue;
			IntelBlockClassifier.BlockIntelProperties props = classifier.properties(world, source.x, source.y, source.z);
			if(!props.constructed && !props.reinforced && !props.machinery) continue;
			result.structuralCells.add(new IntelStructuralCell(source.x, source.y, source.z,
					props.registryId, props.metadata, props.effectiveBlastResistance));
		}
		return consumed;
	}

	public int getCandidateCount(IntelScanResult result) {
		return collectCandidates(result).size();
	}

	private List<IntelSurfaceCell> collectCandidates(IntelScanResult result) {
		List<IntelSurfaceCell> candidates = new ArrayList<IntelSurfaceCell>();
		Set<String> seen = new HashSet<String>();
		for(IntelSurfaceCell cell : result.surfaceCells) addCandidate(candidates, seen, cell);
		for(IntelSurfaceCell cell : result.subsurfaceCells) addCandidate(candidates, seen, cell);
		return candidates;
	}

	private void addCandidate(List<IntelSurfaceCell> candidates, Set<String> seen, IntelSurfaceCell cell) {
		if(!cell.structural || cell.classification == IntelClassification.CAVITY || cell.classification == IntelClassification.NATURAL) return;
		String key = key(cell.x, cell.y, cell.z);
		if(seen.add(key)) candidates.add(cell);
	}

	public IntelStructuralSummary finalizeSummary(IntelScanResult result) {
		IntelStructuralSummary summary = new IntelStructuralSummary();
		if(result.structuralCells.isEmpty()) return summary;

		Map<String, Integer> materials = new HashMap<String, Integer>();
		Set<String> positions = new HashSet<String>();
		double totalResistance = 0D;
		float maxResistance = 0F;
		for(IntelStructuralCell cell : result.structuralCells) {
			Integer count = materials.get(cell.registryId);
			materials.put(cell.registryId, count == null ? 1 : count + 1);
			positions.add(key(cell.x, cell.y, cell.z));
			totalResistance += cell.blastResistance;
			maxResistance = Math.max(maxResistance, cell.blastResistance);
		}

		String dominant = "";
		int dominantCount = -1;
		for(Map.Entry<String, Integer> entry : materials.entrySet()) {
			if(entry.getValue() > dominantCount) {
				dominant = entry.getKey();
				dominantCount = entry.getValue();
			}
		}
		summary.dominantMaterial = dominant;
		summary.averageResistance = (float) (totalResistance / result.structuralCells.size());
		summary.maxResistance = maxResistance;

		List<Integer> wallRuns = new ArrayList<Integer>();
		List<Integer> verticalRuns = new ArrayList<Integer>();
		List<Float> shellScores = new ArrayList<Float>();
		for(IntelStructuralCell cell : result.structuralCells) {
			int runX = contiguousRun(positions, cell.x, cell.y, cell.z, 1, 0, 0);
			int runZ = contiguousRun(positions, cell.x, cell.y, cell.z, 0, 0, 1);
			int runY = contiguousRun(positions, cell.x, cell.y, cell.z, 0, 1, 0);
			int wall = Math.max(1, Math.min(runX, runZ));
			wallRuns.add(wall);
			verticalRuns.add(Math.max(1, runY));
			shellScores.add(cell.blastResistance * Math.max(1, Math.min(wall, runY)));
		}
		summary.wallThickness = medianInt(wallRuns);
		summary.roofThickness = medianInt(verticalRuns);
		summary.floorThickness = summary.roofThickness;

		Collections.sort(shellScores);
		float medianScore = shellScores.get(shellScores.size() / 2);
		int weak = 0;
		for(Float score : shellScores) if(score < medianScore * 0.75F) weak++;
		summary.weakPointCount = weak;
		return summary;
	}

	private int contiguousRun(Set<String> positions, int x, int y, int z, int dx, int dy, int dz) {
		int count = 1;
		for(int sign : new int[] {-1, 1}) {
			for(int step = 1; step <= 16; step++) {
				if(!positions.contains(key(x + dx * step * sign, y + dy * step * sign, z + dz * step * sign))) break;
				count++;
			}
		}
		return count;
	}

	private int medianInt(List<Integer> values) {
		if(values.isEmpty()) return 0;
		Collections.sort(values);
		return values.get(values.size() / 2);
	}

	private String key(int x, int y, int z) {
		return x + ":" + y + ":" + z;
	}
}
