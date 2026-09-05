# American-themed size 20 missile

The set adds a **Size 20 Thermonuclear Bunker Buster Warhead** and American-themed variants of the size 20 solid-fuel fuselage, fins, and solid-fuel thruster. All four parts appear in the missile creative tab. The tab also includes a complete **American Bunker Buster** missile with a guidance chip and the four matching parts.

| Part | Item registry name |
| --- | --- |
| Thermonuclear bunker buster | `hbm:item.mp_warhead_20_buster_thermonuclear_american_flag` |
| Solid-fuel fuselage | `hbm:item.mp_fuselage_20_solid_american_flag` |
| Fins | `hbm:item.mp_stability_20_american_flag` |
| Solid-fuel thruster | `hbm:item.mp_thruster_20_solid_american_flag` |

The new head uses size 20 connections, strength 250, weight 10 t, and 40 HP. The other three parts retain their source parts' gameplay values. The fuselage is size 20 at both ends with 20,000 fuel capacity.

## Penetration and detonation

The head uses the existing bunker-buster traversal and HBM's effective block explosion resistance:

- It penetrates individual breakable blocks rated **100 or less**, along its impact direction, up to **96 blocks of travel**.
- Successfully penetrating resistance **40 or higher** arms the thermonuclear effect.
- After arming, the first interior air cell is the detonation location. HBM's existing strength-250 TX explosion and its visual effect originate there.
- If armed penetration stops at a stronger barrier, unavailable space, failed block removal, or the travel limit, it detonates at the last reachable location.
- If it never arms, it produces the existing small strength-4 conventional impact effect. Ordinary soil and caves alone cannot trigger full thermonuclear yield.

For example, three ordinary ground blocks followed by a 98-rated roof block and interior air result in penetration through all four blocks, followed by a thermonuclear detonation in the interior. Each further roof layer rated at most 100 can also be penetrated. The original conventional bunker-buster and ordinary thermonuclear heads retain their behavior.

## Crafting

Use the assembly machine to combine the existing Minuteman size 20 thermonuclear head, the Minuteman size 20 bunker-buster head, and one each of red, white, and blue dye into the new payload.

The other skins have shapeless crafting recipes: combine the source part with one each of red, white, and blue dye. Sources are the Minuteman size 20 solid-fuel fuselage, Size 20 White Fins, and the normal size 20 solid-fuel thruster.

Assemble the painted parts and a guidance chip in the missile assembly machine.

## Artwork and installation

The set uses the centered Minuteman artwork: a mostly white fuselage with navy Air Force lettering and insignia, white fins, a metallic thruster, and a small US flag on each side of the white payload. The models retain their existing dimensions. Item registry names are preserved, so existing parts and assembled missiles receive the revised appearance when the mod is updated.

Install `tjHBM-NTM-v1.6.jar` on both client and server, replacing the previous HBM JAR. A resource pack alone cannot add the payload behavior or registered parts. Model previews are external renders, not Minecraft screenshots.
