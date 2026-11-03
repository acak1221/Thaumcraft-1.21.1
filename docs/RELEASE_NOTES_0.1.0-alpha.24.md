# Thaumcraft 4 Reload 0.1.0-alpha.24

- Fixed cultist altars failing to spawn their initial ritualists on the solid floor of naturally generated eldritch rings. The old check required air inside the floor block below the actual spawn position, so an armed altar could never start.
- Ritualists now spawn above a supporting floor with clear body space and no liquid. Their four corner positions, zero-eye access, spawn timing, equipment, home, persistence and knight reinforcements are preserved. Killing all ritualists still stops reinforcements.
- The fix applies when existing armed cultist altars tick; generating another world or inserting eyes is not required. Inactive altar variants remain inactive.
- Added a required server regression for the solid floor, blocked head space, persisted spawner flags, four ritualists, knight reinforcements and the stop condition. It also runs in isolation before publication.
- Retains the alpha.23 taint droplet crash fix, matrix goggles, crystal lighting and scanned JEI aspect-source lookup.

Minecraft 1.21.1, Java 21. Separate Fabric and NeoForge jars are provided. The altar defect was reproduced in a native Fabric server before the fix. The focused fixture supplies the same floor and spawn geometry as world generation; it does not establish a full survival acquisition or client presentation acceptance.
