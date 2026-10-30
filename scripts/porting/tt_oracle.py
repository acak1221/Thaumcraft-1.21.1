#!/usr/bin/env python3
"""Extract the research tree and the recipes of Thaumic Tinkerer 1.7.10 into the parity oracle.

Source (read only): a checkout of https://github.com/GTNewHorizons/ThaumicTinkerer pinned to ``SOURCE_COMMIT``
(CC BY-NC-SA 3.0). The script reads the original Java with a small tokenizer, a parser for the subset of Java
the add-on uses and a symbolic interpreter, so builder chains, helper methods, loops, ``if`` on a configuration
flag or a loaded mod and the instances of the multi-variant classes are understood, not pattern-matched. It
replays what the 1.7.10 mod does at start-up (``TTRegistry`` loads the blocks and items, ``init`` registers
every recipe, ``ResearchHelper.initResearch`` and ``postInit`` register the research) and writes, below
``addons/thaumic-tinkerer/common/src/gameTest/resources/tt_oracle``:

* ``research.json``  every research entry: key, category, aspects (in order), column, row, complexity, parents,
  hidden parents, flags, warp, pages, the enabling condition, the class that registers it and file:line;
* ``recipes.json``   every recipe: kind (arcane shaped, crucible, infusion, crafting table shaped/shapeless),
  name, research key, output (source text and the resolved 1.21.1 id), count, aspects, instability, pattern and
  ingredients, catalyst, enabling condition and file:line;
* ``provenance.json`` the pin, the sha256 of every file that was read, the number of constructions found per
  file, what could not be resolved and the digest that ties the three files together.

An item expression is recorded as source text and, where that is mechanical, resolved to a 1.21.1 id:
``ConfigItems``/``ConfigBlocks`` (field, meta) through ``common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json``,
Thaumic Tinkerer classes through the ``Lib*Names`` constants of the port and the ``getItemName()`` /
``getBlockName()`` of the original, vanilla 1.7.10 names through the explicit table ``VANILLA`` below. What
cannot be resolved is marked ``unresolved`` with the reason, never guessed.

The script fails (exit 2, nothing written) when a research or recipe construction of the original source is not
understood or not reached; it lists them.

Usage:
    tt_oracle.py [--source CHECKOUT] [--check] [--allow-unpinned]

``--check`` recomputes everything and fails (exit 1) when the checked-in output differs; it writes nothing.
"""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import re
import subprocess
import sys
from collections import Counter
from dataclasses import dataclass, field
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SOURCE = Path("/home/user/gtnewhorizons/thaumictinkerer")
SOURCE_REPO = "https://github.com/GTNewHorizons/ThaumicTinkerer"
SOURCE_COMMIT = "28f4def938e80d7a0b6f433c2b84300dfb7c35ce"
LICENSE_NAME = "CC BY-NC-SA 3.0"
GENERATOR = "scripts/porting/tt_oracle.py"

JAVA_ROOT = "src/main/java"
REGISTRY_FILE = "thaumic/tinkerer/common/registry/TTRegistry.java"
OUT_DIR = "addons/thaumic-tinkerer/common/src/gameTest/resources/tt_oracle"
LEGACY_IDS = "common/src/main/resources/data/thaumcraft/tc4/legacy_ids.json"
PORT_LIB = "addons/thaumic-tinkerer/common/src/main/java/thaumic/tinkerer/common/lib"
ASPECT_JAVA = "common/src/main/java/thaumcraft/api/aspects/Aspect.java"
CATEGORY_CONSTANT = ("LibResearch", "CATEGORY_THAUMICTINKERER")


class OracleError(Exception):
    """The source does not extract cleanly; nothing is written."""


class ParseError(OracleError):
    pass


class Unsupported(OracleError):
    """A construct the interpreter does not model; raised instead of guessing."""


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def dumps(document: object) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2) + "\n"


# --------------------------------------------------------------------------- lexer

TOKEN_RE = re.compile(r"""
 (?P<ws>\s+)
|(?P<lc>//[^\n]*)
|(?P<bc>/\*.*?\*/)
|(?P<str>"(?:\\.|[^"\\\n])*")
|(?P<chr>'(?:\\.|[^'\\\n])+')
|(?P<float>(?:\d+\.\d*|\.\d+)(?:[eE][+-]?\d+)?[fFdD]?|\d+[eE][+-]?\d+[fFdD]?|\d+[fFdD])
|(?P<int>0[xX][0-9a-fA-F]+[lL]?|\d+[lL]?)
|(?P<id>[A-Za-z_$][A-Za-z_0-9$]*)
|(?P<op>>>>=|<<=|>>=|>>>|\.\.\.|->|::|==|!=|<=|>=|&&|\|\||\+\+|--|\+=|-=|\*=|/=|%=|&=|\|=|\^=|<<|>>|[{}()\[\];,.<>=!~?:+\-*/&|^%@])
""", re.X | re.S)

_ESCAPES = {"n": "\n", "t": "\t", "r": "\r", "b": "\b", "f": "\f", "0": "\0", '"': '"', "'": "'", "\\": "\\"}


def unescape(body: str) -> str:
    out: list[str] = []
    i = 0
    while i < len(body):
        c = body[i]
        if c != "\\":
            out.append(c)
            i += 1
            continue
        n = body[i + 1]
        if n == "u":
            out.append(chr(int(body[i + 2:i + 6], 16)))
            i += 6
        else:
            out.append(_ESCAPES.get(n, n))
            i += 2
    return "".join(out)


@dataclass(slots=True)
class Tok:
    kind: str   # id int float str chr op eof
    val: str
    line: int
    col: int


def tokenize(text: str, label: str = "?") -> list[Tok]:
    toks: list[Tok] = []
    pos = 0
    line = 1
    line_start = 0
    while pos < len(text):
        m = TOKEN_RE.match(text, pos)
        if m is None:
            raise ParseError(f"{label}:{line}: cannot tokenize {text[pos:pos + 20]!r}")
        kind = m.lastgroup
        chunk = m.group()
        if kind not in ("ws", "lc", "bc"):
            toks.append(Tok(kind, chunk, line, pos - line_start + 1))
        nl = chunk.count("\n")
        if nl:
            line += nl
            line_start = pos + chunk.rfind("\n") + 1
        pos = m.end()
    toks.append(Tok("eof", "", line, 0))
    return toks


# --------------------------------------------------------------------------- AST (tuples; first item is the kind)

PRIMITIVES = {"int", "long", "short", "byte", "char", "boolean", "float", "double", "void"}
MODIFIERS = {"public", "private", "protected", "static", "final", "abstract", "synchronized", "native",
             "transient", "volatile", "default", "strictfp"}
ASSIGN_OPS = {"=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>="}
BINARY_PREC = {"||": 1, "&&": 2, "|": 3, "^": 4, "&": 5, "==": 6, "!=": 6, "<": 7, ">": 7, "<=": 7, ">=": 7,
               "<<": 8, ">>": 8, ">>>": 8, "+": 9, "-": 9, "*": 10, "/": 10, "%": 10}
CAST_FOLLOW_KW = {"new", "this", "super", "true", "false", "null"}


class Parser:
    """Recursive-descent parser for the Java subset used by the add-on's registration code."""

    def __init__(self, toks: list[Tok], label: str):
        self.t = toks
        self.i = 0
        self.label = label

    # ---- helpers
    def err(self, message: str) -> ParseError:
        tok = self.t[min(self.i, len(self.t) - 1)]
        return ParseError(f"{self.label}:{tok.line}: {message} (at {tok.val!r})")

    def peek(self, k: int = 0) -> Tok:
        return self.t[min(self.i + k, len(self.t) - 1)]

    def at(self, val: str, k: int = 0) -> bool:
        tok = self.peek(k)
        return tok.val == val and tok.kind in ("op", "id")

    def accept(self, val: str) -> bool:
        if self.at(val):
            self.i += 1
            return True
        return False

    def expect(self, val: str) -> Tok:
        if not self.at(val):
            raise self.err(f"expected {val!r}")
        tok = self.peek()
        self.i += 1
        return tok

    def ident(self) -> str:
        tok = self.peek()
        if tok.kind != "id":
            raise self.err("expected identifier")
        self.i += 1
        return tok.val

    # ---- types
    def type_at(self, i: int, dims: bool = True):
        """(name, next_index) of a type starting at token ``i``, or None."""
        t = self.t
        if t[i].kind != "id" or t[i].val in ("new", "return", "this", "super", "null", "true", "false"):
            return None
        name = t[i].val
        j = i + 1
        while t[j].val == "." and t[j].kind == "op" and t[j + 1].kind == "id":
            name = t[j + 1].val
            j += 2
        if t[j].val == "<" and t[j].kind == "op":
            depth = 0
            k = j
            while True:
                tok = t[k]
                if tok.kind == "eof":
                    return None
                if tok.val == "<":
                    depth += 1
                elif tok.val == ">":
                    depth -= 1
                elif tok.val == ">>":
                    depth -= 2
                elif tok.val == ">>>":
                    depth -= 3
                elif tok.kind == "id" or tok.val in (",", "?", ".", "[", "]", "&") or tok.val == "extends" or tok.val == "super":
                    pass
                else:
                    return None
                k += 1
                if depth <= 0:
                    break
            j = k
        if dims:
            while t[j].val == "[" and t[j + 1].val == "]":
                name += "[]"
                j += 2
        return name, j

    # ---- expressions
    def expr(self):
        return self.assign()

    def assign(self):
        left = self.ternary()
        tok = self.peek()
        if tok.kind == "op" and tok.val in ASSIGN_OPS:
            self.i += 1
            right = self.assign()
            return ("assign", tok.val, left, right, tok.line)
        return left

    def ternary(self):
        cond = self.binary(1)
        if self.at("?"):
            line = self.peek().line
            self.i += 1
            a = self.ternary()
            self.expect(":")
            b = self.ternary()
            return ("cond", cond, a, b, line)
        return cond

    def binary(self, min_prec: int):
        left = self.unary()
        while True:
            tok = self.peek()
            if tok.kind == "id" and tok.val == "instanceof" and 7 >= min_prec:
                self.i += 1
                ty = self.type_at(self.i)
                if ty is None:
                    raise self.err("expected a type after instanceof")
                self.i = ty[1]
                left = ("instanceof", left, ty[0], tok.line)
                continue
            prec = BINARY_PREC.get(tok.val) if tok.kind == "op" else None
            if prec is None or prec < min_prec:
                return left
            self.i += 1
            right = self.binary(prec + 1)
            left = ("binary", tok.val, left, right, tok.line)

    def is_cast(self) -> bool:
        if not self.at("("):
            return False
        ty = self.type_at(self.i + 1)
        if ty is None:
            return False
        j = ty[1]
        if self.t[j].val != ")" or self.t[j].kind != "op":
            return False
        nxt = self.t[j + 1]
        if ty[0] in PRIMITIVES:
            return nxt.kind != "eof" and not (nxt.kind == "op" and nxt.val in (")", ";", ",", ".", "]", "}"))
        if nxt.kind in ("str", "chr", "int", "float"):
            return True
        if nxt.kind == "id":
            return nxt.val != "instanceof"
        return nxt.kind == "op" and nxt.val in ("(", "!", "~")

    def unary(self):
        tok = self.peek()
        if tok.kind == "op" and tok.val in ("!", "-", "+", "~"):
            self.i += 1
            operand = self.unary()
            if tok.val == "-" and operand[0] == "lit" and isinstance(operand[1], (int, float)) and not isinstance(operand[1], bool):
                return ("lit", -operand[1], tok.line)
            return ("unary", tok.val, operand, tok.line)
        if tok.kind == "op" and tok.val in ("++", "--"):
            self.i += 1
            return ("prefix", tok.val, self.unary(), tok.line)
        if self.is_cast():
            self.i += 1
            ty = self.type_at(self.i)
            self.i = ty[1] + 1
            return ("cast", ty[0], self.unary(), tok.line)
        return self.postfix()

    def args(self) -> list:
        self.expect("(")
        out = []
        if not self.at(")"):
            out.append(self.expr())
            while self.accept(","):
                out.append(self.expr())
        self.expect(")")
        return out

    def postfix(self):
        node = self.primary()
        while True:
            tok = self.peek()
            if tok.kind == "op" and tok.val == ".":
                self.i += 1
                nxt = self.peek()
                if nxt.kind == "id" and nxt.val == "class":
                    self.i += 1
                    node = ("classlit", node, nxt.line)
                    continue
                if nxt.kind == "id" and nxt.val == "new":
                    raise self.err("qualified new is not supported")
                name = self.ident()
                if self.at("("):
                    node = ("call", node, name, self.args(), nxt.line)
                else:
                    node = ("field", node, name, nxt.line)
            elif tok.kind == "op" and tok.val == "[":
                self.i += 1
                index = self.expr()
                self.expect("]")
                node = ("index", node, index, tok.line)
            elif tok.kind == "op" and tok.val in ("++", "--"):
                self.i += 1
                node = ("postfix", tok.val, node, tok.line)
            elif tok.kind == "op" and tok.val == "::":
                raise self.err("method references are not supported")
            else:
                return node

    def skip_balanced(self, open_: str, close: str) -> None:
        depth = 0
        while True:
            tok = self.peek()
            if tok.kind == "eof":
                raise self.err(f"unbalanced {open_}")
            self.i += 1
            if tok.kind == "op" and tok.val == open_:
                depth += 1
            elif tok.kind == "op" and tok.val == close:
                depth -= 1
                if depth == 0:
                    return

    def array_init(self):
        line = self.expect("{").line
        elems = []
        while not self.at("}"):
            elems.append(self.array_init() if self.at("{") else self.expr())
            if not self.accept(","):
                break
        self.expect("}")
        return ("arrayinit", elems, line)

    def lambda_ahead(self) -> bool:
        tok = self.peek()
        if tok.kind == "id" and self.peek(1).val == "->":
            return True
        if tok.val == "(" and tok.kind == "op":
            depth = 0
            k = self.i
            while self.t[k].kind != "eof":
                if self.t[k].val == "(":
                    depth += 1
                elif self.t[k].val == ")":
                    depth -= 1
                    if depth == 0:
                        return self.t[k + 1].val == "->"
                k += 1
        return False

    def primary(self):
        tok = self.peek()
        if self.lambda_ahead():
            raise self.err("lambda expressions are not supported")
        k = tok.kind
        if k == "int":
            self.i += 1
            text = tok.val.rstrip("lL")
            return ("lit", int(text, 16) if text.lower().startswith("0x") else int(text), tok.line)
        if k == "float":
            self.i += 1
            return ("lit", float(tok.val.rstrip("fFdD")), tok.line)
        if k == "str":
            self.i += 1
            return ("lit", unescape(tok.val[1:-1]), tok.line)
        if k == "chr":
            self.i += 1
            return ("char", unescape(tok.val[1:-1]), tok.line)
        if k == "op" and tok.val == "(":
            self.i += 1
            inner = self.expr()
            self.expect(")")
            return ("paren", inner, tok.line)
        if k == "op" and tok.val == "{":
            return self.array_init()
        if k == "id":
            v = tok.val
            if v in ("true", "false"):
                self.i += 1
                return ("lit", v == "true", tok.line)
            if v == "null":
                self.i += 1
                return ("lit", None, tok.line)
            if v == "this":
                self.i += 1
                if self.at("("):
                    return ("call", None, "this", self.args(), tok.line)
                return ("this", tok.line)
            if v == "super":
                self.i += 1
                if self.at("("):
                    return ("call", None, "super", self.args(), tok.line)
                return ("super", tok.line)
            if v == "new":
                return self.new_expr()
            self.i += 1
            if self.at("("):
                return ("call", None, v, self.args(), tok.line)
            return ("name", v, tok.line)
        raise self.err("unexpected token in expression")

    def new_expr(self):
        line = self.expect("new").line
        ty = self.type_at(self.i, dims=False)
        if ty is None:
            raise self.err("expected a type after new")
        name, j = ty
        self.i = j
        if self.at("["):
            dims = []
            extra = 0
            while self.at("["):
                self.i += 1
                if self.at("]"):
                    extra += 1
                else:
                    dims.append(self.expr())
                self.expect("]")
            init = self.array_init() if self.at("{") else None
            return ("newarray", name, dims, init, line)
        args = self.args()
        body = False
        if self.at("{"):
            self.skip_balanced("{", "}")
            body = True
        return ("new", name, args, body, line)

    # ---- statements
    def block(self):
        line = self.expect("{").line
        stmts = []
        while not self.at("}"):
            if self.peek().kind == "eof":
                raise self.err("unterminated block")
            stmts.append(self.stmt())
        self.expect("}")
        return ("block", stmts, line)

    def local_decl_ahead(self) -> bool:
        i = self.i
        while self.t[i].val == "final" and self.t[i].kind == "id":
            i += 1
        ty = self.type_at(i)
        if ty is None:
            return False
        j = ty[1]
        if self.t[j].kind != "id" or self.t[j].val in ("instanceof",):
            return False
        return self.t[j + 1].val in ("=", ";", ",", ":", "[") and self.t[j + 1].kind == "op"

    def local_decl(self, expect_semi: bool = True):
        while self.at("final"):
            self.i += 1
        line = self.peek().line
        ty = self.type_at(self.i)
        self.i = ty[1]
        decls = []
        while True:
            name = self.ident()
            while self.at("["):
                self.i += 1
                self.expect("]")
            init = None
            if self.accept("="):
                init = self.array_init() if self.at("{") else self.expr()
            decls.append((name, init))
            if not self.accept(","):
                break
        if expect_semi:
            self.expect(";")
        return ("local", ty[0], decls, line)

    def stmt(self):
        tok = self.peek()
        v = tok.val
        line = tok.line
        if tok.kind == "op":
            if v == "{":
                return self.block()
            if v == ";":
                self.i += 1
                return ("empty", line)
        if tok.kind == "id":
            if v == "if":
                self.i += 1
                self.expect("(")
                cond = self.expr()
                self.expect(")")
                then = self.stmt()
                other = self.stmt() if self.accept("else") else None
                return ("if", cond, then, other, line)
            if v == "for":
                return self.for_stmt()
            if v == "while":
                self.i += 1
                self.expect("(")
                cond = self.expr()
                self.expect(")")
                return ("while", cond, self.stmt(), line)
            if v == "do":
                self.i += 1
                body = self.stmt()
                self.expect("while")
                self.expect("(")
                cond = self.expr()
                self.expect(")")
                self.expect(";")
                return ("dowhile", body, cond, line)
            if v == "return":
                self.i += 1
                value = None if self.at(";") else self.expr()
                self.expect(";")
                return ("return", value, line)
            if v == "break":
                self.i += 1
                self.expect(";")
                return ("break", line)
            if v == "continue":
                self.i += 1
                self.expect(";")
                return ("continue", line)
            if v == "throw":
                self.i += 1
                value = self.expr()
                self.expect(";")
                return ("throw", value, line)
            if v == "switch":
                return self.switch_stmt()
            if v == "try":
                return self.try_stmt()
            if v == "synchronized":
                self.i += 1
                self.expect("(")
                lock = self.expr()
                self.expect(")")
                return ("sync", lock, self.block(), line)
            if v in ("class", "interface", "enum"):
                raise self.err("local classes are not supported")
        if self.local_decl_ahead():
            return self.local_decl()
        value = self.expr()
        self.expect(";")
        return ("expr", value, line)

    def for_stmt(self):
        line = self.expect("for").line
        self.expect("(")
        if self.local_decl_ahead():
            save = self.i
            while self.at("final"):
                self.i += 1
            ty = self.type_at(self.i)
            j = ty[1]
            if self.t[j + 1].val == ":" and self.t[j + 1].kind == "op":
                self.i = j
                name = self.ident()
                self.expect(":")
                it = self.expr()
                self.expect(")")
                return ("foreach", ty[0], name, it, self.stmt(), line)
            self.i = save
        init = []
        if not self.at(";"):
            if self.local_decl_ahead():
                init.append(self.local_decl(expect_semi=False))
            else:
                init.append(("expr", self.expr(), line))
                while self.accept(","):
                    init.append(("expr", self.expr(), line))
        self.expect(";")
        cond = None if self.at(";") else self.expr()
        self.expect(";")
        update = []
        if not self.at(")"):
            update.append(self.expr())
            while self.accept(","):
                update.append(self.expr())
        self.expect(")")
        return ("for", init, cond, update, self.stmt(), line)

    def switch_stmt(self):
        line = self.expect("switch").line
        self.expect("(")
        subject = self.expr()
        self.expect(")")
        self.expect("{")
        groups = []   # (labels, stmts); labels: list of expr or "default"
        labels: list = []
        body: list = []
        while not self.at("}"):
            if self.at("case") or self.at("default"):
                if body:
                    groups.append((labels, body))
                    labels, body = [], []
                if self.accept("default"):
                    labels.append("default")
                else:
                    self.i += 1
                    labels.append(self.ternary())
                self.expect(":")
            else:
                body.append(self.stmt())
        if labels or body:
            groups.append((labels, body))
        self.expect("}")
        return ("switch", subject, groups, line)

    def try_stmt(self):
        line = self.expect("try").line
        if self.at("("):
            raise self.err("try-with-resources is not supported")
        body = self.block()
        catches = []
        while self.accept("catch"):
            self.expect("(")
            while not self.at(")"):
                self.i += 1
            self.expect(")")
            catches.append(self.block())
        final = self.block() if self.accept("finally") else None
        return ("try", body, catches, final, line)


