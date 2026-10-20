#!/usr/bin/env python3
"""Extract only TC4 GuiResearchPopup's exact160x32 vanilla1.7.10 toast frame.

Usage: python3 scripts/porting/extract_research_toast.py --minecraft-jar /path/minecraft-1.7.10-client.jar
The original JAR and whole achievement atlas must stay outside the repository.
"""
from argparse import ArgumentParser
from hashlib import sha256
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
from PIL import Image

JAR_SHA256 = "a4fc2284657544e0f4bcc964f927c2fda3e3a205178ed1d5d58883aaf9780cce"
SOURCE = "assets/minecraft/textures/gui/achievement/achievement_background.png"
SOURCE_SHA256 = "b99ec87194c344874949bcf3dcc40745128800c82def4a75c48b6d38e1694575"
OUTPUT = "common/src/main/resources/assets/thaumcraft/textures/gui/research_toast.png"

def main():
    parser = ArgumentParser(description=__doc__)
    parser.add_argument("--minecraft-jar", required=True, type=Path)
    parser.add_argument("--output", type=Path, default=Path(OUTPUT))
    arguments = parser.parse_args()
    if sha256(arguments.minecraft_jar.read_bytes()).hexdigest() != JAR_SHA256:
        raise ValueError("Unexpected original Minecraft1.7.10 JAR SHA256")
    with ZipFile(arguments.minecraft_jar) as archive:
        source = archive.read(SOURCE)
    if sha256(source).hexdigest() != SOURCE_SHA256:
        raise ValueError("Unexpected original achievement atlas SHA256")
    image = Image.open(BytesIO(source))
    if image.size != (256, 256):
        raise ValueError("Unexpected original atlas dimensions")
    frame = image.crop((96, 202, 256, 234))
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    frame.save(arguments.output, format="PNG", optimize=False, compress_level=6)
    print(f"{arguments.output}:160x32; source={SOURCE}; crop=(96,202,160,32); sha256={sha256(arguments.output.read_bytes()).hexdigest()}")

if __name__ == "__main__":
    main()
