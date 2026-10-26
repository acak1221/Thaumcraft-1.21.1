#!/usr/bin/env python3
"""Read the original TC4 4.2.3.5 bytecode for scripts/porting/extract_legacy_ids.py.

The original JAR is read, never changed: its ``thaumcraft/`` classes are copied to a temporary
directory and disassembled with ``javap -c -l -p``; nothing of it is written into the repository.

Four facts are taken from the bytecode:

* the public static Item/Block fields of ``ConfigItems`` and ``ConfigBlocks``, the class each one is
  built from, its unlocalized name and its ``GameRegistry`` name (``fields``);
* the metadata values a class offers in ``getSubItems`` / ``getSubBlocks`` (``subtypes``). They are
  found by running the method on a small interpreter: integer locals and constants are concrete, the
  ``Config.found*`` switches of the other-mod metals are explored both ways and recorded as the
  condition of a value, and anything else (iterators over registries, NBT) makes the value dynamic;
* every ``new ItemStack(ConfigItems.x / ConfigBlocks.x, n, meta)`` with a constant metadata in any
  original class (``usages``);
* every world placement ``setBlock(..., ConfigBlocks.x, meta, ...)`` with a constant metadata in any
  original class (``world_usages``).

Line numbers are the LineNumberTable entries of the original class files, the lines of the original
source; they are not the lines of a decompilation.
"""
from __future__ import annotations

import re
import subprocess
import tempfile
import zipfile
from dataclasses import dataclass, field
from pathlib import Path

INSTRUCTION = re.compile(r"^\s+(\d+): (\w+)\s*(.*?)(?:\s*// (.*))?$")
LINE = re.compile(r"^\s+line (\d+): (\d+)$")
ITEM = "net/minecraft/item/Item"
BLOCK = "net/minecraft/block/Block"
STACK = "net/minecraft/item/ItemStack"
CONFIG_ITEMS = "thaumcraft/common/config/ConfigItems"
CONFIG_BLOCKS = "thaumcraft/common/config/ConfigBlocks"
# SRG names of the 1.7.10 members this reader needs (MCP stable 12-1.7.10).
GET_SUB_ITEMS = "func_150895_a"      # Item.getSubItems(Item, CreativeTabs, List)
GET_SUB_BLOCKS = "func_149666_a"     # Block.getSubBlocks(Item, CreativeTabs, List)
SET_ITEM_NAME = "func_77655_b"       # Item.setUnlocalizedName(String)
SET_BLOCK_NAME = "func_149663_c"     # Block.setBlockName(String)
ITEM_OF_BLOCK = "func_150898_a"      # Item.getItemFromBlock(Block)


@dataclass
class Method:
    signature: str
    code: list[tuple[int, str, str, str]]
    lines: list[tuple[int, int]]

    def line_of(self, offset: int) -> int:
        return max((line for start, line in self.lines if start <= offset), default=0)


@dataclass
class ClassFile:
    name: str
    superclass: str | None
    header: str
    methods: dict[str, Method] = field(default_factory=dict)

    def method(self, srg: str, descriptor_hint: str = "") -> Method | None:
        for signature, method in self.methods.items():
            if re.search(r"\b" + re.escape(srg) + r"\(", signature) and descriptor_hint in signature:
                return method
        return None


def disassemble(jar: Path) -> dict[str, ClassFile]:
    """Every original thaumcraft class, disassembled in memory."""
    with zipfile.ZipFile(jar) as archive, tempfile.TemporaryDirectory() as temporary:
        root = Path(temporary)
        names = sorted(n for n in archive.namelist() if n.startswith("thaumcraft/") and n.endswith(".class"))
        for name in names:
            target = root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
        classes: dict[str, ClassFile] = {}
        binary = [n[:-6] for n in names]
        for start in range(0, len(binary), 200):
            chunk = binary[start:start + 200]
            text = subprocess.run(["javap", "-c", "-l", "-p", "-classpath", str(root)]
                                  + [n.replace("/", ".") for n in chunk],
                                  capture_output=True, text=True, check=True).stdout
            parts = [p for p in re.split(r"(?m)^(?=Compiled from )", text) if p.strip()]
            if len(parts) != len(chunk):
                raise SystemExit(f"javap returned {len(parts)} classes for {len(chunk)}")
            for name, part in zip(chunk, parts):
                classes[name] = parse(name, part)
        return classes


