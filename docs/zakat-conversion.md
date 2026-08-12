# LEST _ZAKAT MAP01 → C3D2

`docs/LEST _ZAKAT.wad` is an author-provided **PWAD** source file. It is not
loaded by Java ME and is not part of the game resource path. The resulting
self-contained C3D2 package is:

```text
res/gamedata/custom/doom-zakat/
res/gamedata/custom/doom-zakat-common/textures/
```

`doom-zakat` is routed as the third Doom map/menu entry (`zakat`) after E1M1
and E1M2. Its custom world BMPs sit in the separate common directory so map
metadata does not copy the texture set into other Doom packages.

## Rebuild command

```bash
python3 scripts/convert_doom_e1m1.py \
    "docs/LEST _ZAKAT.wad" \
    res/gamedata/custom/doom-zakat \
    --map MAP01 \
    --base-wad docs/DOOM.WAD \
    --shared-assets res/gamedata/custom/doom-zakat-common \
    --pvs doom-reject \
    --allow-bsp-mismatches
```

The PWAD has no `PLAYPAL` and refers to a few stock resources, so
`--base-wad docs/DOOM.WAD` is an **editor/converter-only** fallback for the
palette, standard sprites and missing patches. All resulting BMP4 assets are
written to `res/`; the MIDlet does not need either WAD at runtime.

## What is imported

- classic binary `MAP01`: 2,534 source vertices, 2,365 linedefs, 4,325
  sidedefs, 438 sectors and 151 Things;
- 50 custom wall textures, 22 custom flats, custom sky and 64 world sprite
  materials, quantized to Java ME BMP4;
- all 135 visible world Things (the 15 deathmatch-start markers are not world
  props);
- player spawn, pickups, props, native Doom weapons/HUD and fixed-point/BSP
  geometry;
- `doom-reject` PVS for this very large outdoor map, avoiding an all-visible
  traversal over 1,862 C3D BSP leaves on a small Java ME heap.

## Zandronum note and known geometry limit

Although the mod is commonly launched through Zandronum, MAP01 itself uses the
classic Doom binary map lump layout and does not contain ACS/UDMF data or
linedef specials that require a Zandronum runtime feature for the initial
walkable import.

It does contain several overlapping/stacked sector samples. The deterministic
C3D BSP validation reports **12 local mismatches** in this 438-sector map. The
converter writes this map only with `--allow-bsp-mismatches`; the count is
recorded in `doom_conversion.json` rather than being hidden. Thirteen
zero-height, untagged structural sectors are retained as closed geometry rather
than being falsely expanded into 64-unit portal rooms. This removes the large
sky slivers/false openings caused by treating ZDoom control sectors as walkable
space. It remains a compatibility bridge rather than a claim that the local
overlapping areas implement full 3D floors. Test MAP01 in KEmulator and send a
screenshot/location if one of those areas is visibly wrong; a true
stacked-sector/3D-floor layer should then be implemented from actual evidence
rather than globally slowing the renderer.

The mod's `SKY1` texture expects the commercial `RSKY1` patch, which is absent
from the supplied base WAD. The converter uses the PWAD's authored direct
`SKY1` patch with a brown RSKY-style tint and records that fallback in
`doom_conversion.json`.