# --------------------------------------------------------------------------- class scanner

@dataclass(eq=False)
class MethodInfo:
    name: str
    params: list[str]
    varargs: bool
    static: bool
    start: int       # body token range [start, end), braces excluded; None for abstract
    end: int
    owner: "ClassInfo" = None
    ast: object = None


@dataclass(eq=False)
class FieldInfo:
    name: str
    static: bool
    final: bool
    start: int
    end: int
    type_name: str
    owner: "ClassInfo" = None
    ast: object = None


class ClassInfo:
    def __init__(self, name: str, kind: str, file: str, tokens: list[Tok], outer: "ClassInfo | None" = None):
        self.name = name
        self.kind = kind
        self.file = file
        self.tokens = tokens
        self.outer = outer
        self.extends: str | None = None
        self.implements: list[str] = []
        self.fields: dict[str, FieldInfo] = {}
        self.methods: dict[str, list[MethodInfo]] = {}
        self.ctors: list[MethodInfo] = []
        self.nested: dict[str, "ClassInfo"] = {}
        self.enum_constants: list[str] = []

    def __deepcopy__(self, memo):
        return self

    def __repr__(self):
        return f"<class {self.name}>"


def match_close(toks: list[Tok], i: int, open_: str, close: str) -> int:
    """Index of the token that closes the ``open_`` at ``i``."""
    depth = 0
    k = i
    while toks[k].kind != "eof":
        tok = toks[k]
        if tok.kind == "op":
            if tok.val == open_:
                depth += 1
            elif tok.val == close:
                depth -= 1
                if depth == 0:
                    return k
        k += 1
    raise ParseError(f"unbalanced {open_} at line {toks[i].line}")


def match_angle(toks: list[Tok], i: int) -> int:
    """Index of the token that closes the generic ``<`` at ``i`` (``>>`` and ``>>>`` close several levels)."""
    depth = 0
    k = i
    while toks[k].kind != "eof":
        tok = toks[k]
        if tok.kind == "op":
            if tok.val == "<":
                depth += 1
            elif tok.val == ">":
                depth -= 1
            elif tok.val == ">>":
                depth -= 2
            elif tok.val == ">>>":
                depth -= 3
            elif tok.val in (";", "{", "}", "(", ")", "="):
                break
            if depth <= 0:
                return k
        k += 1
    raise ParseError(f"unbalanced < at line {toks[i].line}")


def skip_annotation(toks: list[Tok], i: int) -> int:
    i += 1   # @
    i += 1   # name
    while toks[i].val == "." and toks[i].kind == "op":
        i += 2
    if toks[i].val == "(" and toks[i].kind == "op":
        i = match_close(toks, i, "(", ")") + 1
    return i


def split_params(toks: list[Tok], start: int, end: int) -> tuple[list[str], bool]:
    """Parameter names and the varargs flag of the tokens between the parentheses."""
    names: list[str] = []
    varargs = False
    depth = 0
    cur: list[Tok] = []
    parts: list[list[Tok]] = []
    for tok in toks[start:end]:
        if tok.kind == "op" and tok.val in ("(", "<", "["):
            depth += 1
        elif tok.kind == "op" and tok.val in (")", ">", "]"):
            depth -= 1
        elif tok.kind == "op" and tok.val == ">>":
            depth -= 2
        if tok.kind == "op" and tok.val == "," and depth == 0:
            parts.append(cur)
            cur = []
        else:
            cur.append(tok)
    if cur:
        parts.append(cur)
    for part in parts:
        if any(t.val == "..." for t in part):
            varargs = True
        ids = [t.val for t in part if t.kind == "id"]
        names.append(ids[-1])
    return names, varargs


def scan_class_body(cls: ClassInfo, i: int, end: int, registry: dict[str, ClassInfo]) -> None:
    """Fill ``cls`` from the tokens in [i, end) (the inside of the class body)."""
    toks = cls.tokens
    if cls.kind == "enum":
        depth = 0
        names = []
        while i < end:
            tok = toks[i]
            if tok.kind == "op" and tok.val == ";" and depth == 0:
                i += 1
                break
            if tok.kind == "op" and tok.val in ("(", "{"):
                close = match_close(toks, i, tok.val, ")" if tok.val == "(" else "}")
                i = close + 1
                continue
            if tok.kind == "id" and depth == 0 and (i == 0 or toks[i - 1].val in (",", "{") or not names):
                names.append(tok.val)
            i += 1
        cls.enum_constants = names
    while i < end:
        tok = toks[i]
        if tok.kind == "op" and tok.val == "@" and toks[i + 1].val != "interface":
            i = skip_annotation(toks, i)
            continue
        static = final = False
        while toks[i].kind == "id" and toks[i].val in MODIFIERS:
            static = static or toks[i].val == "static"
            final = final or toks[i].val == "final"
            i += 1
            while toks[i].kind == "op" and toks[i].val == "@" and toks[i + 1].val != "interface":
                i = skip_annotation(toks, i)
        tok = toks[i]
        if tok.kind == "op" and tok.val == "{":
            i = match_close(toks, i, "{", "}") + 1
            continue
        if tok.kind == "op" and tok.val == ";":
            i += 1
            continue
        if tok.kind == "id" and tok.val in ("class", "interface", "enum") or (tok.val == "@" and toks[i + 1].val == "interface"):
            i = scan_type(toks, i, cls.file, registry, cls)
            continue
        if tok.kind == "op" and tok.val == "<":
            i = match_angle(toks, i) + 1
            tok = toks[i]
        # header: tokens up to ( = ; at depth 0
        header_start = i
        j = i
        while True:
            t = toks[j]
            if t.kind == "eof":
                raise ParseError(f"{cls.file}:{tok.line}: member of {cls.name} does not end")
            if t.kind == "op" and t.val in ("(", "=", ";", ","):
                break
            if t.kind == "op" and t.val == "<":
                j = match_angle(toks, j)
            j += 1
        t = toks[j]
        if t.val == "(" and t.kind == "op":
            name = toks[j - 1].val
            close = match_close(toks, j, "(", ")")
            params, varargs = split_params(toks, j + 1, close)
            k = close + 1
            while toks[k].val == "[" and toks[k].kind == "op":
                k += 2
            if toks[k].val == "throws":
                while not (toks[k].kind == "op" and toks[k].val in ("{", ";")):
                    k += 1
            if toks[k].val == "{" and toks[k].kind == "op":
                bend = match_close(toks, k, "{", "}")
                body = (k + 1, bend)
                i = bend + 1
            else:
                body = (None, None)
                i = k + 1
            method = MethodInfo(name, params, varargs, static, body[0], body[1], cls)
            if name == cls.name and j - 1 == header_start:
                cls.ctors.append(method)
            else:
                cls.methods.setdefault(name, []).append(method)
            continue
        # field declarators
        k = i
        ty_res = Parser(toks, cls.file).type_at(i)
        type_name = ty_res[0] if ty_res else "?"
        while True:
            # find the declarator name: last identifier before '=' ',' ';'
            m = k
            while not (toks[m].kind == "op" and toks[m].val in ("=", ",", ";")):
                m += 1
            fname = toks[m - 1].val
            if toks[m - 1].val == "]":
                n = m - 1
                while toks[n].val in ("]", "["):
                    n -= 1
                fname = toks[n].val
            start = end_ = None
            if toks[m].val == "=":
                start = m + 1
                depth = 0
                n = start
                while True:
                    tt = toks[n]
                    if tt.kind == "op" and tt.val in ("(", "{", "["):
                        depth += 1
                    elif tt.kind == "op" and tt.val in (")", "}", "]"):
                        depth -= 1
                    elif tt.kind == "op" and tt.val in (",", ";") and depth == 0:
                        break
                    n += 1
                end_ = n
                m = n
            cls.fields[fname] = FieldInfo(fname, static, final, start, end_, type_name, cls)
            if toks[m].val == ",":
                k = m + 1
                continue
            i = m + 1
            break


def scan_type(toks: list[Tok], i: int, file: str, registry: dict[str, ClassInfo], outer: ClassInfo | None) -> int:
    """Scan the type declaration whose keyword is at ``i``; returns the index after its closing brace."""
    if toks[i].val == "@":
        i += 1
    kind = toks[i].val
    name = toks[i + 1].val
    cls = ClassInfo(name, kind, file, toks, outer)
    i += 2
    if toks[i].val == "<" and toks[i].kind == "op":
        i = match_angle(toks, i) + 1
    while not (toks[i].kind == "op" and toks[i].val == "{"):
        if toks[i].val == "extends" and toks[i].kind == "id":
            ty = Parser(toks, file).type_at(i + 1)
            if kind != "interface":
                cls.extends = ty[0]
            else:
                cls.implements.append(ty[0])
            i = ty[1]
            continue
        if toks[i].val == "implements" and toks[i].kind == "id":
            i += 1
            while True:
                ty = Parser(toks, file).type_at(i)
                cls.implements.append(ty[0])
                i = ty[1]
                if toks[i].val == ",":
                    i += 1
                    continue
                break
            continue
        i += 1
    close = match_close(toks, i, "{", "}")
    scan_class_body(cls, i + 1, close, registry)
    if outer is None:
        registry.setdefault(name, cls)
    else:
        outer.nested[name] = cls
        registry.setdefault(name, cls)
    return close + 1


