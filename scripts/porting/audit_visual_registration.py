#!/usr/bin/env python3
"""Reproduce the bounded source inventory; no Java/Gradle/client execution.

Prints VISUAL_FIXES.md to stdout. Reference closure is a navigation aid, not
proof that a loader invoked a callback or that an inspected frame is correct.
"""
import json
from collections import Counter
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[2]


def code(path):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', path.read_text(), flags=re.S)


def inventory():
    files = [p for mod in ('common', 'fabric', 'neoforge')
             for p in (ROOT / mod / 'src/main/java').rglob('*.java')]
    renderers = {p.stem: p for p in files if '/client/renderers/' in str(p)}
    texts = {p: code(p) for p in files}
    reached, witness = set(), {}
    for p, text in texts.items():
        if p in renderers.values():
            continue
        for name in renderers:
            if re.search(r'\b' + name + r'\b', text):
                reached.add(name)
                witness[name] = p
    while True:
        before = len(reached)
        for name in sorted(reached):
            for candidate in renderers:
                if candidate not in reached and re.search(r'\b' + candidate + r'\b', texts[renderers[name]]):
                    reached.add(candidate)
                    witness[candidate] = renderers[name]
        if len(reached) == before:
            break
    return renderers, reached, witness


UNUSED = {
    'ArcaneFurnaceBakedModel': 'Unused pure texture/geometry rule helper, not a BakedModel implementation; furnace has resource models.',
    'ObsidianTotemBakedModel': 'Unused pure texture-index helper, not a BakedModel implementation; totem has resource models.',
    'ModelCentrifuge': 'Superseded: TileCentrifugeRenderer bakes its own six-box geometry.',
    'ModelManaPod': 'Superseded: TileManaPodRenderer builds its own inner/outer fruit.',
    'ModelTubeValve': 'Superseded: TileTubeRenderer.bakeStem builds the valve geometry.',
    'RenderExplosiveOrb': 'Superseded: ThaumcraftEntityRenderers binds explosive orb to RenderFocusOrb(ctx,true).',
    'RenderFallbackBiped': 'Unused generic fallback; no entity binding requests it.',
    'RenderFallbackLiving': 'Unused generic fallback; no entity binding requests it.',
    'RenderNoop': 'Unused no-op fallback; no entity binding requests it.',
    'RenderWandEntityItem': 'Unused vanilla ItemEntityRenderer subclass; vanilla dropped-item renderer invokes the registered wand item renderer.',
}

ITEM_RENDERERS = {
    'advanced_alchemical_furnace': 'TileAlchemyFurnaceAdvancedRenderer',
    'advanced_node_stabilizer': 'TileNodeStabilizerRenderer (lock2; FIXED)',
    'arcane_bore': 'TileArcaneBoreRenderer', 'arcane_bore_base': 'TileArcaneBoreBaseRenderer',
    'aura_node': 'ItemNodeRenderer', 'banner': 'ItemBannerRenderer',
    'centrifuge': 'TileCentrifugeRenderer', 'essentia_buffer': 'TileTubeBufferRenderer',
    'essentia_crystallizer': 'TileEssentiaCrystalizerRenderer',
    'essentia_filter': 'TileTubeRenderer.renderAdvancedItem',
    'essentia_oneway': 'TileTubeRenderer.renderAdvancedItem',
    'essentia_restrict': 'TileTubeRenderer.renderAdvancedItem',
    'essentia_valve': 'TileTubeRenderer.renderValveItem',
    'hungry_chest': 'synthetic TileChestHungry -> registered BER',
    'jar': 'JarItemRenderer', 'jar_void': 'JarItemRenderer', 'jar_node': 'ItemJarNodeRenderer',
    'node_stabilizer': 'TileNodeStabilizerRenderer (lock1)',
    'node_transducer': 'TileNodeConverterRenderer', 'thaumometer': 'ItemThaumometerRenderer',
    'vis_charge_relay': 'TileMagicWorkbenchChargerRenderer', 'vis_relay': 'TileVisRelayRenderer',
    'staff_shape': 'renderWand (WAND override template, not a separate item)',
    'wand_shape': 'renderWand (WAND override template, not a separate item)',
}


def item_renderer(name):
    return 'TileCrystalRenderer (cluster registration loop)' if name.startswith('crystal_') else ITEM_RENDERERS[name]