def parse(name: str, text: str) -> ClassFile:
    lines = text.split("\n")
    header = next((line for line in lines if re.search(r"\b(class|interface) ", line)), "")
    extends = re.search(r"\bextends ([\w.$]+)", header)
    result = ClassFile(name, extends.group(1).replace(".", "/") if extends else None, header)
    current: Method | None = None
    for line in lines:
        if line.startswith("  ") and not line.startswith("   ") and line.rstrip().endswith(";"):
            current = Method(line.strip(), [], [])
            result.methods[current.signature] = current
            continue
        if current is None:
            continue
        match = INSTRUCTION.match(line)
        if match and not line.strip().startswith("line "):
            current.code.append((int(match.group(1)), match.group(2), match.group(3), match.group(4) or ""))
            continue
        match = LINE.match(line)
        if match:
            current.lines.append((int(match.group(2)), int(match.group(1))))
    for method in result.methods.values():
        method.lines.sort()
    return result


def declared_fields(config: ClassFile) -> list[tuple[str, str]]:
    """The public static Item/Block fields, in declaration order."""
    found = []
    for signature in config.methods:
        match = re.match(r"public static (net\.minecraft\.(?:item\.Item|block\.Block)) (\w+);$", signature)
        if match:
            found.append((match.group(2), "item" if match.group(1).endswith("Item") else "block"))
    return found


@dataclass
class FieldInfo:
    field: str
    kind: str
    owner: str
    cls: str | None = None
    unlocalized: str | None = None
    registry_name: str | None = None
    item_block: str | None = None
    line: int = 0


def fields(classes: dict[str, ClassFile]) -> dict[str, FieldInfo]:
    """Field -> the class it is built from, its unlocalized name and its GameRegistry name."""
    result: dict[str, FieldInfo] = {}
    for owner, kind in ((CONFIG_ITEMS, "item"), (CONFIG_BLOCKS, "block")):
        config = classes[owner]
        for name, declared_kind in declared_fields(config):
            result[name] = FieldInfo(name, declared_kind, owner.split("/")[-1])
        for method in config.methods.values():
            # Construction: new X ... setUnlocalizedName("name") ... putstatic field.
            first_new, name = None, None
            strings: list[str] = []
            for offset, op, _arg, comment in method.code:
                if op == "new" and first_new is None and "class thaumcraft/" in comment:
                    first_new = comment.split("class ")[-1]
                elif op.startswith("ldc") and comment.startswith("String "):
                    strings.append(comment[7:])
                elif op == "invokevirtual" and (SET_ITEM_NAME in comment or SET_BLOCK_NAME in comment):
                    name = strings[-1] if strings else None
                elif op == "pop":
                    # The end of a statement that assigns no field (a registration, a fluid container).
                    first_new, name, strings = None, None, []
                elif op == "putstatic":
                    match = re.match(r"Field (\w+):L(\S+);", comment)
                    if match and match.group(1) in result and match.group(2) in (ITEM, BLOCK):
                        info = result[match.group(1)]
                        if info.cls is None and first_new and first_new.startswith("thaumcraft/"):
                            info.cls, info.unlocalized, info.line = first_new, name, method.line_of(offset)
                    first_new, name, strings = None, None, []
            # Registration: getstatic field [ldc ItemBlockClass] ldc "name" [ldc "Thaumcraft"] GameRegistry.register*.
            target, item_block, strings = None, None, []
            for offset, op, _arg, comment in method.code:
                if op == "getstatic":
                    match = re.match(r"Field (\w+):L(\S+);", comment)
                    if match and match.group(1) in result and match.group(2) in (ITEM, BLOCK):
                        target, item_block, strings = match.group(1), None, []
                elif op.startswith("ldc") and comment.startswith("class "):
                    item_block = comment.split("class ")[-1]
                elif op.startswith("ldc") and comment.startswith("String "):
                    strings.append(comment[7:])
                elif op == "invokestatic" and "GameRegistry.register" in comment:
                    if target is not None:
                        registered = [s for s in strings if s != "Thaumcraft"]
                        if registered and result[target].registry_name is None:
                            result[target].registry_name = registered[0]
                        if item_block and result[target].item_block is None:
                            result[target].item_block = item_block
                    target, item_block, strings = None, None, []
    return result


# ------------------------------------------------------------------ interpreter

@dataclass(frozen=True)
class Value:
    kind: str           # int, this, item, stack, flag, null, obj, unknown
    value: object = None


UNKNOWN = Value("unknown")


@dataclass
class SubType:
    meta: int | None    # None = computed at run time (dynamic)
    condition: tuple[str, ...]
    line: int


def _descriptor_args(descriptor: str) -> list[str]:
    inner = re.match(r"\((.*)\)", descriptor).group(1)
    return re.findall(r"\[*(?:L[^;]+;|[ZBCSIJFD])", inner)


