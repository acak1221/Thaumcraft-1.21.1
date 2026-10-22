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
# The vanilla 1.7.10 ``blur`` program, embedded rather than read from the client JAR.
#
# The warp chain needs it because TC4's own blurtc.json names the vanilla OptiFine program,
# and the TC4 JAR does not carry it. It is embedded so that the conversion stays
# deterministic and reproducible from this repository alone: a build machine has no
# Mojang client JAR, and a conversion that silently skips an asset when the JAR is absent
# would produce different output on different machines.
#
# Provenance: assets/minecraft/shaders/program/blur.fsh and blur.json of the official
# Mojang 1.7.10 client JAR, SHA1 e80d9b3bf5085002218d4be59e668bac718abbc6.
# Source SHA256: blur.fsh f76a326cbaeeae0b4d5001fbd5d69f381153f8ffa7bc30ece510f0124860c709,
#                blur.json 47745ace34856c757e998393ba03da524abc158cce6c1462f7daa4ea49853d67
MOJANG_1710_BLUR_FSH = """#version 120

uniform sampler2D DiffuseSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec2 BlurDir;
uniform float Radius;

void main() {
    vec4 blurred = vec4(0.0);
    float totalStrength = 0.0;
    for(float r = -Radius; r <= Radius; r += 1.0) {
        float strength = abs(1.0 - r / Radius);
        strength = strength * strength;
        totalStrength = totalStrength + strength;
        blurred = blurred + texture2D(DiffuseSampler, texCoord + oneTexel * r * BlurDir) * strength;
    }
    gl_FragColor = vec4(blurred.rgb / totalStrength, texture2D(DiffuseSampler, texCoord).a);
}
"""

# The vanilla 1.7.10 ``sobel.vsh``, which the original blur.json names as its vertex program.
# Same provenance and the same reason for embedding: SHA256
# 60b7661feb6305f7bb72a7055493b36cf64290f9b43c8207d451fd1ec7214705.
MOJANG_1710_SOBEL_VSH = """#version 120

attribute vec4 Position;

uniform mat4 ProjMat;
uniform vec2 InSize;
uniform vec2 OutSize;

varying vec2 texCoord;
varying vec2 oneTexel;

void main(){
    vec4 outPos = ProjMat * vec4(Position.xy, 0.0, 1.0);
    gl_Position = vec4(outPos.xy, 0.2, 1.0);

    oneTexel = 1.0 / InSize;

    texCoord = Position.xy / OutSize;
    texCoord.y = 1.0 - texCoord.y;
}
"""

MOJANG_1710_BLUR_JSON = {
    "blend": {"func": "add", "srcrgb": "one", "dstrgb": "zero"},
    "vertex": "sobel",
    "fragment": "blur",
    "attributes": ["Position"],
    "samplers": [{"name": "DiffuseSampler"}],
    "uniforms": [
        {"name": "ProjMat", "type": "matrix4x4", "count": 16,
         "values": [1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 1.0]},
        {"name": "InSize", "type": "float", "count": 2, "values": [1.0, 1.0]},
        {"name": "OutSize", "type": "float", "count": 2, "values": [1.0, 1.0]},
        {"name": "BlurDir", "type": "float", "count": 2, "values": [1.0, 1.0]},
        {"name": "Radius", "type": "float", "count": 1, "values": [5.0]},
    ],
}
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
# The original chain passes that name a vanilla OptiFine program.
#
# ``blur`` is NOT replaceable by 1.21.1's ``box_blur``. Mojang's 1.7.10 blur.fsh weights
# a sample by (1 - r/Radius) squared and normalises by the sum of the weights; the modern
# box_blur uses a different kernel. Naming the vanilla program would silently replace the
# original warp effect with a different one, so the original GLSL is ported to GLSL 150
# and shipped as ``thaumcraft_blur`` (see BLUR_PROGRAM below).
VANILLA_PASS = {"blur": "thaumcraft_blur", "color_convolve": "color_convolve", "blit": "blit"}
# The ported 1.7.10 program whose source is not in the TC4 jar (it is a vanilla asset).
BLUR_PROGRAM = "thaumcraft_blur"


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

    # The ported vanilla 1.7.10 blur: the name the chain refers to is not the modern
    # ``box_blur``, whose kernel differs, so the original GLSL is shipped instead.
    blur = dict(MOJANG_1710_BLUR_JSON)
    blur["vertex"] = "thaumcraft_blur_vertex"
    blur["fragment"] = "thaumcraft_blur"
    write(PROGRAM_TARGET / "thaumcraft_blur.json", json.dumps(blur, indent=2) + "\n")
    write(PROGRAM_TARGET / "thaumcraft_blur.fsh", glsl150(MOJANG_1710_BLUR_FSH, "fsh"))
    write(PROGRAM_TARGET / "thaumcraft_blur_vertex.vsh", glsl150(MOJANG_1710_SOBEL_VSH, "vsh"))

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