def scan_file(label: str, text: str, registry: dict[str, ClassInfo]) -> list[Tok]:
    toks = tokenize(text, label)
    i = 0
    while toks[i].kind != "eof":
        tok = toks[i]
        if tok.kind == "id" and tok.val in ("package", "import"):
            while not (toks[i].kind == "op" and toks[i].val == ";"):
                i += 1
            i += 1
            continue
        if tok.kind == "op" and tok.val == "@" and toks[i + 1].val != "interface":
            i = skip_annotation(toks, i)
            continue
        if tok.kind == "id" and tok.val in MODIFIERS:
            i += 1
            continue
        if tok.kind == "id" and tok.val in ("class", "interface", "enum") or (tok.val == "@" and toks[i + 1].val == "interface"):
            i = scan_type(toks, i, label, registry, None)
            continue
        if tok.kind == "op" and tok.val == ";":
            i += 1
            continue
        raise ParseError(f"{label}:{tok.line}: unexpected top-level token {tok.val!r}")
    return toks


# --------------------------------------------------------------------------- unparse (the "source text" of an expression)

def unparse(node) -> str:
    kind = node[0]
    if kind == "lit":
        v = node[1]
        if v is None:
            return "null"
        if isinstance(v, bool):
            return "true" if v else "false"
        if isinstance(v, str):
            return json.dumps(v, ensure_ascii=False)
        return str(v)
    if kind == "char":
        return "'" + node[1] + "'"
    if kind == "name":
        return node[1]
    if kind in ("this", "super"):
        return kind
    if kind == "paren":
        return "(" + unparse(node[1]) + ")"
    if kind == "field":
        return unparse(node[1]) + "." + node[2]
    if kind == "classlit":
        return unparse(node[1]) + ".class"
    if kind == "call":
        head = (unparse(node[1]) + ".") if node[1] is not None else ""
        return head + node[2] + "(" + ", ".join(unparse(a) for a in node[3]) + ")"
    if kind == "new":
        return "new " + node[1] + "(" + ", ".join(unparse(a) for a in node[2]) + ")"
    if kind == "newarray":
        dims = "".join("[" + unparse(d) + "]" for d in node[2]) or "[]"
        init = (" " + unparse(node[3])) if node[3] is not None else ""
        return "new " + node[1] + dims + init
    if kind == "arrayinit":
        return "{" + ", ".join(unparse(e) for e in node[1]) + "}"
    if kind == "cast":
        return "(" + node[1] + ") " + unparse(node[2])
    if kind == "unary":
        return node[1] + unparse(node[2])
    if kind in ("prefix",):
        return node[1] + unparse(node[2])
    if kind == "postfix":
        return unparse(node[2]) + node[1]
    if kind == "binary":
        return unparse(node[2]) + " " + node[1] + " " + unparse(node[3])
    if kind == "assign":
        return unparse(node[2]) + " " + node[1] + " " + unparse(node[3])
    if kind == "cond":
        return unparse(node[1]) + " ? " + unparse(node[2]) + " : " + unparse(node[3])
    if kind == "index":
        return unparse(node[1]) + "[" + unparse(node[2]) + "]"
    if kind == "instanceof":
        return unparse(node[1]) + " instanceof " + node[2]
    return "<" + kind + ">"


def walk(node):
    """Every AST node below ``node`` (tuples and lists), the node itself first."""
    if isinstance(node, tuple) and node and isinstance(node[0], str):
        yield node
        for child in node[1:]:
            yield from walk(child)
    elif isinstance(node, (list, tuple)):
        for child in node:
            yield from walk(child)


# --------------------------------------------------------------------------- values

class Sym:
    """A value the interpreter does not model (kept symbolic instead of guessed)."""

    def __init__(self, text: str, kind: str = "unknown"):
        self.text = text
        self.kind = kind

    def __repr__(self):
        return f"Sym({self.text!r})"


class CondV(Sym):
    """A boolean the oracle cannot decide: a configuration flag or a loaded mod. ``negated`` flips the atom."""

    def __init__(self, text: str, kind: str, negated: bool = False):
        super().__init__(text, kind)
        self.negated = negated

    def __repr__(self):
        return f"Cond({'!' if self.negated else ''}{self.text})"


class ClassRef:
    def __init__(self, name: str):
        self.name = name

    def __eq__(self, other):
        return isinstance(other, ClassRef) and other.name == self.name

    def __hash__(self):
        return hash(("ClassRef", self.name))


class Namespace:
    """A class name used as the qualifier of a static member."""

    def __init__(self, name: str):
        self.name = name


class LibStr(str):
    """A string constant of a ``Lib*Names`` class; remembers which constant it was."""
    ref: str = ""

    def __new__(cls, value: str, ref: str):
        obj = super().__new__(cls, value)
        obj.ref = ref
        return obj


class Inst:
    """An instance of a Thaumic Tinkerer class (a block or an item) as ``TTRegistry`` creates it."""

    def __init__(self, cls: ClassInfo, index: int = 0):
        self.cls = cls
        self.index = index
        self.fields: dict = {}
        self.reg_when: list = []
        self.is_block = False

    def key(self):
        return (self.cls.name, self.index)

    def __eq__(self, other):
        return isinstance(other, Inst) and other.key() == self.key()

    def __hash__(self):
        return hash(self.key())

    def __repr__(self):
        return f"<{self.cls.name}#{self.index}>"


class AspectV:
    def __init__(self, name: str, tag: str):
        self.name = name
        self.tag = tag

    def __eq__(self, other):
        return isinstance(other, AspectV) and other.tag == self.tag

    def __hash__(self):
        return hash(self.tag)


class AspectListV:
    def __init__(self):
        self.entries: list[list] = []   # [tag, amount, constant name]

    def add(self, aspect: AspectV, amount: int):
        for entry in self.entries:
            if entry[0] == aspect.tag:
                entry[1] += amount
                return
        self.entries.append([aspect.tag, amount, aspect.name])

    def as_pairs(self) -> list[list]:
        return [[e[0], e[1]] for e in self.entries]


class ItemV:
    """An item or block that is not a Thaumic Tinkerer instance: vanilla, a Thaumcraft field or unknown."""

    def __init__(self, kind: str, ref: str):
        self.kind = kind     # vanilla_item vanilla_block tc_item tc_block unknown
        self.ref = ref       # Items.diamond, ConfigItems.itemShard, ...

    def __eq__(self, other):
        return isinstance(other, ItemV) and (other.kind, other.ref) == (self.kind, self.ref)

    def __hash__(self):
        return hash((self.kind, self.ref))


class StackV:
    def __init__(self, item, count: int, meta: int, text: str, line: int, file: str):
        self.item = item
        self.count = count
        self.meta = meta
        self.text = text
        self.line = line
        self.file = file
        self.via: str | None = None
        self.fields: dict = {}


class OrOre:
    """``RecipeHelper.oreDictOrStack(stack, ore)``: the ore dictionary entry when it exists, else the stack."""

    def __init__(self, stack, ore: str, text: str):
        self.stack = stack
        self.ore = ore
        self.text = text


class PageV:
    def __init__(self, kind: str, ref: str | None = None, refs: list[str] | None = None, text: str = ""):
        self.kind = kind
        self.ref = ref
        self.refs = refs or []
        self.text = text


class ResearchV:
    def __init__(self, cls_name: str, key: str, tags, col, row, complexity, icon, file: str, line: int):
        self.cls_name = cls_name
        self.kami = cls_name == "KamiResearchItem"
        self.key = key
        self.tags = tags
        self.col = col
        self.row = row
        self.complexity = complexity
        self.icon = icon
        self.parents: list[str] | None = None
        self.parents_hidden: list[str] | None = None
        self.siblings: list[str] | None = None
        self.flags: dict[str, bool] = {n: False for n in
                                       ("secondary", "concealed", "hidden", "special", "stub", "round", "autoUnlock",
                                        "virtual", "lost")}
        self.warp = 0
        self.pages: list[PageV] = []
        self.first_load = True
        self.kami_auto_hidden = False
        self.registered = False
        self.file = file
        self.line = line
        if self.kami:
            self.flags["concealed"] = True


class RecipeV:
    def __init__(self, cls_name: str, name, file: str, line: int):
        self.cls_name = cls_name
        self.name = name
        self.research = None
        self.output = None
        self.aspects: AspectListV | None = None
        self.instability: int | None = None
        self.input = None        # infusion central item / crucible catalyst
        self.stuff: list = []
        self.stuff_texts: list[str] = []
        self.custom_class: str | None = None
        self.file = file
        self.line = line
        self.kind = ""


class MultiV:
    def __init__(self, kind: str):
        self.kind = kind      # research | recipe
        self.items: list = []


class RegistryV:
    """``ThaumicTinkerer.registry``."""


class SuperV:
    """``super`` inside a method of ``cls``."""

    def __init__(self, cls: ClassInfo):
        self.cls = cls


# --------------------------------------------------------------------------- interpreter

RESEARCH_BUILDERS = {"setSecondary": "secondary", "setConcealed": "concealed", "setHidden": "hidden",
                     "setSpecial": "special", "setStub": "stub", "setRound": "round", "setAutoUnlock": "autoUnlock",
                     "setVirtual": "virtual", "setLost": "lost"}
CONFIG_NAMESPACES = {"ConfigHandler": "config", "Config": "config", "LoadedMods": "mod"}
WATCHED_TYPES = ("TTResearchItem", "KamiResearchItem", "ThaumicTinkererArcaneRecipe", "ThaumicTinkererCrucibleRecipe",
                 "ThaumicTinkererInfusionRecipe", "ThaumicTinkererCraftingBenchRecipe")
LIB_NAME_CLASSES = ("LibItemNames", "LibBlockNames")
MAX_LOOP = 2000


class Ctx:
    def __init__(self, cls: ClassInfo | None, this: Inst | None, local: dict | None = None):
        self.cls = cls
        self.this = this
        self.locals: dict = local or {}
        self.conds: dict[str, list] = {}      # text -> [kind, value]
        self.recipes: list = []               # recipes registered as a side effect (GameRegistry.add*)
        self.research: list = []              # research registered as a side effect (registerResearchItem)