def _returns(descriptor: str) -> str:
    return descriptor.rsplit(")", 1)[1]


def run_subtypes(method: Method, self_field: str | None = None, max_paths: int = 512,
                 max_steps: int = 20000) -> list[SubType]:
    """The metadata a getSubItems/getSubBlocks adds to its list, over every explored path.

    A value is ``None`` when it is computed at run time (a loop bounded by a registry or an
    array the interpreter does not read). ``condition`` holds the ``Config.found*`` switches
    that every path adding the value had to pass switched on.
    """
    code = method.code
    index_of = {offset: i for i, (offset, *_rest) in enumerate(code)}
    found: dict[int | None, tuple[set[str], int]] = {}
    # A path: pc index, locals, operand stack, switch conditions, dynamic flag, visit counts.
    work = [(0, {0: Value("this"), 1: Value("item"), 3: Value("list")}, [], (), False, {})]
    paths = 0

    def fork(target, local, stack, conditions, dynamic, visits):
        work.append((target, dict(local), list(stack), conditions, dynamic, dict(visits)))

    while work and paths < max_paths:
        pc, local, stack, conditions, dynamic, visits = work.pop()
        paths += 1
        steps = 0
        while 0 <= pc < len(code) and steps < max_steps:
            steps += 1
            offset, op, arg, comment = code[pc]
            visits[pc] = visits.get(pc, 0) + 1
            if visits[pc] > 300:
                break
            nxt = pc + 1
            if op.startswith("iconst_"):
                stack.append(Value("int", -1 if op == "iconst_m1" else int(op[-1])))
            elif op in ("bipush", "sipush"):
                stack.append(Value("int", int(arg)))
            elif op.startswith("ldc"):
                number = re.match(r"int (-?\d+)", comment)
                stack.append(Value("int", int(number.group(1))) if number else Value("obj", comment))
            elif op == "aconst_null":
                stack.append(Value("null"))
            elif op in ("iload", "aload") or re.fullmatch(r"[ia]load_\d", op):
                slot = int(op.split("_")[1]) if "_" in op else int(arg)
                stack.append(local.get(slot, UNKNOWN))
            elif op.startswith(("istore", "astore")):
                slot = int(op.split("_")[1]) if "_" in op else int(arg)
                local[slot] = stack.pop() if stack else UNKNOWN
            elif op == "iinc":
                slot, delta = (int(x) for x in arg.split(","))
                current = local.get(slot, UNKNOWN)
                local[slot] = Value("int", current.value + delta) if current.kind == "int" else UNKNOWN
            elif op == "dup":
                stack.append(stack[-1] if stack else UNKNOWN)
            elif op == "pop":
                if stack:
                    stack.pop()
            elif op == "new":
                stack.append(Value("new", comment.split("class ")[-1]))
            elif op == "checkcast":
                pass
            elif op == "getstatic":
                match = re.match(r"Field (\S+?)\.(\w+):", comment)
                owner, name = (match.group(1), match.group(2)) if match else ("", "")
                if owner.endswith("config/Config") and name.startswith("found"):
                    stack.append(Value("flag", name))
                elif owner in (CONFIG_ITEMS, CONFIG_BLOCKS):
                    stack.append(Value("field", name))
                else:
                    stack.append(UNKNOWN)
            elif op in ("ifeq", "ifne"):
                value = stack.pop() if stack else UNKNOWN
                target = index_of[int(arg)]
                if value.kind == "int":
                    jump = (value.value == 0) == (op == "ifeq")
                    nxt = target if jump else nxt
                elif value.kind == "flag":
                    # ifeq jumps when the switch is off.
                    on, off = (nxt, target) if op == "ifeq" else (target, nxt)
                    fork(off, local, stack, conditions + (f"!{value.value}",), dynamic, visits)
                    conditions = conditions + (value.value,)
                    nxt = on
                else:
                    fork(target, local, stack, conditions, dynamic, visits)
            elif op.startswith("if_icmp"):
                right = stack.pop() if stack else UNKNOWN
                left = stack.pop() if stack else UNKNOWN
                target = index_of[int(arg)]
                if left.kind == "int" and right.kind == "int":
                    a, b = left.value, right.value
                    taken = {"eq": a == b, "ne": a != b, "lt": a < b, "ge": a >= b, "gt": a > b, "le": a <= b}[op[7:]]
                    nxt = target if taken else nxt
                else:
                    # A loop bound the interpreter cannot read: what follows is computed at run time.
                    fork(target, local, stack, conditions, True, visits)
                    dynamic = True
            elif op.startswith("if_acmp"):
                right = stack.pop() if stack else UNKNOWN
                left = stack.pop() if stack else UNKNOWN
                target = index_of[int(arg)]
                pair = {left.kind, right.kind}
                if pair == {"item", "itemof"} and self_field is not None:
                    other = right if right.kind == "itemof" else left
                    same = other.value == self_field
                    nxt = target if same == (op == "if_acmpeq") else nxt
                else:
                    fork(target, local, stack, conditions, dynamic, visits)
            elif op.startswith("if"):  # ifnull, ifnonnull, iflt ... on unknowns
                if stack:
                    stack.pop()
                fork(index_of[int(arg)], local, stack, conditions, dynamic, visits)
            elif op == "goto":
                nxt = index_of[int(arg)]
            elif op in ("return", "areturn", "ireturn", "athrow"):
                break
            elif op in ("invokespecial", "invokevirtual", "invokestatic", "invokeinterface"):
                descriptor = re.search(r":(\(.*)$", comment).group(1)
                args = [stack.pop() if stack else UNKNOWN for _ in _descriptor_args(descriptor)][::-1]
                receiver = None
                if op != "invokestatic":
                    receiver = stack.pop() if stack else UNKNOWN
                if op == "invokespecial" and f'{STACK}."<init>"' in comment:
                    # new ItemStack; dup; <args>; invokespecial -> the dup'd reference is the receiver.
                    item = args[0] if args else UNKNOWN
                    meta = args[2] if len(args) >= 3 else (Value("int", 0) if len(args) <= 2 else UNKNOWN)
                    made = Value("stack", (item, meta))
                    if stack and stack[-1].kind == "new":
                        stack[-1] = made
                    else:
                        stack.append(made)
                elif op == "invokeinterface" and "java/util/List.add" in comment:
                    added = args[0] if args else UNKNOWN
                    meta = None
                    if added.kind == "stack" and not dynamic:
                        _item, value = added.value
                        meta = value.value if value.kind == "int" else None
                    switched_on = {c for c in conditions if not c.startswith("!")}
                    if meta in found:
                        required, line = found[meta]
                        found[meta] = (required & switched_on, line)
                    else:
                        found[meta] = (switched_on, method.line_of(offset))
                    stack.append(UNKNOWN)
                elif ITEM_OF_BLOCK in comment:
                    source = args[0] if args else UNKNOWN
                    stack.append(Value("itemof", source.value if source.kind == "field" else None))
                elif _returns(descriptor) != "V":
                    stack.append(UNKNOWN)
            elif op in ("iadd", "isub", "imul", "idiv", "irem", "iand", "ior", "ishl", "ishr"):
                right = stack.pop() if stack else UNKNOWN
                left = stack.pop() if stack else UNKNOWN
                if left.kind == right.kind == "int":
                    a, b = left.value, right.value
                    value = {"iadd": a + b, "isub": a - b, "imul": a * b, "idiv": int(a / b) if b else 0,
                             "irem": a % b if b else 0, "iand": a & b, "ior": a | b, "ishl": a << b,
                             "ishr": a >> b}[op]
                    stack.append(Value("int", value))
                else:
                    stack.append(UNKNOWN)
            elif op in ("aaload", "iaload", "baload", "arraylength", "getfield", "i2b", "i2s", "l2i", "f2i", "d2i"):
                if op in ("aaload", "iaload", "baload") and len(stack) >= 2:
                    stack.pop(); stack.pop()
                elif stack and op != "getfield":
                    stack.pop()
                elif stack:
                    stack.pop()
                stack.append(UNKNOWN)
            elif op in ("putfield",):
                stack = stack[:-2]
            elif op in ("anewarray", "newarray"):
                if stack:
                    stack.pop()
                stack.append(UNKNOWN)
            elif op in ("aastore", "iastore", "bastore"):
                stack = stack[:-3]
            elif op in ("lookupswitch", "tableswitch"):
                break
            else:
                stack.append(UNKNOWN)
            pc = nxt
    return [SubType(meta, tuple(sorted(required)), line)
            for meta, (required, line) in sorted(found.items(), key=lambda e: (e[0] is None, e[0] or 0))]


