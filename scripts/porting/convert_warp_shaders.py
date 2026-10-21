#!/usr/bin/env python3
"""TC4 4.2.3.5 warp shader assets -> native 1.21.1 post chains and post programs.

The original TC4 4.2.3.5 JAR ships four OptiFine post chains and three of its
own post programs, both in the default namespace:

    assets/minecraft/shaders/post/{blurtc,desaturatetc,hunger,sunscorned}.json
    assets/minecraft/shaders/program/{bloom2,bloomColor,bloomVertexBase,color_convolve2}

Minecraft 1.21.1 has no OptiFine driver.  The native driver is the vanilla
``PostChain``/``EffectInstance`` pair, and that pair has one hard constraint
the previous wave of this port missed:

  * ``EffectInstance`` (``EFFECT_SHADER_PATH = "shaders/program/"``) resolves the
    program description with ``ResourceLocation.withDefaultNamespace(...)`` and
    the vertex/fragment sources with
    ``ResourceLocation.withDefaultNamespace(EFFECT_SHADER_PATH + name + ".vsh/.fsh")``.
    A post program therefore only exists in the DEFAULT namespace.  A chain pass
    named ``minecraft:box_blur`` or ``thaumcraft:bloom2`` is looked up as
    ``minecraft:shaders/program/minecraft:box_blur.json`` and can never be
    found, so such a chain is dropped when it is loaded.
  * vanilla 1.21.1 DOES ship post programs (``blit``, ``box_blur``,
    ``color_convolve``, ``blur``, ``invert``, ...).  The previous wave claimed
    the opposite and closed the whole warp post effect as an impossible gap.

The native layout produced here is therefore:

    assets/thaumcraft/shaders/post/warp_*.json          the four native chains
    assets/minecraft/shaders/program/thaumcraft_*.json  the three original TC4
                                                         programs, ported to GLSL
                                                         150 and renamed so they
                                                         cannot collide with the
                                                         vanilla program names.

The program renames keep the original file names visible:

    bloomColor      -> thaumcraft_bloom_color
    bloom2          -> thaumcraft_bloom2
    bloomVertexBase -> thaumcraft_bloom_vertex_base
    color_convolve2 -> thaumcraft_color_convolve2

The two chains that only use vanilla programs keep the vanilla program names
(``color_convolve``, ``blit``, ``box_blur``).

The source of truth is the original JAR, which is tracked in this repository;
the retained 1.12 reference tree is used only as a fallback.
"""
from __future__ import annotations

import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ORIGINAL_JAR = ROOT / "Thaumcraft-1.7.10-4.2.3.5.jar"
FALLBACK = ROOT / "thaumcraft_src/assets/minecraft/shaders"
POST_TARGET = ROOT / "common/src/main/resources/assets/thaumcraft/shaders/post"
PROGRAM_TARGET = ROOT / "common/src/main/resources/assets/minecraft/shaders/program"
STALE_PROGRAM_DIR = ROOT / "common/src/main/resources/assets/thaumcraft/shaders/program"

# The original post chain name -> the native chain name and the original pass names.
CHAINS = {
    "blurtc": "warp_blurred",
    "desaturatetc": "warp_death_gaze",
    "hunger": "warp_hunger",
    "sunscorned": "warp_sun_scorned",
}
# The original TC4 program names -> the native program names.
PROGRAMS = {
    "bloomColor": "thaumcraft_bloom_color",
    "bloom2": "thaumcraft_bloom2",
    "bloomVertexBase": "thaumcraft_bloom_vertex_base",
    "color_convolve2": "thaumcraft_color_convolve2",
}
# The original chain passes that name a vanilla OptiFine program.  1.21.1 has no
# OptiFine ``blur``; the nearest vanilla program is ``box_blur``, which keeps the
# original Radius and the original two axis pass order.
VANILLA_PASS = {"blur": "box_blur", "color_convolve": "color_convolve", "blit": "blit"}


