# tjHBM-NTM 1.14 — Nuclear Detection Satellite

Adds a Nuclear Detection Satellite item, stable appended satellite registration, and assembly recipe: one existing emission detector satellite, 16 magnetrons and 16 titanium plates. It reuses the detector item texture. Existing item metadata and satellite IDs retain their meanings.

A connected Satellite Ground Station exposes `nuclearEvents(epoch, cursor)` to OpenComputers for this satellite type. Success returns `true, epoch, nextCursor, rows, lostCount, more`; failure returns `false, reason`. Each of at most eight pipe-separated rows contains `sequence,kind,payload,x,y,z,dimension,tick`, with `?` for unknown altitude. The node should acknowledge consumption before persisting its cursor. A new epoch resets pagination; invalid same-epoch cursors are rejected.

The per-dimension journal retains at most 256 events for 12,000 world ticks, in server memory only. Polling checks at most 256 loaded entities per 20 ticks and shares that budget across ground stations. Nuclear payload classification uses the existing radome classifier without granting ordinary radars new capabilities. Nuclear explosion hooks preserve exact X/Z before legacy detector inaccuracy; radar and accelerator emissions are excluded. No new chunk loading is performed, so unloaded or short-lived missiles are not guaranteed to be observed.

Use STRATCOM 3.8.0 for automatic NUC node enrollment, live coordinate alerts, reliable packet acknowledgment and delayed post-blast scans through an existing Combined Intelligence Satellite. These scans are terrain reconstructions, not photographs or damage calculations.

Java 8 build and JUnit checks passed. Live client, dedicated server, optional OpenComputers absence/presence, satellite launch and projection rendering remain unverified.