def find_method(classes: dict[str, ClassFile], cls: str, srg: str) -> tuple[str, Method] | None:
    """The nearest override in the original class hierarchy."""
    current = cls
    while current in classes:
        method = classes[current].method(srg)
        if method is not None:
            return current, method
        current = classes[current].superclass
    return None


def usages(classes: dict[str, ClassFile]) -> dict[tuple[str, int], list[str]]:
    """(field, meta) -> "Class:line" of every constant new ItemStack(ConfigX.field, n, meta)."""
    found: dict[tuple[str, int], set[str]] = {}
    for name, cls in classes.items():
        short = name.split("/")[-1]
        for method in cls.methods.values():
            code = method.code
            for i, (offset, op, _arg, comment) in enumerate(code):
                if op != "invokespecial" or f'{STACK}."<init>"' not in comment:
                    continue
                descriptor = re.search(r":(\(.*)$", comment).group(1)
                params = _descriptor_args(descriptor)
                if not params or params[0] not in (f"L{ITEM};", f"L{BLOCK};"):
                    continue
                # Walk back over the constant arguments to the field load.
                consts = []
                j = i - 1
                while j >= 0 and len(consts) < len(params) - 1:
                    _o, prev, parg, pcom = code[j]
                    if prev.startswith("iconst_"):
                        consts.append(-1 if prev == "iconst_m1" else int(prev[-1]))
                    elif prev in ("bipush", "sipush"):
                        consts.append(int(parg))
                    elif prev.startswith("ldc") and re.match(r"int -?\d+", pcom):
                        consts.append(int(pcom.split()[1]))
                    else:
                        break
                    j -= 1
                if len(consts) != len(params) - 1 or j < 0:
                    continue
                _o, prev, _parg, pcom = code[j]
                match = re.match(r"Field (\S+?)\.(\w+):", pcom) if prev == "getstatic" else None
                if not match or match.group(1) not in (CONFIG_ITEMS, CONFIG_BLOCKS):
                    continue
                consts.reverse()
                meta = consts[1] if len(params) == 3 else 0
                found.setdefault((match.group(2), meta), set()).add(f"{short}:{method.line_of(offset)}")
    return {key: sorted(refs, key=_ref_order) for key, refs in found.items()}