def read_original(relative: str) -> str | None:
    """Read one original shader asset, the tracked JAR first."""
    if ORIGINAL_JAR.exists():
        with zipfile.ZipFile(ORIGINAL_JAR) as jar:
            try:
                return jar.read("assets/minecraft/shaders/" + relative).decode("utf-8")
            except KeyError:
                return None
    path = FALLBACK / relative
    return path.read_text(encoding="utf-8") if path.exists() else None


def glsl150(text: str, stage: str) -> str:
    """Port the original GLSL 120 source to the GLSL 150 dialect of 1.21.1."""
    text = text.replace("#version 120", "#version 150")
    text = text.replace("texture2D(", "texture(")
    text = text.replace("attribute vec4 Position;", "in vec4 Position;")
    if stage == "vsh":
        text = re.sub(r"\bvarying\b", "out", text)
    else:
        text = re.sub(r"\bvarying\b", "in", text)
        if "gl_FragColor" in text:
            text = text.replace("gl_FragColor", "fragColor")
            if "out vec4 fragColor;" not in text:
                text = text.replace("#version 150", "#version 150\n\nout vec4 fragColor;", 1)
    # The legacy drivers accepted a uniform texture read in a global initializer.
    # Core GLSL requires the reads inside main().
    initializer = re.search(r"vec4 averageColor=\(.*?\)/81\.0;", text, re.S)
    if initializer:
        code = initializer.group()
        text = text[: initializer.start()] + text[initializer.end() :]
        text = text.replace("void main() {", "void main() {\n    " + code, 1)
    return text


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def program_source(name: str) -> tuple[str, str] | None:
    """The native (vertex, fragment) program names of one original program."""
    description = json.loads(read_original(f"program/{name}.json"))
    vertex = PROGRAMS.get(description["vertex"], description["vertex"])
    fragment = PROGRAMS.get(description["fragment"], description["fragment"])
    return vertex, fragment


def main() -> None:
    # The four native chains.
    for original, native in CHAINS.items():
        chain = json.loads(read_original(f"post/{original}.json"))
        for row in chain["passes"]:
            name = row["name"]
            if name in PROGRAMS:
                row["name"] = PROGRAMS[name]
            elif name in VANILLA_PASS:
                row["name"] = VANILLA_PASS[name]
            else:
                raise SystemExit(f"unmapped post program {name!r} of {original}")
        write(POST_TARGET / f"{native}.json", json.dumps(chain, indent=2) + "\n")

    # The three original TC4 programs, ported to GLSL 150.
    for original in ("bloomColor", "bloom2", "color_convolve2"):
        native = PROGRAMS[original]
        description = json.loads(read_original(f"program/{original}.json"))
        vertex, fragment = program_source(original)
        description["vertex"] = vertex
        description["fragment"] = fragment
        if original == "color_convolve2":
            # The vanilla sobel vertex shader of 1.21.1 reads InSize; the original
            # OptiFine one did not, so the original program description had no
            # InSize uniform.  Declaring it keeps the vertex shader well defined.
            names = [uniform["name"] for uniform in description["uniforms"]]
            if "InSize" not in names:
                description["uniforms"].insert(
                    1,
                    {
                        "name": "InSize",
                        "type": "float",
                        "count": 2,
                        "values": [1.0, 1.0],
                    },
                )
        write(PROGRAM_TARGET / f"{native}.json", json.dumps(description, indent=2) + "\n")
        fragment_source = read_original(f"program/{original}.fsh")
        write(PROGRAM_TARGET / f"{native}.fsh", glsl150(fragment_source, "fsh"))

    # The shared vertex program of the two bloom programs.
    vertex_source = read_original("program/bloomVertexBase.vsh")
    write(PROGRAM_TARGET / f"{PROGRAMS['bloomVertexBase']}.vsh", glsl150(vertex_source, "vsh"))

    # The previous wave left programs in the mod namespace, where 1.21.1 can never
    # resolve them.  They are dead files and are removed here.
    if STALE_PROGRAM_DIR.exists():
        for stale in sorted(STALE_PROGRAM_DIR.iterdir()):
            stale.unlink()
        STALE_PROGRAM_DIR.rmdir()


if __name__ == "__main__":
    main()
