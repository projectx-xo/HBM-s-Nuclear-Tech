package com.hbm.saveddata.satellites.intel;

public class IntelTargetScanner {
	public interface TargetAccess {
		boolean isChunkLoaded(int x, int z);
		Iterable<IntelFinding> targetsInChunk(int x, int z);
	}

	public int process(TargetAccess access, IntelScanJob job, IntelScanResult result, int budget) {
		if(job.mode != IntelScanMode.COMBINED || result.mode != IntelScanMode.COMBINED) return 0;
		int minX = result.targetX - result.width / 2, minZ = result.targetZ - result.depth / 2;
		int chunksX = ((minX + result.width - 1) >> 4) - (minX >> 4) + 1;
		int consumed = 0;
		while(consumed < budget && job.phaseCursor < getChunkCount(result)) {
			int index = job.phaseCursor++;
			int chunkX = (minX >> 4) + index % chunksX;
			int chunkZ = (minZ >> 4) + index / chunksX;
			consumed++;
			job.processedWork++;
			if(!access.isChunkLoaded(chunkX, chunkZ)) continue;
			for(IntelFinding finding : access.targetsInChunk(chunkX, chunkZ)) {
				if(finding.minX < minX || finding.minX >= minX + result.width
						|| finding.minZ < minZ || finding.minZ >= minZ + result.depth) continue;
				if(finding.sourceEntityId != null && !job.seenMissiles.add(finding.sourceEntityId)) continue;
				if(result.findings.size() >= IntelScanResult.MAX_FINDINGS) {
					// Do not let earlier terrain/cavity findings hide explicit targets.
					for(int i = result.findings.size() - 1; i >= 0; i--) {
						if(result.findings.get(i).targetType.isEmpty()) {
							result.findings.remove(i);
							break;
						}
					}
				}
				if(result.findings.size() < IntelScanResult.MAX_FINDINGS) result.findings.add(0, finding);
			}
		}
		return consumed;
	}

	public int getChunkCount(IntelScanResult result) {
		int minX = result.targetX - result.width / 2, minZ = result.targetZ - result.depth / 2;
		return (((minX + result.width - 1) >> 4) - (minX >> 4) + 1)
				* (((minZ + result.depth - 1) >> 4) - (minZ >> 4) + 1);
	}
}