class InterpBase:
    def __init__(self, classes: dict[str, ClassInfo], aspects: dict[str, str]):
        self.classes = classes
        self.aspects = aspects          # constant name -> tag
        self.statics: dict = {}
        self.registry_items: dict[str, list[Inst]] = {}
        self.registry_blocks: dict[str, list[Inst]] = {}
        self.evaluated: set[int] = set()       # id() of every watched `new` node that was evaluated
        self.node_sites: dict[int, tuple] = {}
        self.diagnostics: list[str] = []
        self.numeric_aspects: list[AspectV] = []
        self.custom_recipes: list[tuple] = []     # (RecipeV, Inst): CraftingManager.getRecipeList().add(...) in a constructor

    # ---- parsing on demand
    def body_ast(self, m: MethodInfo):
        if m.ast is None:
            if m.start is None:
                raise Unsupported(f"{m.owner.file}: {m.name} has no body")
            p = Parser(m.owner.tokens[m.start - 1:m.end + 1], m.owner.file)
            m.ast = p.block()
        return m.ast

    def init_ast(self, f: FieldInfo):
        if f.ast is None:
            p = Parser(f.owner.tokens[f.start:f.end] + [Tok("eof", "", 0, 0)], f.owner.file)
            f.ast = p.array_init() if p.at("{") else p.expr()
        return f.ast

    # ---- lookup
    def superclass(self, cls: ClassInfo) -> ClassInfo | None:
        if cls.extends and cls.extends in self.classes and self.classes[cls.extends] is not cls:
            return self.classes[cls.extends]
        return None

    def chain(self, cls: ClassInfo):
        seen = set()
        while cls is not None and cls.name not in seen:
            seen.add(cls.name)
            yield cls
            cls = self.superclass(cls)

    def find_field(self, cls: ClassInfo, name: str) -> FieldInfo | None:
        c = cls
        while c is not None:
            for k in self.chain(c):
                if name in k.fields:
                    return k.fields[name]
            c = c.outer
        return None

    def find_methods(self, cls: ClassInfo, name: str) -> list[MethodInfo]:
        found: list[MethodInfo] = []
        c = cls
        while c is not None:
            for k in self.chain(c):
                found.extend(k.methods.get(name, []))
            if found:
                return found
            c = c.outer
        return found

    @staticmethod
    def pick(methods: list[MethodInfo], argc: int) -> MethodInfo | None:
        for m in methods:
            if len(m.params) == argc and not m.varargs:
                return m
        for m in methods:
            if m.varargs and argc >= len(m.params) - 1:
                return m
        return None

    def static_value(self, owner: ClassInfo, name: str):
        key = (owner.name, name)
        if key in self.statics:
            return self.statics[key]
        f = owner.fields[name]
        if f.start is None:
            value = 0 if f.type_name in PRIMITIVES and f.type_name != "boolean" else (False if f.type_name == "boolean" else None)
        else:
            ctx = Ctx(owner, None)
            try:
                value = self.ev(self.init_ast(f), ctx)
            except Unsupported:
                value = Sym(f"{owner.name}.{name}")
        if isinstance(value, str) and not isinstance(value, LibStr) and owner.name in LIB_NAME_CLASSES:
            value = LibStr(value, f"{owner.name}.{name}")
        self.statics[key] = value
        return value

    # ---- instances
    def construct(self, cls: ClassInfo, args: list, index: int = 0, inst: Inst | None = None) -> Inst:
        first = inst is None
        inst = inst or Inst(cls, index)
        ctors = [m for m in cls.ctors if len(m.params) == len(args) and not m.varargs]
        if not ctors and cls.ctors:
            ctors = [m for m in cls.ctors if m.varargs]
        if not ctors:
            if cls.ctors and args:
                raise Unsupported(f"{cls.name}: no constructor takes {len(args)} arguments")
            sup = self.superclass(cls)
            if sup is not None:
                self.construct(sup, [], inst=inst)
            return inst
        ctor = ctors[0]
        ctx = Ctx(cls, inst, dict(zip(ctor.params, args)))
        for stmt in self.ctor_statements(ctor):
            self.ctor_statement(stmt, ctx, cls, inst)
        if first and not any(k.name == "Block" for k in self.chain(cls)):
            pass
        return inst

    CONTROL_KEYWORDS = ("if", "for", "while", "do", "try", "switch", "synchronized", "return", "throw", "final", "else")

    def ctor_statements(self, ctor: MethodInfo):
        """The expression statements of a constructor body, parsed one by one (a statement that does not parse is skipped)."""
        if ctor.ast is not None:
            return ctor.ast
        toks = ctor.owner.tokens
        out = []
        i = ctor.start
        while i < ctor.end:
            j = i
            depth = 0
            while j < ctor.end:
                t = toks[j]
                if t.kind == "op" and t.val in ("(", "{", "["):
                    depth += 1
                elif t.kind == "op" and t.val in (")", "}", "]"):
                    depth -= 1
                    if depth == 0 and t.val == "}" and toks[i].val in self.CONTROL_KEYWORDS:
                        if toks[j + 1].val not in ("else", "catch", "finally"):
                            j += 1
                            break
                elif t.kind == "op" and t.val == ";" and depth == 0:
                    j += 1
                    break
                j += 1
            if toks[i].val not in self.CONTROL_KEYWORDS and toks[i].val != "{":
                p = Parser(toks[i:j] + [Tok("eof", "", 0, 0)], ctor.owner.file)
                try:
                    out.append(p.stmt())
                except ParseError:
                    pass
            i = j
        ctor.ast = out
        return out

    def ctor_statement(self, stmt, ctx: Ctx, cls: ClassInfo, inst: Inst) -> None:
        if stmt[0] == "local":
            try:
                self.run_stmt(stmt, ctx)
            except (OracleError, KeyError, IndexError, TypeError, AttributeError, ValueError):
                pass
            return
        if stmt[0] != "expr":
            return
        node = stmt[1]
        if node[0] == "call" and node[1] is None and node[2] == "this":
            self.construct(cls, [self.lenient(a, ctx) for a in node[3]], inst=inst)
        elif node[0] == "call" and node[1] is None and node[2] == "super":
            args = [self.lenient(a, ctx) for a in node[3]]
            sup = self.superclass(cls)
            if sup is not None:
                self.construct(sup, args, inst=inst)
            else:
                for position, name in SUPER_FIELDS.get(cls.extends or "", {}).items():
                    if position < len(args):
                        inst.fields[name] = args[position]
        else:
            try:
                self.ev(node, ctx)
            except (OracleError, KeyError, IndexError, TypeError, AttributeError, ValueError):
                pass

    def lenient(self, node, ctx: Ctx):
        """Evaluate a constructor argument; what the interpreter does not model stays symbolic."""
        try:
            return self.ev(node, ctx)
        except Unsupported:
            return Sym(unparse(node))

    # ---- method calls
    def call_method(self, m: MethodInfo, args: list, this: Inst | None, parent: Ctx | None):
        ctx = Ctx(m.owner, this)
        params = m.params
        if m.varargs:
            fixed = len(params) - 1
            packed = list(args[:fixed]) + [list(args[fixed:])]
            if len(args) == len(params) and isinstance(args[-1], list):
                packed = list(args)
            args = packed
        ctx.locals = dict(zip(params, args))
        if parent is not None:
            ctx.conds = copy.deepcopy(parent.conds)
        try:
            outcomes = self.run_stmts(self.body_ast(m)[1], ctx)
        except Unsupported as error:
            raise Unsupported(f"{error} [in {m.owner.name}.{m.name}]") from error
        values = [o[2] for o in outcomes if o[1] == "return"]
        if len(outcomes) > 1 and not all(self.same(values[0], v) for v in values[1:]):
            raise Unsupported(f"{m.owner.name}.{m.name}: forks inside an expression")
        if parent is not None and outcomes:
            parent.recipes.extend(outcomes[0][0].recipes)
            parent.research.extend(outcomes[0][0].research)
        return values[0] if values else None

    @staticmethod
    def same(a, b) -> bool:
        if isinstance(a, (ResearchV, RecipeV, MultiV, StackV, AspectListV)):
            return a is b
        return a == b

    # ---- statements
    def run_stmts(self, stmts: list, ctx: Ctx):
        active = [(ctx, None, None)]
        done: list = []
        for st in stmts:
            nxt = []
            for c, _, _ in active:
                for out in self.run_stmt(st, c):
                    (nxt if out[1] is None else done).append(out)
            active = nxt
            if not active:
                break
        return active + done

    def run_stmt(self, st, ctx: Ctx):
        kind = st[0]
        if kind == "block":
            return self.run_stmts(st[1], ctx)
        if kind == "empty":
            return [(ctx, None, None)]
        if kind == "local":
            for name, init in st[2]:
                if init is None:
                    ctx.locals[name] = None
                elif init[0] == "arrayinit":
                    ctx.locals[name] = [self.ev(e, ctx) for e in init[1]]
                else:
                    ctx.locals[name] = self.ev(init, ctx)
            return [(ctx, None, None)]
        if kind == "expr":
            self.ev(st[1], ctx)
            return [(ctx, None, None)]
        if kind == "return":
            return [(ctx, "return", None if st[1] is None else self.ev(st[1], ctx))]
        if kind == "break":
            return [(ctx, "break", None)]
        if kind == "continue":
            return [(ctx, "continue", None)]
        if kind == "throw":
            return [(ctx, "throw", None)]
        if kind == "if":
            return self.run_if(st, ctx)
        if kind == "for":
            return self.run_for(st, ctx)
        if kind == "foreach":
            return self.run_foreach(st, ctx)
        if kind in ("while", "dowhile"):
            return self.run_while(st, ctx)
        if kind == "switch":
            return self.run_switch(st, ctx)
        if kind == "try":
            outs = self.run_stmts(st[1][1], ctx)
            if st[3] is not None:
                fixed = []
                for c, sig, val in outs:
                    fixed.extend((c2, s2, v2) if s2 else (c2, sig, val) for c2, s2, v2 in self.run_stmts(st[3][1], c))
                outs = fixed
            return outs
        if kind == "sync":
            return self.run_stmts(st[2][1], ctx)
        raise Unsupported(f"statement {kind}")

    def truth(self, value):
        if isinstance(value, CondV):
            return value
        if isinstance(value, bool):
            return value
        if isinstance(value, Sym):
            raise Unsupported(f"condition on an unmodelled value {value.text}")
        if value is None:
            return False
        if isinstance(value, (int, float)):
            return value != 0
        raise Unsupported(f"condition on {type(value).__name__}")

    def add_cond(self, ctx: Ctx, cond: CondV, holds: bool) -> bool:
        """Record that the condition ``cond`` evaluates to ``holds``; False when that contradicts the path so far."""
        want = (not cond.negated) if holds else cond.negated     # the truth value of the atom
        known = ctx.conds.get(cond.text)
        if known is not None:
            return known[1] == want
        ctx.conds[cond.text] = [cond.kind, want]
        return True

    def run_if(self, st, ctx: Ctx):
        cond = self.truth(self.ev(st[1], ctx))
        if isinstance(cond, CondV):
            out = []
            for holds in (True, False):
                branch = copy.deepcopy(ctx)
                if not self.add_cond(branch, cond, holds):
                    continue
                if holds:
                    out.extend(self.run_stmt(st[2], branch))
                elif st[3] is not None:
                    out.extend(self.run_stmt(st[3], branch))
                else:
                    out.append((branch, None, None))
            return out
        if cond:
            return self.run_stmt(st[2], ctx)
        if st[3] is not None:
            return self.run_stmt(st[3], ctx)
        return [(ctx, None, None)]

    def loop_body(self, body, ctx: Ctx):
        outs = self.run_stmt(body, ctx)
        if len(outs) != 1:
            raise Unsupported("a loop body forks on a configuration flag")
        return outs[0]

    def run_for(self, st, ctx: Ctx):
        for init in st[1]:
            self.run_stmt(init, ctx)
        for _ in range(MAX_LOOP):
            if st[2] is not None:
                c = self.truth(self.ev(st[2], ctx))
                if isinstance(c, CondV):
                    raise Unsupported("a loop condition on a configuration flag")
                if not c:
                    return [(ctx, None, None)]
            c2, sig, val = self.loop_body(st[4], ctx)
            ctx = c2
            if sig == "return":
                return [(ctx, sig, val)]
            if sig == "break":
                return [(ctx, None, None)]
            for u in st[3]:
                self.ev(u, ctx)
        raise Unsupported("a loop does not end")

    def run_foreach(self, st, ctx: Ctx):
        seq = self.ev(st[3], ctx)
        if not isinstance(seq, list):
            raise Unsupported("for-each over a value that is not a list")
        for item in list(seq):
            ctx.locals[st[2]] = item
            c2, sig, val = self.loop_body(st[4], ctx)
            ctx = c2
            if sig == "return":
                return [(ctx, sig, val)]
            if sig == "break":
                break
        return [(ctx, None, None)]

    def run_while(self, st, ctx: Ctx):
        cond, body = (st[1], st[2]) if st[0] == "while" else (st[2], st[1])
        first = st[0] == "dowhile"
        for _ in range(MAX_LOOP):
            if not first:
                c = self.truth(self.ev(cond, ctx))
                if isinstance(c, CondV):
                    raise Unsupported("a loop condition on a configuration flag")
                if not c:
                    return [(ctx, None, None)]
            first = False
            c2, sig, val = self.loop_body(body, ctx)
            ctx = c2
            if sig == "return":
                return [(ctx, sig, val)]
            if sig == "break":
                return [(ctx, None, None)]
        raise Unsupported("a loop does not end")

    def run_switch(self, st, ctx: Ctx):
        subject = self.ev(st[1], ctx)
        if isinstance(subject, Sym):
            raise Unsupported("switch on an unmodelled value")
        groups = st[2]
        start = None
        for gi, (labels, _) in enumerate(groups):
            for label in labels:
                if label == "default":
                    continue
                if self.ev(label, ctx) == subject:
                    start = gi
                    break
            if start is not None:
                break
        if start is None:
            for gi, (labels, _) in enumerate(groups):
                if "default" in labels:
                    start = gi
        if start is None:
            return [(ctx, None, None)]
        body = []
        for _, stmts in groups[start:]:
            body.extend(stmts)
        outs = self.run_stmts(body, ctx)
        fixed = []
        for c, sig, val in outs:
            fixed.append((c, None, None) if sig == "break" else (c, sig, val))
        return fixed

    # ---- expressions
    def ev(self, node, ctx: Ctx):
        kind = node[0]
        m = getattr(self, "ev_" + kind, None)
        if m is None:
            raise Unsupported(f"expression {kind}")
        return m(node, ctx)

    def ev_lit(self, node, ctx):
        return node[1]

    def ev_char(self, node, ctx):
        return node[1]

    def ev_paren(self, node, ctx):
        return self.ev(node[1], ctx)

    def ev_this(self, node, ctx):
        if ctx.this is None:
            raise Unsupported("this outside an instance")
        return ctx.this

    def ev_super(self, node, ctx):
        return SuperV(ctx.cls)

    def ev_cast(self, node, ctx):
        return self.ev(node[2], ctx)

    def ev_classlit(self, node, ctx):
        return ClassRef(unparse(node[1]).split(".")[-1])

    def ev_arrayinit(self, node, ctx):
        return [self.ev(e, ctx) for e in node[1]]

    def ev_newarray(self, node, ctx):
        if node[3] is not None:
            return [self.ev(e, ctx) for e in node[3][1]]
        sizes = [self.ev(d, ctx) for d in node[2]]
        if sizes and isinstance(sizes[0], int):
            return [None] * sizes[0]
        return []

    def ev_index(self, node, ctx):
        arr = self.ev(node[1], ctx)
        idx = self.ev(node[2], ctx)
        if isinstance(arr, list) and isinstance(idx, int):
            return arr[idx]
        if isinstance(arr, Sym):
            return Sym(f"{arr.text}[{idx}]")
        raise Unsupported("indexing a value that is not a list")

    def ev_name(self, node, ctx):
        name = node[1]
        if name in ctx.locals:
            return ctx.locals[name]
        if ctx.this is not None and name in ctx.this.fields:
            return ctx.this.fields[name]
        if ctx.cls is not None:
            f = self.find_field(ctx.cls, name)
            if f is not None:
                if f.static:
                    return self.static_value(f.owner, name)
                if ctx.this is not None:
                    return ctx.this.fields.get(name, Sym("this." + name))
                return Sym("this." + name)
        return Namespace(name)

    def ev_field(self, node, ctx):
        target = self.ev(node[1], ctx)
        name = node[2]
        if isinstance(target, Namespace):
            return self.namespace_field(target.name, name, ctx)
        if isinstance(target, Inst):
            if name in target.fields:
                return target.fields[name]
            f = self.find_field(target.cls, name)
            if f is not None and f.static:
                return self.static_value(f.owner, name)
            return Sym(f"{target.cls.name}.{name}")
        if isinstance(target, list) and name == "length":
            return len(target)
        if isinstance(target, RegistryV):
            return Sym("registry." + name)
        if isinstance(target, Sym):
            return Sym(f"{target.text}.{name}")
        if target is None:
            return Sym(f"null.{name}")
        if isinstance(target, StackV):
            if name == "stackSize":
                return target.count
            return target.fields.get(name)      # the NBT tag (stackTagCompound) starts as null and is not interpreted
        raise Unsupported(f"field {name} of {type(target).__name__}")

    def namespace_field(self, ns: str, name: str, ctx: Ctx):
        if ns in CONFIG_NAMESPACES:
            return CondV(f"{ns}.{name}", CONFIG_NAMESPACES[ns])
        if ns == "Aspect":
            tag = self.aspects.get(name)
            if tag is None:
                raise Unsupported(f"unknown aspect Aspect.{name}")
            return AspectV(name, tag)
        if ns == "Items":
            return ItemV("vanilla_item", f"Items.{name}")
        if ns == "Blocks":
            return ItemV("vanilla_block", f"Blocks.{name}")
        if ns == "ConfigItems":
            return ItemV("tc_item", f"ConfigItems.{name}")
        if ns == "ConfigBlocks":
            return ItemV("tc_block", f"ConfigBlocks.{name}")
        if ns == "ThaumicTinkerer" and name == "registry":
            return RegistryV()
        cls = self.classes.get(ns)
        if cls is not None:
            f = self.find_field(cls, name)
            if f is not None and f.static:
                return self.static_value(f.owner, name)
            if name in cls.nested:
                return Namespace(name)
            if (ns, name) in self.statics:
                return self.statics[(ns, name)]
        return Sym(f"{ns}.{name}")

    # -- operators
    def ev_unary(self, node, ctx):
        v = self.ev(node[2], ctx)
        op = node[1]
        if op == "!":
            if isinstance(v, CondV):
                return CondV(v.text, v.kind, not v.negated)
            if isinstance(v, bool):
                return not v
            if isinstance(v, Sym):
                raise Unsupported(f"! on {v.text}")
            return not self.truth(v)
        if isinstance(v, Sym):
            return Sym(f"{op}{v.text}")
        if op == "-":
            return -v
        if op == "+":
            return v
        if op == "~":
            return ~v
        raise Unsupported(f"unary {op}")

    def ev_binary(self, node, ctx):
        op = node[1]
        left = self.ev(node[2], ctx)
        if op in ("&&", "||"):
            lt = self.truth(left)
            if not isinstance(lt, CondV):
                if op == "&&" and not lt:
                    return False
                if op == "||" and lt:
                    return True
                right = self.truth(self.ev(node[3], ctx))
                return right
            right = self.truth(self.ev(node[3], ctx))
            if isinstance(right, bool):
                if op == "&&":
                    return lt if right else False
                return True if right else lt
            raise Unsupported("a condition combines two configuration flags")
        right = self.ev(node[3], ctx)
        if op in ("==", "!="):
            eq = self.equal(left, right)
            return eq if op == "==" else (not eq if isinstance(eq, bool) else eq)
        if isinstance(left, Sym) or isinstance(right, Sym):
            return Sym(f"({getattr(left, 'text', left)} {op} {getattr(right, 'text', right)})")
        if op == "+" and (isinstance(left, str) or isinstance(right, str)):
            return java_str(left) + java_str(right)
        if op == "+":
            return left + right
        if op == "-":
            return left - right
        if op == "*":
            return left * right
        if op == "/":
            if isinstance(left, int) and isinstance(right, int):
                return int(left / right)
            return left / right
        if op == "%":
            if isinstance(left, int) and isinstance(right, int):
                return left - right * int(left / right)
            return left % right
        if op == "<":
            return left < right
        if op == ">":
            return left > right
        if op == "<=":
            return left <= right
        if op == ">=":
            return left >= right
        if op == "&":
            return left & right
        if op == "|":
            return left | right
        if op == "^":
            return left ^ right
        if op == "<<":
            return left << right
        if op == ">>":
            return left >> right
        raise Unsupported(f"binary {op}")

    def equal(self, a, b):
        if isinstance(a, CondV) or isinstance(b, CondV):
            raise Unsupported("a configuration flag compared with a value")
        if isinstance(a, Sym) or isinstance(b, Sym):
            raise Unsupported(f"comparison with the unmodelled value {getattr(a, 'text', None) or getattr(b, 'text', None)}")
        if isinstance(a, (ResearchV, RecipeV, MultiV)) or isinstance(b, (ResearchV, RecipeV, MultiV)):
            return a is b
        return a == b

    def ev_cond(self, node, ctx):
        c = self.truth(self.ev(node[1], ctx))
        if isinstance(c, CondV):
            raise Unsupported("a ternary on a configuration flag")
        return self.ev(node[2] if c else node[3], ctx)

    def ev_instanceof(self, node, ctx):
        raise Unsupported("instanceof")

    def ev_assign(self, node, ctx):
        op, target, rhs = node[1], node[2], node[3]
        value = self.ev(rhs, ctx) if rhs[0] != "arrayinit" else self.ev_arrayinit(rhs, ctx)
        if op != "=":
            current = self.ev(target, ctx)
            value = self.ev(("binary", op[:-1], ("lit", current, node[4]), ("lit", value, node[4]), node[4]), ctx)
        self.store(target, value, ctx)
        return value

    def ev_prefix(self, node, ctx):
        current = self.ev(node[2], ctx)
        value = current + 1 if node[1] == "++" else current - 1
        self.store(node[2], value, ctx)
        return value

    def ev_postfix(self, node, ctx):
        current = self.ev(node[2], ctx)
        self.store(node[2], current + 1 if node[1] == "++" else current - 1, ctx)
        return current

    def store(self, target, value, ctx: Ctx) -> None:
        kind = target[0]
        if kind == "name":
            name = target[1]
            if name in ctx.locals:
                ctx.locals[name] = value
                return
            f = self.find_field(ctx.cls, name) if ctx.cls else None
            if f is not None and f.static:
                self.statics[(f.owner.name, name)] = value
                return
            if ctx.this is not None:
                ctx.this.fields[name] = value
                return
            ctx.locals[name] = value
            return
        if kind == "field":
            obj = target[1]
            if obj[0] == "this":
                ctx.this.fields[target[2]] = value
                return
            holder = self.ev(obj, ctx)
            if isinstance(holder, Namespace):
                cls = self.classes.get(holder.name)
                owner = self.find_field(cls, target[2]).owner.name if cls and self.find_field(cls, target[2]) else holder.name
                self.statics[(owner, target[2])] = value
                return
            if isinstance(holder, Inst):
                holder.fields[target[2]] = value
                return
            if isinstance(holder, StackV):
                holder.fields[target[2]] = value
            return
        if kind == "index":
            arr = self.ev(target[1], ctx)
            idx = self.ev(target[2], ctx)
            if isinstance(arr, list) and isinstance(idx, int):
                arr[idx] = value
            return
        raise Unsupported(f"assignment to {kind}")



