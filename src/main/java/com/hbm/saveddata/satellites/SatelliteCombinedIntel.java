package com.hbm.saveddata.satellites;

import java.util.ArrayList;
import java.util.List;

import com.hbm.saveddata.satellites.intel.IntelClassification;
import com.hbm.saveddata.satellites.intel.IntelFinding;
import com.hbm.saveddata.satellites.intel.IntelScanMode;
import com.hbm.saveddata.satellites.intel.IntelScanResult;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

public class SatelliteCombinedIntel extends SatelliteIntelligenceBase {

	@Override
	public String getType() {
		return "COMBINED_INTEL";
	}

	@Override
	public IntelScanMode getScanMode() {
		return IntelScanMode.COMBINED;
	}

	@Override
	protected void onIntelligenceReady(IntelScanResult result) {
		correlateCombinedFindings(result);
	}

	public void correlateCombinedFindings(IntelScanResult result) {
		if(result == null || result.correlated) return;
		List<IntelFinding> snapshot = new ArrayList<IntelFinding>(result.findings);

		for(IntelFinding underground : snapshot) {
			if(underground.classification != IntelClassification.BUNKER
					&& underground.classification != IntelClassification.POSSIBLE_SILO
					&& underground.classification != IntelClassification.MACHINERY) continue;

			for(IntelFinding surface : snapshot) {
				if(surface == underground || !overlapsXZ(surface, underground)) continue;

				if(surface.classification == IntelClassification.LAUNCH_INFRASTRUCTURE || surface.launchInfrastructure) {
					if(underground.classification == IntelClassification.POSSIBLE_SILO) {
						underground.confidence = Math.min(1F, underground.confidence + 0.20F);
						underground.launchInfrastructure = true;
					} else if(underground.reinforced && result.findings.size() < IntelScanResult.MAX_FINDINGS && !hasOverlap(result, IntelClassification.POSSIBLE_SILO, underground)) {
						IntelFinding silo = copyBounds(underground, IntelClassification.POSSIBLE_SILO);
						silo.confidence = Math.min(1F, underground.confidence + 0.25F);
						silo.launchInfrastructure = true;
						result.findings.add(silo);
					}
				}

				if((surface.classification == IntelClassification.COMMUNICATIONS || surface.communications)
						&& !hasOverlap(result, IntelClassification.COMMUNICATIONS, underground)
						&& result.findings.size() < IntelScanResult.MAX_FINDINGS) {
					IntelFinding comms = copyBounds(underground, IntelClassification.COMMUNICATIONS);
					comms.confidence = Math.min(1F, underground.confidence + 0.15F);
					comms.communications = true;
					comms.machinery = true;
					result.findings.add(comms);
				}

				if((surface.classification == IntelClassification.POWER || surface.power)
						&& !hasOverlap(result, IntelClassification.POWER, underground)
						&& result.findings.size() < IntelScanResult.MAX_FINDINGS) {
					IntelFinding power = copyBounds(underground, IntelClassification.POWER);
					power.confidence = Math.min(1F, underground.confidence + 0.15F);
					power.power = true;
					power.machinery = true;
					result.findings.add(power);
				}
			}
		}
		result.correlated = true;
	}

	private IntelFinding copyBounds(IntelFinding source, IntelClassification classification) {
		IntelFinding copy = new IntelFinding();
		copy.classification = classification;
		copy.minX = source.minX; copy.maxX = source.maxX;
		copy.minY = source.minY; copy.maxY = source.maxY;
		copy.minZ = source.minZ; copy.maxZ = source.maxZ;
		copy.reinforced = source.reinforced;
		copy.machinery = source.machinery;
		copy.power = source.power;
		copy.launchInfrastructure = source.launchInfrastructure;
		copy.communications = source.communications;
		return copy;
	}

	private boolean hasOverlap(IntelScanResult result, IntelClassification classification, IntelFinding bounds) {
		for(IntelFinding finding : result.findings) {
			if(finding.classification == classification && overlapsXZ(finding, bounds)) return true;
		}
		return false;
	}

	private boolean overlapsXZ(IntelFinding a, IntelFinding b) {
		return a.maxX >= b.minX && b.maxX >= a.minX && a.maxZ >= b.minZ && b.maxZ >= a.minZ;
	}

	@Override
	public IChatComponent[] getInfo(World world) {
		return new IChatComponent[] { new ChatComponentText("Combined Intelligence Satellite") };
	}
}
