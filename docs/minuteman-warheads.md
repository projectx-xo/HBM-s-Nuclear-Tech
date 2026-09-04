# Minuteman III missile parts

The Minuteman III set adds a white solid-fuel fuselage with US Air Force markings and three matching warheads with small US flags. The new fuselage has **size 20 connections at both ends**. Use an existing size 20 solid-fuel thruster, a guidance chip, and one of the new size 20 heads in the missile assembly machine. All four new parts have assembly-machine recipes and appear in the missiles creative tab.

| Part | Item registry name | Gameplay values |
| --- | --- | --- |
| Solid-fuel fuselage | `hbm:item.mp_fuselage_20_solid_minuteman` | Size 20/20, 20,000 fuel capacity, 70 HP |
| Bunker buster | `hbm:item.mp_warhead_20_buster_minuteman` | Size 20, strength 20 when armed, 10 t, 40 HP |
| HE | `hbm:item.mp_warhead_20_he_minuteman` | Size 20, strength 75, 7.5 t, 25 HP |
| Thermonuclear | `hbm:item.mp_warhead_20_thermonuclear_minuteman` | Size 20, existing TX effect at strength 250, 10 t, 35 HP |

Strength values are the mod's gameplay parameters. The thermonuclear head uses the existing TX handler; the HE head uses the existing HE handler. Both detonate on impact.

## Bunker-buster behavior

The new bunker buster follows its impact direction through up to **96 blocks of travel**. Penetration checks the effective explosion resistance displayed by HBM's block-information tooltip, using `block.getExplosionResistance(null)`.

- Each block with resistance **100 or less** can be penetrated. Multiple 100-rated layers do not consume a shared resistance budget.
- The full-strength fuze arms after successfully penetrating a block with resistance **40 or more**. This is the fork's heavy-resistance threshold.
- Dirt, ordinary stone, and caves do not arm the full-strength fuze. Once armed, the first air space along the path triggers detonation inside the bunker.
- A block above 100 resistance, an unbreakable block, unavailable world space, a failed block removal, or the travel limit ends penetration. Detonation occurs at the last reachable location.
- If it never arms, it produces a small strength-4 impact explosion. If it arms but cannot reach an interior air space, it detonates at full strength where penetration stopped.

For example: soil → several 100-rated roof blocks → interior air penetrates the roof and detonates in the interior. Soil → natural cave → ordinary stone continues searching for resistant material without arming. An initial 101-rated block stops it with the reduced impact effect.

The older bunker-buster parts retain their existing behavior. The earlier Minuteman size 15/20 skin remains a separate part and takes size 15 heads.

## Installation and verification

These are new registered parts and gameplay code, so they require the updated mod JAR on both the client and server. A resource pack alone cannot add them. Replace the previous HBM JAR with the supplied build rather than loading two copies. Existing fork dependencies are unchanged.

Build from the fork with Java 8: `./gradlew build --no-daemon`. The JAR is written to `build/libs/`.

The Java 8 build and 12 penetration tests passed. Tests cover layered resistance, arming, cavities, barriers, failed removals, diagonal traversal, negative coordinates, and the travel limit. Models were checked with an external render. A Minecraft client/server playtest has not been run.