# --------------------------------------------------------------------------- calls and constructors

class Char(str):
    """A Java ``char`` literal (a key of a recipe pattern, as opposed to a row)."""


class ResLoc(str):
    """A ``ResourceLocation``."""


def java_str(value) -> str:
    if value is None:
        return "null"
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, float):
        return repr(value)
    return str(value)


def flatten_varargs(args: list) -> list:
    if len(args) == 1 and isinstance(args[0], list):
        return list(args[0])
    return list(args)




REGISTRY_ACCESS = ("getFirstItemFromClass", "getFirstBlockFromClass", "getItemFromClassAndName", "getItemArrayFromClass",
                   "getBlockFromClass", "getBlockArrayFromClass", "getItemFromClass")











def split_pattern(stuff: list, what: str):
    """Rows and the key map of a shaped recipe's ``Object...``."""
    rows: list[str] = []
    i = 0
    while i < len(stuff) and isinstance(stuff[i], str) and not isinstance(stuff[i], Char):
        rows.append(str(stuff[i]))
        i += 1
    key: dict[str, object] = {}
    while i < len(stuff):
        k = stuff[i]
        if not isinstance(k, Char) or i + 1 >= len(stuff):
            raise Unsupported(f"{what}: malformed recipe arguments near {k!r}")
        key[str(k)] = stuff[i + 1]
        i += 2
    if not rows:
        raise Unsupported(f"{what}: a shaped recipe without pattern rows")
    return rows, key





def arg_texts(arg_nodes: list) -> list[str]:
    """Source text of every argument, an `new T[] {a, b}` last argument expanded into its elements (like the varargs)."""
    texts = [unparse(n) for n in arg_nodes]
    if arg_nodes and arg_nodes[-1][0] == "newarray" and arg_nodes[-1][3] is not None:
        texts = texts[:-1] + [unparse(e) for e in arg_nodes[-1][3][1]]
    return texts


