# Projectile flight troubleshooting

The radar label "Artillery Shell" can represent an artillery rocket as well as a
shell. These have separate implementations from standard missiles and ABMs.

Forge chunk tickets reserve chunks but do not immediately load them. Each flying
projectile must load its destination chunk before Minecraft reconciles entity
membership after movement. Otherwise `addedToChunk` becomes false and subsequent
ticks skip projectile movement even though the ticket and radar entry still exist.
Keeping only a larger ticket window or overriding `EntityEvent.CanUpdate` does
not repair a projectile that has already lost membership.

The artillery loaders now force and load the destination before releasing the
previous chunk. Missile and ABM loaders perform the same handoff with a 3x3 window.
The target area does not need a player stationed there for this handoff to work.

After replacing the mod JAR, restart Minecraft completely and test a fresh shot.
For a flight smoke test, launch toward terrain outside player view distance, leave
the launcher, and observe continued flight through impact. Test shells, artillery
rockets, missiles, and ABMs on both an integrated and a dedicated server.

`ArtilleryChunkHandoffTest` exercises the actual Forge-patched World tick and
membership gates with the production loaders and repeatedly absent destinations.
It stubs flight physics and terrain, so it does not establish correct targeting,
collision, explosion behavior, or compatibility with a complete modpack.