def main():
    models = ROOT / 'common/src/main/resources/assets/thaumcraft/models'
    direct, inherited = [], []
    for p in sorted((models / 'item').glob('*.json')):
        q, seen = p, set()
        while q.exists() and q not in seen:
            seen.add(q)
            parent = json.loads(q.read_text()).get('parent', '')
            if parent in ('builtin/entity', 'minecraft:builtin/entity'):
                (direct if q == p else inherited).append(p)
                break
            if not parent.startswith('thaumcraft:'):
                break
            q = models / (parent.split(':')[1] + '.json')
    renderers, reached, witness = inventory()
    assert set(renderers) - reached == set(UNUSED), 'Review newly unreachable/reachable classes'
    assert len(direct) == 30 and len(inherited) == 995
    for loader in ('fabric/src/main/java/thaumcraft/fabric/ThaumcraftFabricClient.java',
                   'neoforge/src/main/java/thaumcraft/neoforge/client/ThaumcraftNeoForgeClient.java'):
        assert re.search(r'register(?:Item)?\([^;]*ADVANCED_NODE_STABILIZER_ITEM[^;]*;', (ROOT / loader).read_text(), re.S)
    renderer = (ROOT / 'common/src/main/java/thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java').read_text()
    assert 'advanced?0xFF3333:0xFFFFFF' in renderer
    assert 'advanced?0xFF4444:0xFFFFFF' in renderer
    assert 'true,tile.isAdvanced()' in renderer and 'false,advanced' in renderer
    obj = (ROOT / 'common/src/main/resources/assets/thaumcraft/textures/models/node_stabilizer.obj').read_text()
    groups, group = Counter(), None
    for line in obj.splitlines():
        if line.startswith('g '):
            group = line.split()[1]
        elif line.startswith('f '):
            groups[group] += 1
    assert groups == {'lock': 28, 'piston': 12}, groups
    print('''# Visual registration fixes — 2026-10-23

## Advanced stabilizer: implemented, not frame-verified

The advanced item now has a renderer on **Fabric and NeoForge**. Both the item
and placed-block paths select lock2. Expected player-visible result: the original
lock body with four pistons and **red piston overlays**, visible in inventory,
hand and dropped-item contexts; the placed active device also has its **red bubble**.
Basic lock1 remains white. No bubble is fabricated for an inactive item.

### Oracle evidence (private 4.2.3.5 oracle, paths relative to its root)

* `thaumcraft/client/renderers/block/BlockStoneDeviceRenderer.java:90-92` renders
  inventory metadata 9 **and 10** via `new TileNodeStabilizer(metadata)`.
* `thaumcraft/common/tiles/TileNodeStabilizer.java:20-25` starts count at zero and
  maps metadata 9 to lock1, otherwise lock2.
* `thaumcraft/client/renderers/tile/TileNodeStabilizerRenderer.java:38-57,66-83`
  loads **one** `node_stabilizer.obj`, draws `lock`, then four rotated `piston`
  parts. There is no separate lock2 mesh. Lines 71-73 tint the overlay only to
  `(1,.2,.2)`; line 95 uses `0xFF4444` for lock2's active bubble.
* The port retains that OBJ (28 lock + 12 piston triangles; four piston draws),
  base texture and overlay texture. The new vertex tint is `0xFF3333`, exactly
  `(1,.2,.2)` at byte precision. Basic base/piston vertices stay white. Bubble
  tint is passed per vertex, so it cannot leak into a later device draw.

Changed: `ModelNodeStabilizer` gains a coloured render overload;
`TileNodeStabilizerRenderer` accepts the advanced stack and uses the tile's
existing `isAdvanced()` for placed devices; both loader bootstraps register
`ADVANCED_NODE_STABILIZER_ITEM`. This fixes the missing callback **and** the
previous basic-only colour path, not a basic-artwork alias.

## Sweep method and limits

Reproduce with `python3 scripts/porting/audit_visual_registration.py`.
The tables include every direct builtin item asset and every modern renderer
source file (including models/rule helpers). Code-reference closure strips
comments, excludes tests and legacy code, and supplies a navigation witness;
**references are not runtime reachability proof**. Registration sites were also
read: Fabric `ThaumcraftFabricClient.onInitializeClient`, NeoForge
`ThaumcraftNeoForgeClient.onRegisterClientExtensions`, common `ClientSetup.init`
and its `Thaumcraft*Client.init` registrations, entity `registerAll`, eldritch
dispatcher delegates, model-loading adapters and client mixins. Both loaders
invoke common setup. NeoForge adapters wrap item callbacks in a vanilla
`BlockEntityWithoutLevelRenderer`; Fabric uses `BuiltinItemRendererRegistry`.

No Gradle, client, Java compilation or visual acceptance run. The assertions in
the inventory script check source routing, colour constants, counts and OBJ
triangle groups only. Existing unrelated suspected findings in VISUAL_FINDINGS
remain open.
''')
    print(f'**Counts:** {len(direct)} direct builtin models; {len(inherited)} inherited models '
          f'({len(direct)+len(inherited)} total). {len(renderers)} modern renderer/helper files: '
          f'{len(reached)} referenced, {len(UNUSED)} unused/superseded. '
          'One missing live item registration fixed on two loaders; no additional missing live registration found.\n')
    print('## Direct builtin item models\n\nPaths: `common/src/main/resources/assets/thaumcraft/models/item/`.\n')
    print('| Asset | Registered? | Loader | Finding / callback |\n|---|---|---|---|')
    for p in direct:
        status = 'Via WAND (template)' if p.stem in ('wand_shape', 'staff_shape') else 'Yes'
        print(f'| `{p.name}` | {status} | Both | {item_renderer(p.stem)} |')
    print('\n### Inherited builtin models\n\n| Asset/group | Count | Registered? | Loader | Finding |\n|---|---:|---|---|---|')
    variants = [p for p in inherited if p.stem.startswith('wand_') and not p.stem.endswith('_shape')]
    print(f'| `wand_*` material/cap/focus variants (excluding shape templates) | {len(variants)} | Via WAND | Both | WAND model overrides share renderWand; not separate registered items. |')
    for p in inherited:
        if p not in variants:
            target = 'WAND / renderWand template' if p.stem.endswith('_shape') else item_renderer(p.stem)
            print(f'| `{p.name}` | 1 | Yes / via owner | Both | {target} |')
    print('\n## Every modern renderer/helper source\n\nPaths below are relative to `common/src/main/java/thaumcraft/client/renderers/`. '
          '“Indirect” means model/helper/delegate, not a missing standalone registry entry.\n')
    print('| Asset/class | Registered / referenced? | Loader | Finding / source witness |\n|---|---|---|---|')
    base = ROOT / 'common/src/main/java/thaumcraft/client/renderers'
    for name, p in sorted(renderers.items(), key=lambda pair: str(pair[1])):
        if name in UNUSED:
            status, finding = 'No — unused', UNUSED[name]
        else:
            status = 'Yes / indirect'
            finding = f'`{witness[name].relative_to(ROOT)}`'
        loader = 'Neither (unused)' if name in UNUSED else 'Both (shared / platform-adapted)'
        print(f'| `{p.relative_to(base)}` | {status} | {loader} | {finding} |')
    print('\n## Retained legacy tree (not loaded by either modern loader)\n\n'
          '`src/main` is retained 1.12 reference, intentionally not compiled. '
          'Its files must not be mistaken for unregistered modern content; none were changed.\n')
    legacy_assets = [p for p in sorted((ROOT / 'src/main/resources').glob('**/models/item/*.json'))
                     if json.loads(p.read_text()).get('parent') in ('builtin/entity', 'minecraft:builtin/entity')]
    legacy_classes = sorted((ROOT / 'src/main/java').glob('**/client/renderers/**/*.java'))
    print(f'{len(legacy_assets)} direct builtin legacy assets; {len(legacy_classes)} legacy renderer/helper files.\n')
    print('| Asset/class | Registered? | Loader | Finding |\n|---|---|---|---|')
    for p in legacy_assets + legacy_classes:
        print(f'| `{p.relative_to(ROOT)}` | N/A | Neither modern loader | Archived reference; outside modern compilation. |')
    print('\nTest-source files under `client/renderers` are not renderer registrations:\n')
    tests = [p for mod in ('common', 'src')
             for p in ((ROOT / 'common/src/test/java') if mod == 'common' else (ROOT / 'src/test/java')).glob('**/client/renderers/**/*.java')]
    print(f'{len(tests)} test files (not included in production-source counts).\n')
    for p in sorted(tests):
        print(f'* `{p.relative_to(ROOT)}` — test only, no loader registration needed.')


if __name__ == '__main__':
    main()