class Interp(InterpBase):
    """The interpreter with the calls, constructors and the replay of the start-up of the original mod."""

    def ev_char(self, node, ctx):
        return Char(node[1])



    def ev_call(self, node, ctx):
        recv_node, name, arg_nodes = node[1], node[2], node[3]
        line = node[4]
        file = ctx.cls.file if ctx.cls is not None else "?"
        if recv_node is None:
            args = [self.ev(a, ctx) for a in arg_nodes]
            methods = self.find_methods(ctx.cls, name) if ctx.cls is not None else []
            m = self.pick(methods, len(args))
            if m is not None:
                return self.call_method(m, args, ctx.this, ctx)
            if name == "isOpaqueCube" and ctx.this is not None and "field_150004_a" in ctx.this.fields:
                return ctx.this.fields["field_150004_a"]
            return Sym(f"{name}()")
        recv = self.ev(recv_node, ctx)
        args = [self.ev(a, ctx) for a in arg_nodes]
        if isinstance(recv, Namespace):
            return self.static_call(recv.name, name, args, node, ctx)
        if isinstance(recv, RegistryV):
            return self.registry_call(name, args, ctx)
        if isinstance(recv, SuperV):
            sup = self.superclass(recv.cls)
            m = self.pick(self.find_methods(sup, name), len(args)) if sup is not None else None
            if m is not None:
                return self.call_method(m, args, ctx.this, ctx)
            return Sym(f"super.{name}()")
        if isinstance(recv, Inst):
            m = self.pick(self.find_methods(recv.cls, name), len(args))
            if m is not None:
                return self.call_method(m, args, recv, ctx)
            if name == "isOpaqueCube" and "field_150004_a" in recv.fields:
                return recv.fields["field_150004_a"]
            return Sym(f"{recv.cls.name}.{name}()")
        if isinstance(recv, AspectListV):
            if name == "add" and len(args) == 2 and isinstance(args[0], AspectV) and isinstance(args[1], int):
                recv.add(args[0], args[1])
                return recv
            if name == "add":
                raise Unsupported(f"AspectList.add with {[type(a).__name__ for a in args]}")
            return Sym(f"AspectList.{name}()")
        if isinstance(recv, StackV):
            if name == "getItemDamage":
                return recv.meta
            if name == "setItemDamage":
                recv.meta = args[0]
                return None
            return recv if name in ("copy", "setStackDisplayName") else Sym(f"ItemStack.{name}()")
        if isinstance(recv, ResearchV):
            return self.research_call(recv, name, args, node, ctx)
        if isinstance(recv, MultiV):
            if name in ("addRecipe", "addResearch"):
                recv.items.append(args[0])
                return None
            return Sym(f"Multi.{name}()")
        if isinstance(recv, list):
            if name == "add":
                recv.append(args[-1])
                return True
            if name == "size":
                return len(recv)
            if name == "get":
                return recv[args[0]]
            if name == "isEmpty":
                return not recv
            raise Unsupported(f"list.{name}")
        if isinstance(recv, str):
            if name == "equals":
                return recv == args[0]
            if name == "length":
                return len(recv)
            if name == "startsWith":
                return recv.startswith(args[0])
            if name == "endsWith":
                return recv.endswith(args[0])
            raise Unsupported(f"String.{name}")
        if isinstance(recv, AspectV):
            return recv.tag if name == "getTag" else Sym(f"Aspect.{name}()")
        if isinstance(recv, Sym) and name == "add" and recv.text.endswith("getRecipeList()") and len(args) == 1 \
                and isinstance(args[0], Inst):
            # CraftingManager.getInstance().getRecipeList().add(new SomeRecipe(...)): a recipe class of the add-on
            self.evaluated.add(id(node))
            self.node_sites[id(node)] = (file, line, "getRecipeList().add")
            recipe = RecipeV("CraftingManager", None, file, line)
            recipe.kind = "crafting_custom"
            recipe.custom_class = args[0].cls.name
            self.custom_recipes.append((recipe, ctx.this))
            return True
        if isinstance(recv, Sym):
            return Sym(f"{recv.text}.{name}()")
        if recv is None:
            return Sym(f"null.{name}()")
        raise Unsupported(f"call {name} on {type(recv).__name__}")



    def registry_call(self, name, args, ctx):
        if name in ("getFirstItemFromClass", "getFirstBlockFromClass", "getItemFromClass", "getBlockFromClass"):
            table = self.registry_items if "Item" in name else self.registry_blocks
            cls = args[0].name
            index = args[1] if len(args) > 1 and isinstance(args[1], int) else 0
            got = table.get(cls)
            if not got:
                return ItemV("unknown", f"registry.{name}({cls}.class)")
            return got[index]
        if name in ("getItemArrayFromClass", "getBlockArrayFromClass"):
            table = self.registry_items if "Item" in name else self.registry_blocks
            return list(table.get(args[0].name, []))
        if name == "getItemFromClassAndName":
            cls, wanted = args[0].name, args[1]
            for inst in self.registry_items.get(cls, []):
                if self.tt_name(inst) == wanted:
                    return inst
            return ItemV("unknown", f"registry.getItemFromClassAndName({cls}.class, {wanted})")
        return Sym(f"registry.{name}()")



    def tt_name(self, inst: Inst):
        """The name constant (a ``LibStr``) the original class registers this instance under."""
        wanted = "getBlockName" if inst.is_block else "getItemName"
        m = self.pick(self.find_methods(inst.cls, wanted), 0)
        if m is None:
            return None
        return self.call_method(m, [], inst, None)



    def static_call(self, ns, name, args, node, ctx):
        file = ctx.cls.file if ctx.cls is not None else "?"
        if ns == "ResearchHelper" and name in ("recipePage", "arcaneRecipePage", "crucibleRecipePage", "infusionPage",
                                               "enchantPage", "LeviationaryHelp"):
            kinds = {"recipePage": "normal", "arcaneRecipePage": "arcane", "crucibleRecipePage": "crucible",
                     "infusionPage": "infusion", "enchantPage": "enchant"}
            if name == "LeviationaryHelp":
                return PageV("compound", text="ResearchHelper.LeviationaryHelp()")
            if not isinstance(args[0], str):
                raise Unsupported(f"{name} with the non-constant name {args[0]!r}")
            if name == "infusionPage" and len(args) == 2:
                return PageV("infusion", refs=[args[0] + str(i) for i in range(args[1])], text=f"{args[0]}[0..{args[1] - 1}]")
            return PageV(kinds[name], ref=args[0], refs=[args[0]])
        if ns == "Arrays" and name == "asList":
            return flatten_varargs(args)
        if ns == "GameRegistry" and name in ("addRecipe", "addShapelessRecipe", "addShapedRecipe"):
            self.evaluated.add(id(node))
            self.node_sites[id(node)] = (file, node[4], "GameRegistry." + name)
            recipe = RecipeV("GameRegistry", None, file, node[4])
            recipe.kind = "crafting_shapeless" if name == "addShapelessRecipe" else "crafting_shaped"
            recipe.output = args[0]
            recipe.stuff = flatten_varargs(args[1:]) if name == "addShapelessRecipe" else list(args[1:])
            recipe.stuff_texts = arg_texts(node[3])[1:]
            ctx.recipes.append(recipe)
            return recipe
        if ns == "ThaumcraftApi" and (name.startswith("add") and name.endswith("Recipe") or name == "addWarpToResearch"):
            raise Unsupported(f"ThaumcraftApi.{name} called outside the recipe wrappers")
        if ns in ("ThaumicTinkererRecipe", "RecipeHelper") and name == "oreDictOrStack":
            return OrOre(args[0], args[1], unparse(node))
        cls = self.classes.get(ns)
        if cls is not None:
            m = self.pick(self.find_methods(cls, name), len(args))
            if m is not None:
                result = self.call_method(m, args, None, ctx)
                if isinstance(result, StackV) and result.via is None:
                    result = copy.copy(result)
                    result.via = unparse(node)
                return result
        return Sym(f"{ns}.{name}()")



    def research_call(self, r: ResearchV, name, args, node, ctx):
        if name in RESEARCH_BUILDERS:
            r.flags[RESEARCH_BUILDERS[name]] = True
            return r
        if name in ("setParents", "setParentsHidden", "setSiblings"):
            values = flatten_varargs(args)
            for v in values:
                if not isinstance(v, str):
                    raise Unsupported(f"{name} with {v!r}")
            attr = {"setParents": "parents", "setParentsHidden": "parents_hidden", "setSiblings": "siblings"}[name]
            setattr(r, attr, list(values))
            return r
        if name == "setWarp":
            r.warp = args[0]
            return r
        if name == "setComplexity":
            r.complexity = args[0]
            return r
        if name == "setPages":
            self.research_set_pages(r, flatten_varargs(args))
            return r
        if name in ("registerResearchItem", "registerResearch"):
            self.evaluated.add(id(node))
            self.node_sites[id(node)] = (ctx.cls.file, node[4], name)
            r.registered = True
            ctx.research.append(r)
            return r
        if name in ("setItemTriggers", "setEntityTriggers", "setAspectTriggers"):
            raise Unsupported(f"{name}: scan triggers are not part of the oracle")
        if name == "isAutoUnlock":
            return r.flags["autoUnlock"]
        return Sym(f"research.{name}()")



    def research_set_pages(self, r: ResearchV, pages: list) -> None:
        """``TTResearchItem.setPages`` and ``KamiResearchItem.setPages`` of the original, step by step."""
        for page in pages:
            if not isinstance(page, PageV):
                raise Unsupported(f"setPages with {page!r}")
        if r.kami:
            # KamiResearchItem.setPages collects every other research as hidden parents unless the entry is auto-unlocked
            r.kami_auto_hidden = not r.flags["autoUnlock"]
            r.parents_hidden = list(r.parents_hidden or [])
        for page in pages:
            if not r.kami and page.kind == "infusion":     # TTResearchItem.checkInfusion()
                r.parents_hidden = ["INFUSION"] + list(r.parents_hidden or [])
        r.first_load = False
        r.pages = list(pages)



    def ev_new(self, node, ctx):
        tname, arg_nodes, line = node[1], node[2], node[4]
        file = ctx.cls.file if ctx.cls is not None else "?"
        args = [self.ev(a, ctx) for a in arg_nodes]
        if tname == "ItemStack":
            return self.new_stack(node, args, ctx)
        if tname == "AspectList":
            if args:
                raise Unsupported("AspectList with arguments")
            return AspectListV()
        if tname in ("TTResearchItem", "KamiResearchItem"):
            self.evaluated.add(id(node))
            self.node_sites[id(node)] = (file, line, tname)
            if len(args) == 1:
                raise Unsupported(f"{tname}(key) creates a virtual entry")
            if len(args) != 6 or not isinstance(args[0], str) or not isinstance(args[1], AspectListV):
                raise Unsupported(f"{tname} with arguments {[type(a).__name__ for a in args]}")
            for position in (2, 3, 4):
                if not isinstance(args[position], int):
                    raise Unsupported(f"{tname}({args[0]}): argument {position} is not a constant ({args[position]!r})")
            return ResearchV(tname, args[0], args[1], args[2], args[3], args[4], args[5], file, line)
        if tname.startswith("ThaumicTinkerer") and tname.endswith("Recipe"):
            self.evaluated.add(id(node))
            self.node_sites[id(node)] = (file, line, tname)
            return self.new_recipe(tname, args, file, line, arg_texts(arg_nodes))
        if tname == "ThaumicTinkererRecipeMulti":
            multi = MultiV("recipe")
            multi.items = flatten_varargs(args)
            return multi
        if tname == "TTResearchItemMulti":
            multi = MultiV("research")
            if args:
                multi.items = list(args[0])
            return multi
        if tname == "ResearchPage":
            return self.new_page(args, node)
        if tname == "ResourceLocation":
            return ResLoc(args[-1]) if args and isinstance(args[-1], str) else ResLoc(unparse(node))
        if tname in ("ArrayList", "LinkedList", "HashSet"):
            return list(args[0]) if args and isinstance(args[0], list) else []
        cls = self.classes.get(tname)
        if cls is not None and cls.kind == "class":
            inst = self.construct(cls, args)
            inst.is_block = self.is_block(cls)
            return inst
        return Sym(f"new {tname}")



    def is_block(self, cls: ClassInfo) -> bool:
        for k in self.chain(cls):
            if "getBlockName" in k.methods:
                return True
            if k.extends and k.extends.startswith("Block"):
                return True
        return False



    def new_stack(self, node, args, ctx):
        if not args or len(args) > 3:
            raise Unsupported(f"ItemStack with {len(args)} arguments")
        item = args[0]
        if isinstance(item, StackV):
            item = item.item
        if isinstance(item, Sym) or item is None or isinstance(item, (int, str, list)):
            raise Unsupported(f"ItemStack of {item!r} ({unparse(node)})")
        count = args[1] if len(args) > 1 else 1
        meta = args[2] if len(args) > 2 else 0
        if not isinstance(count, int) or not isinstance(meta, int):
            raise Unsupported(f"ItemStack with a non-constant count or damage ({unparse(node)})")
        file = ctx.cls.file if ctx.cls is not None else "?"
        return StackV(item, count, meta, unparse(node), node[4], file)



    def new_page(self, args, node):
        text = unparse(node)
        if len(args) == 1:
            a = args[0]
            if isinstance(a, str):
                return PageV("text", ref=a, text=text)
            if isinstance(a, AspectListV):
                return PageV("aspects", text=text)
            if isinstance(a, StackV):
                return PageV("smelting", text=text)
            if isinstance(a, list):
                return PageV("compound", text=text)
        if len(args) == 2 and isinstance(args[0], ResLoc):
            return PageV("image", ref=str(args[0]), text=text)
        if len(args) == 2 and all(isinstance(a, str) for a in args):
            return PageV("text_concealed", ref=args[1], text=text)
        raise Unsupported(f"ResearchPage with {[type(a).__name__ for a in args]} ({text})")



    def new_recipe(self, tname, args, file, line, texts):
        r = RecipeV(tname, args[0], file, line)
        if tname == "ThaumicTinkererArcaneRecipe":
            r.kind = "arcane_shaped"
            r.research, r.output, r.aspects = args[1], args[2], args[3]
            rest = args[4:]
        elif tname == "ThaumicTinkererCrucibleRecipe":
            r.kind = "crucible"
            r.research = args[0]
            r.output, r.input, r.aspects = args[1], args[2], args[3]
            rest = []
        elif tname == "ThaumicTinkererCraftingBenchRecipe":
            r.kind = "crafting_shaped"
            r.output = args[1]
            rest = args[2:]
        elif tname == "ThaumicTinkererInfusionRecipe":
            r.kind = "infusion"
            if isinstance(args[2], int):       # (name, output, instability, aspects, input, stuff...)
                r.research = args[0]
                offset = 1
            else:                              # (name, research, output, instability, aspects, input, stuff...)
                r.research = args[1]
                offset = 2
            r.output, r.instability, r.aspects, r.input = args[offset], args[offset + 1], args[offset + 2], args[offset + 3]
            rest = args[offset + 4:]
        else:
            raise Unsupported(f"unknown recipe wrapper {tname}")
        r.stuff = flatten_varargs(rest) if len(rest) == 1 and isinstance(rest[0], list) else list(rest)
        r.stuff_texts = texts[len(texts) - len(r.stuff):] if r.stuff else []
        return r

    # ---- the start-up of the original mod
    LOADERS = ("loadSimpleBlock", "loadMetaBlock", "loadSimpleItem", "loadMetaItem")

    def load_registry(self) -> None:
        """Replay ``TTRegistry.preInit``: the blocks and items it creates, with the instances of the meta variants."""
        reg = self.classes.get("TTRegistry")
        if reg is None:
            raise OracleError(f"{REGISTRY_FILE} not found")
        method = self.pick(self.find_methods(reg, "preInit"), 0)
        if method is None:
            raise OracleError("TTRegistry.preInit not found")
        loads = []
        assigned: dict[int, str] = {}      # id(load call) -> the static field it is assigned to
        for node in walk(self.body_ast(method)):
            if node[0] == "assign" and node[1] == "=" and node[2][0] == "name":
                assigned[id(node[3])] = node[2][1]
            elif node[0] == "local":
                for name, init in node[2]:
                    if init is not None:
                        assigned[id(init)] = name
        for node in walk(self.body_ast(method)):
            if node[0] == "call" and node[1] is None and node[2] in self.LOADERS:
                arg = node[3][0]
                if arg[0] != "new":
                    raise OracleError(f"TTRegistry:{node[4]}: {node[2]} of something that is not `new`")
                loads.append((node[4], node[2], arg, assigned.get(id(node))))
        loads.sort(key=lambda entry: entry[0])
        if not loads:
            raise OracleError("TTRegistry.preInit loads nothing")
        ctx = Ctx(reg, None)
        for line, loader, arg, target in loads:
            cls = self.classes.get(arg[1])
            if cls is None:
                raise OracleError(f"TTRegistry:{line}: class {arg[1]} not found")
            is_block = loader.endswith("Block")
            args = [self.ev(a, ctx) for a in arg[2]]
            base = self.construct(cls, args)
            base.is_block = is_block
            instances = [base]
            if loader.startswith("loadMeta"):
                getter = "getMetaBlocks" if is_block else "getMetaItems"
                m = self.pick(self.find_methods(cls, getter), 0)
                extra = self.call_method(m, [], base, None)
                for e in extra:
                    e.is_block = is_block
                instances.extend(extra)
            for i, inst in enumerate(instances):
                inst.index = i
            if target is not None:
                self.statics[("TTRegistry", target)] = base
            (self.registry_blocks if is_block else self.registry_items)[cls.name] = instances
        for instances in list(self.registry_items.values()) + list(self.registry_blocks.values()):
            for inst in instances:
                inst.reg_when = self.should_register(inst)

    def should_register(self, inst: Inst):
        m = self.pick(self.find_methods(inst.cls, "shouldRegister"), 0)
        if m is None:
            return []
        value = self.call_method(m, [], inst, None)
        if value is True:
            return []
        if value is False:
            return None
        if isinstance(value, CondV):
            return [[value.text, value.kind, not value.negated]]
        raise Unsupported(f"{inst.cls.name}.shouldRegister() returned {value!r}")

    def run_entry(self, owner: ClassInfo, name: str, this: Inst | None):
        """Run a no-argument method; returns [(value, ctx)] for every path that returns."""
        m = self.pick(self.find_methods(owner, name), 0)
        if m is None:
            return []
        ctx = Ctx(m.owner, this)
        out = self.run_stmts(self.body_ast(m)[1], ctx)
        return [(val, c) for c, sig, val in out if sig in ("return", None)]

    def simulate(self):
        """Replay init (recipes), ResearchHelper.initResearch and postInit (research); returns (research, recipes)."""
        for call in STARTUP_CALLS:
            ns, method = call.split(".")
            cls = self.classes.get(ns)
            if cls is None:
                continue        # a source tree without the helper (the pinned checkout has it)
            m = self.pick(self.find_methods(cls, method), 0)
            if m is None:
                raise OracleError(f"start-up call {call} not found")
            self.call_method(m, [], None, None)
            self.numeric_aspects = self.statics.get(("NumericAspectHelper", "values"), [])
        self.load_registry()
        order = [i for v in self.registry_items.values() for i in v] + [i for v in self.registry_blocks.values() for i in v]
        recipes: list[tuple] = []
        for inst in order:
            if inst.reg_when is None:
                self.diagnostics.append(f"{inst.cls.name}: shouldRegister() is false, so 1.7.10 never registers its recipes and research")
            for value, ctx in self.run_entry(inst.cls, "getRecipeItem", inst):
                for r in self.flatten(value, ctx.recipes, RecipeV):
                    recipes.append((r, self.merge_when(inst.reg_when or [], ctx.conds), inst))
        for recipe, owner in self.custom_recipes:
            recipes.append((recipe, self.merge_when(owner.reg_when or [] if owner else [], {}), owner))
        research: list[tuple] = []
        helper = self.classes.get("ResearchHelper")
        m = self.pick(self.find_methods(helper, "initResearch"), 0) if helper else None
        if m is None:
            raise OracleError("ResearchHelper.initResearch not found")
        ctx0 = Ctx(helper, None)
        for c, sig, val in self.run_stmts(self.body_ast(m)[1], ctx0):
            for r in self.flatten(None, c.research, ResearchV):
                research.append((r, self.merge_when([], c.conds), None))
        for inst in order:
            for value, ctx in self.run_entry(inst.cls, "getResearchItem", inst):
                for r in self.flatten(value, ctx.research, ResearchV):
                    research.append((r, self.merge_when(inst.reg_when or [], ctx.conds), inst))
        return research, recipes

    @staticmethod
    def flatten(value, side: list, kind) -> list:
        out: list = []
        seen: set[int] = set()

        def add(v):
            if v is None or isinstance(v, Sym):
                return
            if isinstance(v, MultiV):
                for item in v.items:
                    add(item)
                return
            if isinstance(v, kind) and id(v) not in seen:
                seen.add(id(v))
                out.append(v)
            elif not isinstance(v, kind):
                raise Unsupported(f"{type(v).__name__} where {kind.__name__} was expected")

        add(value)
        for v in side:
            add(v)
        return out

    @staticmethod
    def merge_when(reg_when: list, conds: dict):
        """The enabling conditions of a path: [expr, kind, value] ordered, None when they contradict each other."""
        merged: dict[str, list] = {}
        for text, kind, value in reg_when:
            merged[text] = [kind, value]
        for text, (kind, value) in conds.items():
            if text in merged and merged[text][1] != value:
                return None
            merged[text] = [kind, value]
        return [[text, kind, value] for text, (kind, value) in merged.items()]



# --------------------------------------------------------------------------- item resolution tables

# Calls the original makes at start-up that the interpreter has to replay because recipes read their result
# (TTCommonProxy.java:128 calls NumericAspectHelper.init(); the mob essences loop over its list).
STARTUP_CALLS = ("NumericAspectHelper.init",)

# Fields the vanilla superclass constructors set from their arguments, which the add-on's own code reads:
# BlockSlab(boolean isDouble, Material) sets field_150004_a, ItemArmor(material, renderIndex, armorType) sets armorType.
SUPER_FIELDS = {"BlockSlab": {0: "field_150004_a"}, "ItemArmor": {2: "armorType"}}