# World.setBlock(x, y, z, block, meta, flags), WorldGenerator.setBlockAndNotifyAdequately(world, x, y, z,
# block, meta) and StructureComponent.placeBlockAtCurrentPosition(world, block, meta, x, y, z, box).
SET_BLOCK_META = "func_147465_d"
SET_BLOCK_GEN = "func_150516_a"
SET_BLOCK_STRUCTURE = "func_151550_a"
SET_BLOCK_PLAIN = "func_147449_b"    # World.setBlock(x, y, z, block): metadata 0


def _constant(op: str, arg: str, comment: str) -> int | None:
    if op.startswith("iconst_"):
        return -1 if op == "iconst_m1" else int(op[-1])
    if op in ("bipush", "sipush"):
        return int(arg)
    if op.startswith("ldc") and re.match(r"int -?\d+", comment):
        return int(comment.split()[1])
    return None


def world_usages(classes: dict[str, ClassFile]) -> dict[tuple[str, int], list[str]]:
    """(block field, meta) -> "Class:line" of every original world placement with a constant metadata."""
    found: dict[tuple[str, int], set[str]] = {}
    for name, cls in classes.items():
        short = name.split("/")[-1]
        for method in cls.methods.values():
            code = method.code
            for i, (offset, op, _arg, comment) in enumerate(code):
                if op != "getstatic":
                    continue
                match = re.match(rf"Field {CONFIG_BLOCKS}\.(\w+):L{BLOCK};", comment)
                if not match:
                    continue
                window = code[i + 1:i + 14]
                if window and window[0][1] == "invokevirtual" and SET_BLOCK_PLAIN in window[0][3]:
                    found.setdefault((match.group(1), 0), set()).add(f"{short}:{method.line_of(offset)}")
                    continue
                meta = _constant(*window[0][1:]) if window else None
                if meta is None:
                    continue
                for _o, wop, _warg, wcomment in window[1:]:
                    if wop == "getstatic" and (CONFIG_BLOCKS in wcomment or CONFIG_ITEMS in wcomment):
                        break
                    if wop.startswith("invoke") and any(s in wcomment for s in (SET_BLOCK_META, SET_BLOCK_GEN, SET_BLOCK_STRUCTURE)):
                        found.setdefault((match.group(1), meta), set()).add(f"{short}:{method.line_of(offset)}")
                        break
                    if wop.startswith("invoke"):
                        break
    return {key: sorted(refs, key=_ref_order) for key, refs in found.items()}


def _ref_order(ref: str) -> tuple[str, int]:
    name, _, line = ref.rpartition(":")
    return name, int(line)
