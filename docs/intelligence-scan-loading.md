# Remote intelligence scan chunk loading (v1.18)

Intelligence scans temporarily hold the target 64 × 64 block footprint using a Forge chunk ticket. The footprint covers 16–25 chunks depending on alignment, including negative coordinates. One chunk is loaded per tick before any scan sampling starts. Previously ungenerated terrain may be generated. A player does not need to visit the target.

The ticket remains held through terrain, target and projection capture. Completion, scan failure, timeout, and administrative satellite removal release it. Interrupted scan tickets restored after restart are discarded rather than resumed. Forge also owns ticket cleanup on world unload. Scans have a 6000-tick limit; unavailable tickets or insufficient configured ticket depth fail the scan instead of silently evicting footprint chunks. Existing scanner work budgets remain unchanged.

The source dimension and satellite/controller still need to be active to process the scan; this does not start unloaded dimensions. Projection tables display the captured snapshot after target chunks are released. Normal world/player chunk-loading rules may keep chunks loaded independently.

Validation: footprint alignment/negative coordinates, one-chunk-per-tick pacing, retained tickets and failure/idempotent cleanup tests; Gradle build. Live remote scans on client and dedicated server, including restart and ticket exhaustion, remain to be verified.