# Vanilla 1.7.10 field -> 1.21.1 id. A string is for every damage value; a dict maps the damage value.
# Anything that is not in the table is reported as unresolved, never guessed.
VANILLA: dict[str, object] = {
    "Items.diamond": "minecraft:diamond",
    "Items.quartz": "minecraft:quartz",
    "Items.ender_pearl": "minecraft:ender_pearl",
    "Items.ender_eye": "minecraft:ender_eye",
    "Items.iron_ingot": "minecraft:iron_ingot",
    "Items.gold_ingot": "minecraft:gold_ingot",
    "Items.gold_nugget": "minecraft:gold_nugget",
    "Items.wheat_seeds": "minecraft:wheat_seeds",
    "Items.ghast_tear": "minecraft:ghast_tear",
    "Items.feather": "minecraft:feather",
    "Items.nether_star": "minecraft:nether_star",
    "Items.redstone": "minecraft:redstone",
    "Items.paper": "minecraft:paper",
    "Items.book": "minecraft:book",
    "Items.bone": "minecraft:bone",
    "Items.string": "minecraft:string",
    "Items.stick": "minecraft:stick",
    "Items.leather": "minecraft:leather",
    "Items.emerald": "minecraft:emerald",
    "Items.glowstone_dust": "minecraft:glowstone_dust",
    "Items.blaze_rod": "minecraft:blaze_rod",
    "Items.blaze_powder": "minecraft:blaze_powder",
    "Items.clay_ball": "minecraft:clay_ball",
    "Items.glass_bottle": "minecraft:glass_bottle",
    "Items.experience_bottle": "minecraft:experience_bottle",
    "Items.rotten_flesh": "minecraft:rotten_flesh",
    "Items.spider_eye": "minecraft:spider_eye",
    "Items.fire_charge": "minecraft:fire_charge",
    "Items.lava_bucket": "minecraft:lava_bucket",
    "Items.bucket": "minecraft:bucket",
    "Items.shears": "minecraft:shears",
    "Items.snowball": "minecraft:snowball",
    "Items.egg": "minecraft:egg",
    "Items.arrow": "minecraft:arrow",
    "Items.wheat": "minecraft:wheat",
    "Items.cake": "minecraft:cake",
    "Items.stone_sword": "minecraft:stone_sword",
    "Items.nether_wart": "minecraft:nether_wart",
    "Items.comparator": "minecraft:comparator",
    "Items.lead": "minecraft:lead",
    "Items.golden_apple": {0: "minecraft:golden_apple", 1: "minecraft:enchanted_golden_apple"},
    "Items.golden_carrot": "minecraft:golden_carrot",
    "Items.coal": {0: "minecraft:coal", 1: "minecraft:charcoal"},
    "Items.dye": {0: "minecraft:ink_sac", 1: "minecraft:red_dye", 2: "minecraft:green_dye", 3: "minecraft:cocoa_beans",
                  4: "minecraft:lapis_lazuli", 5: "minecraft:purple_dye", 6: "minecraft:cyan_dye",
                  7: "minecraft:light_gray_dye", 8: "minecraft:gray_dye", 9: "minecraft:pink_dye",
                  10: "minecraft:lime_dye", 11: "minecraft:yellow_dye", 12: "minecraft:light_blue_dye",
                  13: "minecraft:magenta_dye", 14: "minecraft:orange_dye", 15: "minecraft:bone_meal"},
    "Items.fish": {0: "minecraft:cod", 1: "minecraft:salmon", 2: "minecraft:tropical_fish", 3: "minecraft:pufferfish"},
    # the potion effect is a data component on 1.21.1: Items.potionitem damage 8195 is fire resistance, 8198 night vision
    "Items.potionitem": {0: "minecraft:potion", 8195: "minecraft:potion", 8198: "minecraft:potion"},
    "Blocks.leaves": {0: "minecraft:oak_leaves", 1: "minecraft:spruce_leaves", 2: "minecraft:birch_leaves",
                      3: "minecraft:jungle_leaves"},
    "Blocks.grass": "minecraft:grass_block",
    "Items.porkchop": "minecraft:porkchop",
    "Items.beef": "minecraft:beef",
    "Items.golden_helmet": "minecraft:golden_helmet",
    "Items.golden_chestplate": "minecraft:golden_chestplate",
    "Items.golden_leggings": "minecraft:golden_leggings",
    "Items.golden_boots": "minecraft:golden_boots",
    "Items.diamond_helmet": "minecraft:diamond_helmet",
    "Items.diamond_chestplate": "minecraft:diamond_chestplate",
    "Items.diamond_leggings": "minecraft:diamond_leggings",
    "Items.diamond_boots": "minecraft:diamond_boots",
    "Blocks.stone": {0: "minecraft:stone"},
    "Blocks.dirt": {0: "minecraft:dirt"},
    "Blocks.sand": {0: "minecraft:sand"},
    "Blocks.glass": "minecraft:glass",
    "Blocks.obsidian": "minecraft:obsidian",
    "Blocks.tnt": "minecraft:tnt",
    "Blocks.ice": "minecraft:ice",
    "Blocks.soul_sand": "minecraft:soul_sand",
    "Blocks.netherrack": "minecraft:netherrack",
    "Blocks.glowstone": "minecraft:glowstone",
    "Blocks.enchanting_table": "minecraft:enchanting_table",
    "Blocks.dispenser": "minecraft:dispenser",
    "Blocks.dropper": "minecraft:dropper",
    "Blocks.hopper": "minecraft:hopper",
    "Blocks.crafting_table": "minecraft:crafting_table",
    "Blocks.ender_chest": "minecraft:ender_chest",
    "Blocks.bookshelf": "minecraft:bookshelf",
    "Blocks.gravel": "minecraft:gravel",
    "Blocks.quartz_ore": "minecraft:nether_quartz_ore",
    "Blocks.quartz_block": {0: "minecraft:quartz_block", 1: "minecraft:chiseled_quartz_block", 2: "minecraft:quartz_pillar"},
    "Blocks.bedrock": "minecraft:bedrock",
    "Blocks.cobblestone": "minecraft:cobblestone",
    "Blocks.planks": {0: "minecraft:oak_planks"},
    "Blocks.log": {0: "minecraft:oak_log"},
    "Blocks.wool": {0: "minecraft:white_wool"},
    "Blocks.clay": "minecraft:clay",
    "Blocks.cactus": "minecraft:cactus",
    "Blocks.piston": "minecraft:piston",
    "Blocks.snow": "minecraft:snow_block",
    "Blocks.iron_block": "minecraft:iron_block",
    "Blocks.gold_block": "minecraft:gold_block",
    "Blocks.lapis_block": "minecraft:lapis_block",
    "Blocks.redstone_block": "minecraft:redstone_block",
}

# 1.7.10 stored variants of one Thaumic Tinkerer item or block in its damage value; the port made them separate registry
# entries. (class, damage value) -> (Lib*Names class, constant) of the port, or (class, constant, index) for an array
# constant. Evidence: docs/deviations/*.md and the port's own Lib*Names.
TT_META: dict[tuple[str, int], tuple] = {
    ("BlockDarkQuartz", 1): ("LibBlockNames", "DARK_QUARTZ_CHISELED"),     # LibBlockNames.java: the 3 variants are 3 blocks
    ("BlockDarkQuartz", 2): ("LibBlockNames", "DARK_QUARTZ_PILLAR"),
    ("BlockMagnet", 1): ("LibBlockNames", "MOB_MAGNET"),                   # the mob magnet was damage value 1 of the magnet
    # damage value 1 of the Talisman of Withhold was its "absorbing" state: a data component of the same item now
    # (docs/deviations/BasicItems.md section 1)
    ("ItemXPTalisman", 1): ("same_item",),
}
for _meta in range(8):                                                     # LibItemNames.KAMI_RESOURCE_NAMES, in the original order
    TT_META[("ItemKamiResource", _meta)] = ("LibItemNames", "KAMI_RESOURCE_NAMES", _meta)
# Variants whose port path is built by a function of the damage value (ItemInfusedGrain.path, ItemInfusedPotion.path,
# ItemMobAspect.path in the port): damage value -> suffix. The infused grain and potion use the four primal aspects in
# the order of PRIMAL_ASPECT_ENUM (aer, ignis, terra, aqua); the mob aspect uses NumericAspectHelper's order and the form
# damage // 21 (0 basic, 1 condensed, 2 infused).
PRIMAL_SUFFIXES = ("aer", "ignis", "terra", "aqua")
WILDCARD = 32767     # OreDictionary.WILDCARD_VALUE: any damage value of the item


# --------------------------------------------------------------------------- resolution

