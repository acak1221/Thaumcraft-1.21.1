"""Audited finite runtime dependencies; public addon inputs are not finite.

The manifest is reviewed, never generated from the set of existing assets.
Changed supplier code/bounds or factory calls require expansion review.
"""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

CONTRACT = Path(__file__).with_name("composed-resource-paths.json")
TOKEN = re.compile(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/', re.S)
FACTORY = re.compile(r'\b(?:ResourceLocation\s*\.\s*(?:fromNamespaceAndPath|parse|tryParse|withDefaultNamespace)|new\s+ResourceLocation)\s*\(')
HELPER = re.compile(r'\b(?:VisNetRender\s*\.\s*(?:texture|modelType)|UtilsFX\s*\.\s*(?:getTextureSize|getTextureAnimationSize)|ModelWand\s*\.\s*(?:rodTexture|capTexture)|LegacyBlockQuads\s*\.\s*sprite)\s*\(')


def uncomment(source: str) -> str:
    return TOKEN.sub(lambda m: m[0] if m[0][0] in "\"'" else " " * len(m[0]), source)


def calls(source: str, pattern=FACTORY) -> list[str]:
    """Balanced factory calls, excluding comments and factory-looking strings."""
    source = uncomment(source)
    masked = TOKEN.sub(lambda m: " " * len(m[0]), source)
    result = []
    for match in pattern.finditer(masked):
        end, depth = match.end(), 1
        while depth and end < len(masked):
            depth += (masked[end] == '(') - (masked[end] == ')')
            end += 1
        pieces = re.split(r'("(?:\\.|[^"\\])*")', source[match.start():end])
        result.append(''.join(p if i % 2 else re.sub(r'\s+', '', p) for i, p in enumerate(pieces)))
    return result


def dynamic_calls(source: str) -> list[str]:
    literal = r'"(?:\\.|[^"\\])*"'
    return [call for call in calls(source) if not re.fullmatch(
        rf'(?:ResourceLocation\.(?:fromNamespaceAndPath|parse|tryParse|withDefaultNamespace)|newResourceLocation)\({literal}(?:,{literal})?\)', call)]


def source_contract(source: str) -> str:
    """Conservative supplier contract: all non-comment Java code tokens.

    Ignores comments/formatting; unrelated code changes also require review.
    Asset contents are NOT inputs, so removing an asset cannot bless its absence.
    """
    source = uncomment(source)
    tokens = re.findall(r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|\w+|[^\s]', source)
    return hashlib.sha256(json.dumps(tokens).encode()).hexdigest()


def string_values(value):
    if isinstance(value, str):
        yield value
    elif isinstance(value, dict):
        for child in value.values():
            yield from string_values(child)
    elif isinstance(value, list):
        for child in value:
            yield from string_values(child)


def corpus_paths(resources: Path) -> dict[str, set[str]]:
    research = json.loads((resources / 'data/thaumcraft/tc4/research.json').read_text())
    icons = {'thaumcraft:' + path.lower() for text in string_values(research)
             for path in re.findall(r'new ResourceLocation\("thaumcraft",\s*"([^"]+)"\)', text)}
    images = set()
    for path in (resources / 'assets/thaumcraft/lang').glob('*.json'):
        for text in string_values(json.loads(path.read_text())):
            for image in re.findall(r'<IMG>(.*?)</IMG>', text, re.S):
                fields = image.split(':')
                if len(fields) == 7:
                    images.add(':'.join(fields[:2]))
    aspects = json.loads((resources / 'data/thaumcraft/tc4/aspects.json').read_text())
    return {'GuiResearchPopup.java': icons, 'GuiResearchRecipe.java': images,
            'Aspect.java': {'thaumcraft:textures/aspects/' + row['tag'].lower() + '.png' for row in aspects['aspects']}}


def validate(root: Path, resources: Path, sprites: dict[str, Path], contract: dict | None = None) -> dict:
    contract = contract if contract is not None else json.loads(CONTRACT.read_text())
    errors = []
    inventory, helpers = {}, {}
    # compat/*: the optional EMI, JEI and Jade plugins, compiled into both loader modules.
    for module in ('common', 'fabric', 'neoforge', 'compat/emi', 'compat/jade', 'compat/jei'):
        for path in sorted((root / module / 'src/main/java').rglob('*.java')):
            source = path.read_text()
            found = dynamic_calls(source)
            if found:
                inventory[path.relative_to(root).as_posix()] = found
            found_helpers = calls(source, HELPER)
            if found_helpers:
                helpers[path.relative_to(root).as_posix()] = found_helpers
    if inventory != contract['dynamic_factories']:
        errors.append('Dynamic ResourceLocation inventory changed; review composed-resource-paths.json expansions/exclusions')
    if helpers != contract['helper_calls']:
        errors.append('Resource helper callers changed; review composed-resource-paths.json expansions')
    for relative, expected in contract['source_contracts'].items():
        if source_contract((root / relative).read_text()) != expected:
            errors.append(f'Resource supplier changed: {relative}; review composed-resource-paths.json expansions')
    for name, paths in corpus_paths(resources).items():
        declared = next(set(site['paths']) for site in contract['sites'] if site['site'].endswith('/' + name))
        if paths != declared:
            errors.append(f'Resource corpus changed: {name}; review composed-resource-paths.json expansions')
    files, atlas = set(), set()
    for site in contract['sites']:
        for identifier in site['paths']:
            namespace, path = identifier.split(':', 1)
            if not re.fullmatch(r'[a-z0-9._-]+', namespace) or not re.fullmatch(r'[a-z0-9/._-]+', path) or '..' in path.split('/'):
                errors.append(f'Invalid composed resource: {identifier}')
                continue
            if site.get('kind') == 'sprite':
                atlas.add(identifier)
                relative = f'assets/{namespace}/textures/{path}.png'
                if namespace == 'thaumcraft' and identifier not in sprites:
                    errors.append(f'Unstitched composed sprite {identifier} ({site["site"]})')
            else:
                relative = f'assets/{namespace}/{path}'
            if namespace == 'minecraft' and site.get('external'):
                continue
            files.add(relative)
            if not (resources / relative).is_file():
                errors.append(f'Missing composed resource {identifier} ({site["site"]})')
    # Literal mesh/texture/shader requests and classpath streams were another
    # verifier blind spot. Check them directly rather than duplicating a list.
    literal_files = set()
    for path in (root / 'common/src/main/java').rglob('*.java'):
        source = uncomment(path.read_text())
        for call in calls(source):
            match = re.fullmatch(r'ResourceLocation\.fromNamespaceAndPath\("thaumcraft","((?:textures|shaders)/[^"\\]+)"\)', call)
            if match:
                literal_files.add('assets/thaumcraft/' + match[1])
        literal_files.update(re.findall(r'getResourceAsStream\("(assets/thaumcraft/[^"\\]+)"\)', source))
    for relative in sorted(literal_files):
        if not (resources / relative).is_file():
            errors.append(f'Missing literal client resource {relative}')
    if errors:
        raise ValueError('Composed resource validation failed:\n' + '\n'.join(errors))
    return {'checked_composed_sites': len(contract['sites']), 'checked_composed_files': len(files),
            'checked_composed_sprites': len(atlas), 'checked_literal_client_files': len(literal_files)}
