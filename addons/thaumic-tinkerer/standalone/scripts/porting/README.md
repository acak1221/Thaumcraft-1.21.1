# Python tools of the resource and oracle pipeline

`tt_assets.py` (converts the 1.7.10 textures and language files of Thaumic Tinkerer into the 1.21.1 resource tree and writes
`tt_assets_map.json` and `docs/lang-key-families.md`) and `tt_oracle.py` (extracts the research tree and the recipes of the 1.7.10
source into `common/src/gameTest/resources/tt_oracle`), with their unit tests `test_tt_assets.py` and `test_tt_oracle.py`. The documents
in `docs/` name them (`docs/PARITY_REPORT.md`, `docs/DEVIATIONS.md`, `NOTICE.md`).

**Licence: MIT** (the licence of Thaumcraft 4 Reload, `LICENSE-MIT` in the root of this repository). They contain no Thaumic Tinkerer
text; the data they read is the 1.7.10 source (CC BY-NC-SA 3.0), the data they write is part of the add-on and under its licence.

These files are copies of `scripts/porting/` of the Thaumcraft 4 Reload monorepo, made by `tools/export-standalone.sh` there: do not
edit them here. They were written for the monorepo, where the add-on is `addons/thaumic-tinkerer` and the Thaumcraft sources are in the
same tree, and they are not changed for this repository. Run them with `tools/porting.sh` from the root, which builds that layout in a
temporary directory (see the header of the script):

```bash
tools/porting.sh test                                           # unit tests (no network); without TT_1710_SOURCE the tests against the real 1.7.10 checkout are skipped
TT_1710_SOURCE=PATH THAUMCRAFT_DIR=../Thaumcraft-1.21.1 tools/porting.sh test   # all of them: PATH is a checkout of GTNewHorizons/ThaumicTinkerer at the pinned commit (NOTICE.md)
tools/porting.sh tt_assets.py --check --source PATH             # the same checkout, passed with --source
THAUMCRAFT_DIR=../Thaumcraft-1.21.1 tools/porting.sh tt_oracle.py --check --source PATH   # also needs the thaumcraft.api sources of a Thaumcraft checkout
```

The tools hard-code the path of the 1.7.10 checkout on the machine they were written on (`DEFAULT_SOURCE`); `TT_1710_SOURCE` makes `tools/porting.sh`
put the checkout you name there in its temporary copies, so nothing in this repository depends on that path.