class Resolver:
    """Turns the item values of the original into the JSON of the oracle and, where mechanical, 1.21.1 ids."""

    def __init__(self, interp: Interp, legacy: dict, port_names: dict[str, dict[str, str]], category: str):
        self.interp = interp
        self.category = category
        self.legacy = legacy
        self.port_names = port_names
        self.unresolved: list[dict] = []
        self.multi: dict[str, int] = {}
        for table in (interp.registry_items, interp.registry_blocks):
            for name, instances in table.items():
                self.multi[name] = len(instances)

    def owner(self, inst: Inst | None) -> str | None:
        if inst is None:
            return None
        return inst.cls.name if self.multi.get(inst.cls.name, 1) == 1 else f"{inst.cls.name}#{inst.index}"

    def lib(self, lib: str, const: str):
        return self.port_names.get(lib, {}).get(const)

    def resolve_tt(self, inst: Inst, meta: int):
        cls = inst.cls.name
        name = self.interp.tt_name(inst)
        if meta == WILDCARD:
            meta = 0
        if cls in ("ItemInfusedGrain", "ItemInfusedPotion"):
            base = self.lib("LibItemNames", "INFUSED_GRAIN" if cls == "ItemInfusedGrain" else "INFUSED_POTION")
            if meta < len(PRIMAL_SUFFIXES) and base:
                return f"thaumictinkerer:{base}_{PRIMAL_SUFFIXES[meta]}", None
            return None, f"damage value {meta} of {cls}"
        if cls == "ItemMobAspect":
            base = self.lib("LibItemNames", "MOB_ASPECT")
            values = self.interp.numeric_aspects
            if meta < 0 or meta >= 63 or meta % 21 >= len(values):
                return None, f"damage value {meta} of ItemMobAspect is not an aspect"
            tag = values[meta % 21].fields["aspect"].tag
            form = ("", "condensed_", "infused_")[meta // 21]
            return f"thaumictinkerer:{base}_{form}{tag}", None
        mapped = TT_META.get((cls, meta))
        if mapped == ("same_item",):
            mapped, meta = None, 0
        if mapped is not None:
            value = self.lib(mapped[0], mapped[1])
            if len(mapped) == 3 and isinstance(value, list):
                value = value[mapped[2]]
            if not isinstance(value, str):
                return None, f"{mapped[0]}.{mapped[1]} does not exist in the port"
            return "thaumictinkerer:" + value, None
        if meta != 0:
            return None, f"damage value {meta} of {cls} has no entry in TT_META"
        if not isinstance(name, LibStr):
            return None, f"{cls} has no Lib name constant ({name!r})"
        lib, const = name.ref.split(".")
        value = self.lib(lib, const)
        if value is None:
            return None, f"{name.ref} does not exist in the port"
        return "thaumictinkerer:" + value, None

    def resolve_vanilla(self, ref: str, meta: int):
        if meta == WILDCARD:
            meta = 0
        entry = VANILLA.get(ref)
        if entry is None:
            return None, f"{ref} is not in the VANILLA table"
        if isinstance(entry, str):
            return entry, None
        if meta in entry:
            return entry[meta], None
        return None, f"{ref} damage value {meta} is not in the VANILLA table"

    def resolve_tc(self, ref: str, meta: int):
        if meta == WILDCARD:
            meta = 0
        kind = "item" if ref.startswith("ConfigItems.") else "block"
        field = ref.split(".", 1)[1]
        row = self.legacy.get((kind, field, meta))
        if row is None:
            return None, f"no legacy_ids row for {ref}:{meta}"
        value = row.get("item") if kind == "block" else row.get("id")
        if value is None:
            return None, f"legacy_ids: {ref}:{meta} has no 1.21.1 counterpart ({row.get('note', '')})".strip()
        return value, None

    def stack(self, st: StackV, where: str) -> dict:
        item = st.item
        if isinstance(item, Inst):
            kind = "tt_block" if item.is_block else "tt_item"
            ref = self.owner(item)
            resolved, why = self.resolve_tt(item, st.meta)
        elif isinstance(item, ItemV):
            kind, ref = item.kind, item.ref
            if kind in ("vanilla_item", "vanilla_block"):
                resolved, why = self.resolve_vanilla(ref, st.meta)
            elif kind in ("tc_item", "tc_block"):
                resolved, why = self.resolve_tc(ref, st.meta)
            else:
                resolved, why = None, f"{ref} is not available in the registry"
        else:
            raise Unsupported(f"{where}: ItemStack of {item!r}")
        doc = {"text": st.text, "kind": kind, "ref": ref, "count": st.count, "meta": st.meta, "resolved": resolved,
               "unresolved": why}
        if st.meta == WILDCARD:
            doc["wildcard"] = True
        if st.via:
            doc["via"] = st.via
        if why:
            self.unresolved.append({"where": where, "text": st.text, "reason": why})
        return doc

    def bare(self, value, text: str, where: str) -> dict:
        """An ingredient that is an item/block (or an ore name) and not an ItemStack."""
        if isinstance(value, StackV):
            return self.stack(value, where)
        if isinstance(value, OrOre):
            inner = self.bare(value.stack, value.text, where)
            return {"text": value.text, "kind": "oredict_or_stack", "ore": value.ore, "stack": inner,
                    "resolved": inner["resolved"], "unresolved": inner["unresolved"]}
        if isinstance(value, str):
            return {"text": text, "kind": "oredict", "ore": str(value), "count": 1, "meta": 0, "resolved": None,
                    "unresolved": "ore dictionary name (a tag on 1.21.1)"}
        if isinstance(value, (Inst, ItemV)):
            return self.stack(StackV(value, 1, 0, text, 0, "?"), where)
        raise Unsupported(f"{where}: ingredient {text} is {value!r}")


def merge_complementary(docs: list[dict]) -> list[dict]:
    """Merge entries that are identical except for one condition that is true in one and false in the other.

    The interpreter forks a path at every `if` on a configuration flag or a loaded mod, so what was built before the
    `if` appears once per branch; if the branches do not change it, the flag has no effect and is dropped.
    """
    changed = True
    while changed:
        changed = False
        groups: dict[str, list[dict]] = {}
        for d in docs:
            body = json.dumps({k: v for k, v in d.items() if k not in ("when", "order")}, sort_keys=True)
            groups.setdefault(body, []).append(d)
        merged: list[dict] = []
        consumed: set[int] = set()
        for d in docs:
            if id(d) in consumed:
                continue
            body = json.dumps({k: v for k, v in d.items() if k not in ("when", "order")}, sort_keys=True)
            partner = None
            for other in groups[body]:
                if other is d or id(other) in consumed:
                    continue
                a = {(w["expr"], w["kind"]): w["value"] for w in d["when"]}
                b = {(w["expr"], w["kind"]): w["value"] for w in other["when"]}
                if a.keys() != b.keys():
                    continue
                diff = [k for k in a if a[k] != b[k]]
                if len(diff) == 1:
                    partner = (other, diff[0])
                    break
            if partner is None:
                merged.append(d)
                continue
            other, drop = partner
            consumed.add(id(other))
            consumed.add(id(d))
            joined = dict(d)
            if "order" in joined:
                joined["order"] = min(d["order"], other["order"])
            joined["when"] = [w for w in d["when"] if (w["expr"], w["kind"]) != drop]
            merged.append(joined)
            changed = True
        docs = merged
    return docs


def clamp_complexity(value: int) -> int:
    return max(1, min(3, value))


def when_json(when: list) -> list[dict]:
    return [{"expr": text, "kind": kind, "value": value} for text, kind, value in when]


def aspects_json(aspects: AspectListV | None) -> dict:
    return {tag: amount for tag, amount in (aspects.as_pairs() if aspects else [])}


def source_ref(file: str, line: int) -> str:
    return f"{file}:{line}"


PAGE_KINDS_WITH_RECIPES = ("normal", "arcane", "crucible", "infusion", "enchant")


def page_json(page: PageV, key: str) -> dict:
    if page.kind == "text":
        return {"kind": "text", "key": page.ref, "textKey": f"ttresearch.page.{key}.{page.ref}"}
    if page.kind == "text_concealed":
        return {"kind": "text_concealed", "key": page.ref}
    if page.kind in PAGE_KINDS_WITH_RECIPES:
        return {"kind": page.kind, "recipes": list(page.refs)}
    if page.kind == "image":
        return {"kind": "image", "image": page.ref}
    return {"kind": page.kind, "source": page.text}


def research_doc(r: ResearchV, when: list, inst: Inst | None, res: Resolver) -> dict:
    where = f"research {r.key}"
    if isinstance(r.icon, StackV):
        icon = res.stack(r.icon, where + " icon")
    elif isinstance(r.icon, ResLoc):
        icon = {"resource": str(r.icon)}
    else:
        raise Unsupported(f"{where}: icon {r.icon!r}")
    pages = [page_json(p, r.key) for p in r.pages]
    return {
        "key": r.key,
        "category": res.category,
        "kami": r.kami,
        "class": r.cls_name,
        "aspects": aspects_json(r.tags),
        "column": r.col,
        "row": r.row,
        "complexitySource": r.complexity,
        "complexity": clamp_complexity(r.complexity),
        "icon": icon,
        "parents": list(r.parents or []),
        "parentsHidden": list(r.parents_hidden or []),
        "parentsHiddenAuto": r.kami_auto_hidden,
        "siblings": list(r.siblings or []),
        "flags": dict(r.flags),
        "warp": r.warp,
        "pages": pages,
        "textPages": sum(1 for p in r.pages if p.kind == "text"),
        "when": when_json(when),
        "owner": res.owner(inst) if inst is not None else "ResearchHelper.initResearch",
        "registered": inst is None or inst.reg_when is not None,
        "source": source_ref(r.file, r.line),
    }


def recipe_doc(r: RecipeV, when: list, inst: Inst | None, res: Resolver) -> dict:
    where = f"recipe {r.name or (r.output.text if r.output else r.custom_class)}"
    doc: dict = {"name": r.name or None, "kind": r.kind, "research": r.research}      # "" = unnamed (ConfigResearch.recipes skips it)
    if r.kind == "crafting_shapeless":
        doc["research"] = None
    if r.kind == "crafting_custom":
        doc["recipeClass"] = r.custom_class
        doc["output"] = None
        doc["when"] = when_json(when)
        doc["owner"] = res.owner(inst) if inst is not None else None
        doc["registered"] = inst is None or inst.reg_when is not None
        doc["source"] = source_ref(r.file, r.line)
        return doc
    doc["output"] = res.stack(r.output, where + " output")
    if r.aspects is not None:
        doc["aspects"] = aspects_json(r.aspects)
    if r.kind == "infusion":
        doc["instability"] = r.instability
        doc["input"] = res.bare(r.input, unparse_value(r.input), where + " central item")
        doc["components"] = [res.bare(v, t, where + " component") for v, t in zip(r.stuff, r.stuff_texts)]
        doc["ingredientCount"] = len(r.stuff)
    elif r.kind == "crucible":
        doc["catalyst"] = res.bare(r.input, unparse_value(r.input), where + " catalyst")
    elif r.kind == "crafting_shapeless":
        doc["ingredients"] = [res.bare(v, t, where + " ingredient") for v, t in zip(r.stuff, r.stuff_texts)]
        doc["ingredientCount"] = len(r.stuff)
    else:
        rows, key = split_pattern(r.stuff, where)
        texts = dict(zip([str(v) for v in r.stuff[len(rows)::2]], r.stuff_texts[len(rows) + 1::2]))
        doc["pattern"] = rows
        doc["width"] = max(len(row) for row in rows)
        doc["height"] = len(rows)
        doc["ingredientCount"] = sum(1 for row in rows for ch in row if ch != " ")
        doc["key"] = {ch: res.bare(v, texts.get(ch, ch), where + f" key {ch}") for ch, v in key.items()}
        missing = sorted({ch for row in rows for ch in row if ch != " " and ch not in key})
        if missing:
            raise Unsupported(f"{where}: pattern symbols {missing} have no ingredient")
    doc["when"] = when_json(when)
    doc["owner"] = res.owner(inst) if inst is not None else None
    doc["registered"] = inst is None or inst.reg_when is not None
    doc["source"] = source_ref(r.file, r.line)
    return doc


def unparse_value(value) -> str:
    if isinstance(value, StackV):
        return value.text
    if isinstance(value, OrOre):
        return value.text
    if isinstance(value, Inst):
        return value.cls.name
    if isinstance(value, ItemV):
        return value.ref
    return repr(value)


# --------------------------------------------------------------------------- extraction

EXCLUDED_IMPLEMENTATIONS = {
    "src/main/java/thaumic/tinkerer/common/registry/ThaumicTinkererArcaneRecipe.java",
    "src/main/java/thaumic/tinkerer/common/registry/ThaumicTinkererCrucibleRecipe.java",
    "src/main/java/thaumic/tinkerer/common/registry/ThaumicTinkererInfusionRecipe.java",
    "src/main/java/thaumic/tinkerer/common/registry/ThaumicTinkererCraftingBenchRecipe.java",
    "src/main/java/thaumic/tinkerer/common/research/TTResearchItem.java",
    "src/main/java/thaumic/tinkerer/common/research/KamiResearchItem.java",
}
RESEARCH_SITE_TYPES = ("TTResearchItem", "KamiResearchItem")


def scan_sites(label: str, toks: list[Tok]) -> list[tuple]:
    """Every place that constructs research or a recipe, by token pattern: (file, line, type)."""
    sites: list[tuple] = []
    if label in EXCLUDED_IMPLEMENTATIONS:
        return sites
    for i, tok in enumerate(toks):
        if tok.kind != "id":
            continue
        if tok.val == "new" and toks[i + 1].kind == "id" and toks[i + 1].val in WATCHED_TYPES and toks[i + 2].val == "(":
            sites.append((label, tok.line, toks[i + 1].val))
        elif tok.val == "GameRegistry" and toks[i + 1].val == "." and re.fullmatch(r"add\w*Recipe", toks[i + 2].val):
            sites.append((label, tok.line, "GameRegistry." + toks[i + 2].val))
        elif tok.val == "ThaumcraftApi" and toks[i + 1].val == "." and (
                re.fullmatch(r"add\w*Recipe", toks[i + 2].val) or toks[i + 2].val == "addWarpToResearch"):
            sites.append((label, tok.line, "ThaumcraftApi." + toks[i + 2].val))
        elif tok.val in ("registerResearchItem",) and toks[i - 1].val == "." and toks[i + 1].val == "(":
            sites.append((label, tok.line, tok.val))
        elif tok.val == "getRecipeList" and toks[i + 1].val == "(" and toks[i + 2].val == ")" and toks[i + 3].val == "." \
                and toks[i + 4].val == "add":
            sites.append((label, tok.line, "getRecipeList().add"))
    return sites


@dataclass
class Plan:
    files: dict[str, str] = field(default_factory=dict)
    errors: list[str] = field(default_factory=list)
    summary: list[str] = field(default_factory=list)


def read_text(path: Path, hashes: dict[str, str], label: str) -> str:
    data = path.read_bytes()
    hashes[label] = sha256(data)
    return data.decode("utf-8")


def load_aspects(text: str) -> dict[str, str]:
    table = dict(re.findall(r'public static final Aspect (\w+) = new Aspect\("(\w+)"', text))
    if len(table) < 40:
        raise OracleError(f"{ASPECT_JAVA}: only {len(table)} aspect constants found")
    return table


def load_port_names(root: Path, hashes: dict[str, str]) -> dict[str, dict[str, str]]:
    names: dict[str, dict[str, str]] = {}
    for lib in LIB_NAME_CLASSES:
        label = f"{PORT_LIB}/{lib}.java"
        classes: dict[str, ClassInfo] = {}
        scan_file(label, read_text(root / label, hashes, label), classes)
        interp = Interp(classes, {})
        cls = classes[lib]
        names[lib] = {}
        for fname, f in cls.fields.items():
            if f.static:
                value = interp.static_value(cls, fname)
                if isinstance(value, str):
                    names[lib][fname] = str(value)
                elif isinstance(value, list) and all(isinstance(v, str) for v in value):
                    names[lib][fname] = [str(v) for v in value]
    return names


def load_legacy(root: Path, hashes: dict[str, str]) -> dict:
    document = json.loads(read_text(root / LEGACY_IDS, hashes, LEGACY_IDS))
    return {(row["kind"], row["field"], row["meta"]): row for row in document["rows"]}


def build(source: Path, root: Path = ROOT) -> Plan:
    plan = Plan()
    java = source / JAVA_ROOT
    if not java.is_dir():
        raise OracleError(f"{java} is not a Thaumic Tinkerer checkout")
    hashes: dict[str, str] = {}
    inputs: dict[str, str] = {}
    classes: dict[str, ClassInfo] = {}
    sites: list[tuple] = []
    token_files = 0
    scan_hashes: dict[str, str] = {}
    for path in sorted(java.rglob("*.java")):
        label = path.relative_to(source).as_posix()
        text = read_text(path, scan_hashes, label)
        toks = scan_file(label, text, classes)
        sites.extend(scan_sites(label, toks))
        token_files += 1
    aspects = load_aspects(read_text(root / ASPECT_JAVA, inputs, ASPECT_JAVA))
    port_names = load_port_names(root, inputs)
    legacy = load_legacy(root, inputs)
    interp = Interp(classes, aspects)
    try:
        research, recipes = interp.simulate()
    except OracleError as error:
        plan.errors.append(str(error))
        return plan

    reached = Counter()
    for node_id in interp.evaluated:
        reached[interp.node_sites[node_id]] += 1
    wanted = Counter()
    for site in sites:
        wanted[site] += 1
    for site, count in sorted(wanted.items()):
        if reached.get(site, 0) < count:
            plan.errors.append(f"{site[0]}:{site[1]}: {site[2]} construction not understood or not reached "
                               f"({reached.get(site, 0)} of {count} evaluated)")
    for site, count in sorted(reached.items()):
        if wanted.get(site, 0) < count:
            plan.errors.append(f"{site[0]}:{site[1]}: {site[2]} evaluated but not found by the token scan")
    if plan.errors:
        return plan

    lib = classes.get(CATEGORY_CONSTANT[0])
    if lib is None or CATEGORY_CONSTANT[1] not in lib.fields:
        raise OracleError("{}.{} not found".format(*CATEGORY_CONSTANT))
    category = interp.static_value(lib, CATEGORY_CONSTANT[1])
    if not isinstance(category, str):
        raise OracleError("{}.{} is not a string".format(*CATEGORY_CONSTANT))
    res = Resolver(interp, legacy, port_names, str(category))
    research_docs = []
    for r, when, inst in research:
        if when is None:
            continue
        research_docs.append(research_doc(r, when, inst, res))
    recipe_docs = []
    for order, (r, when, inst) in enumerate(recipes):
        if when is None:
            continue
        doc = recipe_doc(r, when, inst, res)
        doc["order"] = order          # registration order: the last recipe of a name is the one a research page shows
        recipe_docs.append(doc)
    research_docs = merge_complementary(research_docs)
    recipe_docs = merge_complementary(recipe_docs)
    research_docs.sort(key=lambda d: (d["key"], json.dumps(d["when"])))
    recipe_docs.sort(key=lambda d: d["order"])

    # diagnostics
    names_seen: dict[str, int] = {}
    for d in recipe_docs:
        if d["name"]:
            names_seen[d["name"]] = names_seen.get(d["name"], 0) + 1
    shadowed = sorted(n for n, c in names_seen.items() if c > 1)
    known = set(names_seen)
    dangling = []
    for d in research_docs:
        for page in d["pages"]:
            for ref in page.get("recipes", []):
                if ref not in known:
                    dangling.append(f"{d['key']} -> {page['kind']} page {ref}")
    for message in dict.fromkeys(interp.diagnostics):
        plan.summary.append("note: " + message)

    per_file: dict[str, dict[str, int]] = {}
    for file, line, kind in sites:
        entry = per_file.setdefault(file, {"research": 0, "recipes": 0})
        if kind in RESEARCH_SITE_TYPES:
            entry["research"] += 1
        elif kind.startswith(("Thaumic", "GameRegistry", "getRecipeList")):
            entry["recipes"] += 1
    sources_digest = sha256(json.dumps({**inputs, **scan_hashes}, sort_keys=True).encode())
    keys = sorted({d["key"] for d in research_docs})
    head = {"generator": GENERATOR, "source": {"repository": SOURCE_REPO, "commit": SOURCE_COMMIT,
                                              "license": LICENSE_NAME}, "provenance_digest": sources_digest}
    research_out = {**head, "count": len(research_docs), "keys": len(keys), "entries": research_docs}
    recipes_out = {**head, "count": len(recipe_docs), "entries": recipe_docs}
    kinds: dict[str, int] = {}
    for d in recipe_docs:
        kinds[d["kind"]] = kinds.get(d["kind"], 0) + 1
    provenance = {
        **head,
        "pin": "the source checkout must be at the commit above (git rev-parse HEAD) unless --allow-unpinned",
        "inputs": dict(sorted(inputs.items())),
        "sources": dict(sorted(scan_hashes.items())),
        "constructions": {k: per_file[k] for k in sorted(per_file)},
        "totals": {"source_files": token_files, "research_sites": sum(v["research"] for v in per_file.values()),
                   "recipe_sites": sum(v["recipes"] for v in per_file.values()),
                   "research_entries": len(research_docs), "research_keys": len(keys),
                   "recipes": len(recipe_docs), "recipe_kinds": dict(sorted(kinds.items()))},
        "recipe_names_registered_more_than_once": shadowed,
        "page_references_without_recipe": dangling,
        "unresolved_items": sorted(res.unresolved, key=lambda u: (u["where"], u["text"])),
    }
    base = OUT_DIR
    plan.files = {f"{base}/research.json": dumps(research_out), f"{base}/recipes.json": dumps(recipes_out),
                  f"{base}/provenance.json": dumps(provenance)}
    plan.summary.insert(0, f"{token_files} source files, {len(sites)} construction sites "
                           f"({provenance['totals']['research_sites']} research, {provenance['totals']['recipe_sites']} recipe)")
    plan.summary.append(f"{len(research_docs)} research entries for {len(keys)} keys, {len(recipe_docs)} recipes {dict(sorted(kinds.items()))}")
    plan.summary.append(f"{len(res.unresolved)} item expressions unresolved (see provenance.json)")
    if shadowed:
        plan.summary.append(f"recipe names registered more than once: {', '.join(shadowed)}")
    if dangling:
        plan.summary.append(f"page references without a recipe: {len(dangling)}")
    for file in sorted(per_file):
        plan.summary.append(f"  {file}: {per_file[file]['research']} research, {per_file[file]['recipes']} recipe constructions")
    return plan


# --------------------------------------------------------------------------- cli

def verify_pin(source: Path) -> None:
    try:
        head = subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"], check=True, capture_output=True,
                              text=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError) as error:
        raise OracleError(f"cannot read the commit of {source} ({error}); pass --allow-unpinned to skip") from error
    if head != SOURCE_COMMIT:
        raise OracleError(f"{source} is at {head}, the oracle is pinned to {SOURCE_COMMIT}; pass --allow-unpinned to extract anyway")


def check(plan: Plan, root: Path) -> list[str]:
    problems = []
    for rel, text in plan.files.items():
        path = root / rel
        if not path.is_file():
            problems.append(f"missing: {rel}")
        elif path.read_text(encoding="utf-8") != text:
            problems.append(f"stale: {rel}")
    return problems


def main(argv: list[str] | None = None, root: Path = ROOT) -> int:
    parser = argparse.ArgumentParser(description=__doc__.split("\n", 1)[0])
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE, help="checkout of ThaumicTinkerer")
    parser.add_argument("--check", action="store_true", help="verify the checked-in output, write nothing")
    parser.add_argument("--allow-unpinned", action="store_true", help=f"do not require commit {SOURCE_COMMIT[:12]}")
    args = parser.parse_args(argv)
    try:
        if not args.allow_unpinned:
            verify_pin(args.source)
        plan = build(args.source, root)
    except OracleError as error:
        print(f"tt_oracle: {error}", file=sys.stderr)
        return 2
    if plan.errors:
        print(f"tt_oracle: {len(plan.errors)} constructions could not be extracted, nothing written:", file=sys.stderr)
        for message in plan.errors:
            print("  " + message, file=sys.stderr)
        return 2
    for line in plan.summary:
        print(line)
    if args.check:
        problems = check(plan, root)
        if problems:
            for p in problems:
                print(f"tt_oracle --check: {p}", file=sys.stderr)
            print("tt_oracle --check: run scripts/porting/tt_oracle.py to regenerate", file=sys.stderr)
            return 1
        print("tt_oracle --check: the checked-in oracle is current")
        return 0
    for rel, text in plan.files.items():
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        print(f"wrote {rel}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
