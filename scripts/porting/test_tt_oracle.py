#!/usr/bin/env python3
"""Thaumic Tinkerer oracle: the Java reader, the interpreter, the extraction and the --check mode.

Most tests build a small synthetic checkout (a registry, a block, an item, a few constants) in a temporary
directory, so they need neither the real source checkout nor the original Thaumcraft jars. The last class replays
the extraction against the pinned checkout and the checked-in oracle; it is skipped when that checkout is absent or
at another commit.

Run like the neighbouring tests:  python3 -I -m unittest discover -s scripts/porting -p "test_tt_oracle.py"
"""
import contextlib
import hashlib
import io
import json
import re
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import tt_oracle as oracle  # noqa: E402

ASPECTS = ["AIR", "EARTH", "FIRE", "WATER", "ORDER", "ENTROPY", "VOID", "LIGHT", "WEATHER", "MOTION", "COLD", "CRYSTAL",
           "LIFE", "POISON", "ENERGY", "EXCHANGE", "METAL", "DEATH", "FLIGHT", "DARKNESS", "SOUL", "HEAL", "TRAVEL",
           "ELDRITCH", "MAGIC", "AURA", "TAINT", "SLIME", "PLANT", "TREE", "BEAST", "FLESH", "UNDEAD", "MIND", "SENSES",
           "MAN", "CROP", "MINE", "TOOL", "HARVEST", "WEAPON", "ARMOR"]
ASPECT_JAVA = "package x;\npublic class Aspect {\n" + "".join(
    f'    public static final Aspect {name} = new Aspect("{name.lower()}x", 0);\n' for name in ASPECTS) + "}\n"

LIB_ITEM_NAMES = """package x;
public final class LibItemNames {
    public static final String WAND = "port_wand";
    public static final String GEMS = "port_gem";
    public static final String[] STAGES = new String[] { "s0", "s1", "s2" };
}
"""
LIB_BLOCK_NAMES = """package x;
public final class LibBlockNames {
    public static final String BOX = "port_box";
    public static final String BOX_LIT = "port_box_lit";
}
"""
LEGACY = {"rows": [{"kind": "item", "field": "itemShard", "meta": 3, "id": "thaumcraft:earth_shard"},
                   {"kind": "block", "field": "blockMagicalLog", "meta": 0, "id": "thaumcraft:greatwood_log",
                    "item": "thaumcraft:greatwood_log"}]}

LIB_RESEARCH = """package t;
public final class LibResearch {
    public static final String CATEGORY_THAUMICTINKERER = "THAUMICTINKERER";
    public static final String KEY_BOX = "BOX";
    public static final String KEY_WAND = "WAND";
    public static final String KEY_GEMS = "GEMS";
    public static final String KEY_BOX_LIT = KEY_BOX + "_LIT";
}
"""
LIB_ITEM_NAMES_SRC = """package t;
public final class LibItemNames { public static final String WAND = "wand"; public static final String GEMS = "gems"; }
"""
LIB_BLOCK_NAMES_SRC = """package t;
public final class LibBlockNames { public static final String BOX = "box"; public static final String BOX_LIT = "boxLit"; }
"""
REGISTRY = """package t;
public class TTRegistry {
    public static Object keep;
    public void preInit() {
        keep = loadSimpleBlock(new BlockBox());
        loadMetaBlock(new BlockLamp());
        loadSimpleItem(new ItemWand());
        loadSimpleItem(new ItemGems());
    }
}
"""
HELPER = """package t;
public final class ResearchHelper {
    public static void initResearch() {
        ResearchItem research = new TTResearchItem(LibResearch.KEY_GEMS, new AspectList().add(Aspect.AIR, 1), 0, 0, 1,
                new ItemStack(Items.diamond));
        research.setPages(new ResearchPage("0")).setSecondary().registerResearchItem();
        if (LoadedMods.computerCraftLoaded) {
            research = new TTResearchItem("PERIPHERALS", new AspectList(), -1, 0, 0, new ItemStack(Items.redstone)).setAutoUnlock();
            research.setPages(new ResearchPage("0")).registerResearchItem();
        }
    }
}
"""
BLOCK_BOX = """package t;
public class BlockBox extends Block implements ITTinkererBlock {
    public String getBlockName() { return LibBlockNames.BOX; }
    public boolean shouldRegister() { return true; }
    public IRegisterableResearch getResearchItem() {
        if (!ConfigHandler.enableKami) return null;
        return (IRegisterableResearch) new KamiResearchItem(LibResearch.KEY_BOX,
                new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 3).add(Aspect.EARTH, 1), 7, 8, 5,
                new ItemStack(this)).setParents(LibResearch.KEY_WAND).setParentsHidden("X").setPages(
                        new ResearchPage("0"), ResearchHelper.infusionPage(LibResearch.KEY_BOX));
    }
    public ThaumicTinkererRecipe getRecipeItem() {
        return new ThaumicTinkererRecipeMulti(
                new ThaumicTinkererInfusionRecipe(LibResearch.KEY_BOX, new ItemStack(this), 7,
                        new AspectList().add(Aspect.MAN, 32), new ItemStack(Items.diamond),
                        new ItemStack(ConfigItems.itemShard, 1, 3), new ItemStack(ConfigItems.itemShard, 1, 3)),
                new ThaumicTinkererArcaneRecipe("BOXA", LibResearch.KEY_BOX, new ItemStack(this, 2),
                        new AspectList().add(Aspect.FIRE, 5), "SS", " D", 'S', new ItemStack(ConfigItems.itemShard, 1, 3),
                        'D', Items.diamond));
    }
}
"""
BLOCK_LAMP = """package t;
public class BlockLamp extends BlockSlab implements ITTinkererBlock {
    public BlockLamp() { this(false); }
    public BlockLamp(boolean full) { super(full, Material.rock); }
    public Block[] getMetaBlocks() { return new Block[] { new BlockLamp(true) }; }
    public String getBlockName() { return field_150004_a ? LibBlockNames.BOX_LIT : LibBlockNames.BOX; }
    public boolean shouldRegister() { return true; }
    public IRegisterableResearch getResearchItem() { return null; }
    public ThaumicTinkererRecipe getRecipeItem() {
        if (isOpaqueCube()) return null;
        return new ThaumicTinkererCraftingBenchRecipe("LAMP", new ItemStack(this, 6), "QQQ", 'Q',
                new ItemStack(ConfigBlocks.blockMagicalLog));
    }
}
"""
ITEM_WAND = """package t;
public class ItemWand extends Item implements ITTinkererItem {
    public String getItemName() { return LibItemNames.WAND; }
    public boolean shouldRegister() { return ConfigHandler.enableKami; }
    public IRegisterableResearch getResearchItem() {
        TTResearchItemMulti multi = new TTResearchItemMulti();
        TTResearchItem research = new TTResearchItem(LibResearch.KEY_WAND, new AspectList(), 1, 1, 2, new ItemStack(this));
        research.setPages(new ResearchPage("0"), ResearchHelper.arcaneRecipePage("WANDA"),
                ResearchHelper.infusionPage(LibResearch.KEY_WAND, 2));
        multi.addResearch(research);
        return multi;
    }
    public ThaumicTinkererRecipe getRecipeItem() {
        GameRegistry.addShapelessRecipe(new ItemStack(this, 9), new ItemStack(this));
        ThaumicTinkererRecipeMulti all = new ThaumicTinkererRecipeMulti();
        for (int i = 0; i < 2; i++) {
            all.addRecipe(new ThaumicTinkererInfusionRecipe(LibResearch.KEY_WAND + i, LibResearch.KEY_WAND,
                    new ItemStack(this, 1, i), 3, new AspectList().add(Aspect.AIR, 4 + i), new ItemStack(Items.diamond),
                    new ItemStack[] { new ItemStack(Items.diamond), new ItemStack(Items.diamond) }));
        }
        return all;
    }
}
"""
ITEM_GEMS = """package t;
public class ItemGems extends Item implements ITTinkererItem {
    public String getItemName() { return LibItemNames.GEMS; }
    public boolean shouldRegister() { return false; }
    public IRegisterableResearch getResearchItem() {
        return new TTResearchItem("GEMS_DEAD", new AspectList(), 5, 5, 1, new ItemStack(this));
    }
    public ThaumicTinkererRecipe getRecipeItem() { return null; }
}
"""
GAME_FILES = {
    "t/LibResearch.java": LIB_RESEARCH, "t/LibItemNames.java": LIB_ITEM_NAMES_SRC, "t/LibBlockNames.java": LIB_BLOCK_NAMES_SRC,
    "t/TTRegistry.java": REGISTRY, "t/ResearchHelper.java": HELPER, "t/BlockBox.java": BLOCK_BOX,
    "t/BlockLamp.java": BLOCK_LAMP, "t/ItemWand.java": ITEM_WAND, "t/ItemGems.java": ITEM_GEMS,
}


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def make_checkout(base: Path, files: dict | None = None) -> Path:
    source = base / "source"
    for name, text in (GAME_FILES if files is None else files).items():
        write(source / oracle.JAVA_ROOT / name, text)
    return source


def make_root(base: Path) -> Path:
    root = base / "repo"
    write(root / oracle.ASPECT_JAVA, ASPECT_JAVA)
    write(root / oracle.PORT_LIB / "LibItemNames.java", LIB_ITEM_NAMES)
    write(root / oracle.PORT_LIB / "LibBlockNames.java", LIB_BLOCK_NAMES)
    write(root / oracle.LEGACY_IDS, json.dumps(LEGACY))
    return root


class TempCase(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.base = Path(self.tmp.name)
        self.root = make_root(self.base)

    def checkout(self, **overrides):
        files = dict(GAME_FILES)
        for name, text in overrides.items():
            files[name.replace("__", "/") + ".java"] = text
        return make_checkout(self.base, files)

    def plan(self, **overrides):
        return oracle.build(self.checkout(**overrides), self.root)

    def docs(self, plan):
        rel = oracle.OUT_DIR
        return (json.loads(plan.files[f"{rel}/research.json"]), json.loads(plan.files[f"{rel}/recipes.json"]),
                json.loads(plan.files[f"{rel}/provenance.json"]))

    def run_main(self, *arguments):
        out, err = io.StringIO(), io.StringIO()
        with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
            code = oracle.main([*arguments], root=self.root)
        return code, out.getvalue(), err.getvalue()


# ----------------------------------------------------------------------------------------------- the Java reader

class LexerTest(unittest.TestCase):
    def kinds(self, text):
        return [(t.kind, t.val) for t in oracle.tokenize(text)[:-1]]

    def test_comments_strings_and_numbers(self):
        toks = self.kinds('a /* x */ = "s\\"t" + \'c\' + 0x1F + 2.5f + 3L; // end\nb')
        self.assertEqual(toks, [("id", "a"), ("op", "="), ("str", '"s\\"t"'), ("op", "+"), ("chr", "'c'"), ("op", "+"),
                                ("int", "0x1F"), ("op", "+"), ("float", "2.5f"), ("op", "+"), ("int", "3L"), ("op", ";"),
                                ("id", "b")])

    def test_line_numbers_follow_block_comments(self):
        toks = oracle.tokenize("a\n/* one\ntwo */\nb")
        self.assertEqual([(t.val, t.line) for t in toks[:-1]], [("a", 1), ("b", 4)])

    def test_shift_and_arrow_tokens(self):
        self.assertEqual([v for _, v in self.kinds("a >> b >>> c -> d :: e ... f")],
                         ["a", ">>", "b", ">>>", "c", "->", "d", "::", "e", "...", "f"])

    def test_unescape(self):
        self.assertEqual(oracle.unescape("a\\n\\u0041\\\\"), "a\nA\\")

    def test_an_unknown_character_is_an_error(self):
        with self.assertRaises(oracle.ParseError):
            oracle.tokenize("a # b")


def parse_expr(text):
    return oracle.Parser(oracle.tokenize(text), "t").expr()


def parse_block(text):
    return oracle.Parser(oracle.tokenize(text), "t").block()


class ParserTest(unittest.TestCase):
    def test_builder_chain_with_a_cast(self):
        node = parse_expr("(TTResearchItem) new TTResearchItem(K, tags, 1, 2, 3, icon).setParents(A, B).setSecondary()")
        self.assertEqual(node[0], "cast")
        self.assertEqual(oracle.unparse(node),
                         "(TTResearchItem) new TTResearchItem(K, tags, 1, 2, 3, icon).setParents(A, B).setSecondary()")

    def test_parenthesised_cast_receiver(self):
        node = parse_expr("((TTResearchItem) research).setPages(new ResearchPage(\"0\"))")
        self.assertEqual(node[0], "call")
        self.assertEqual(node[2], "setPages")

    def test_precedence_and_ternary(self):
        node = parse_expr("a + b * c == d ? e : f")
        self.assertEqual(node[0], "cond")
        self.assertEqual(node[1][0:2], ("binary", "=="))
        self.assertEqual(node[1][2][1], "+")

    def test_array_creation_with_an_initializer(self):
        node = parse_expr("new ItemStack[] { a, new ItemStack(b, 1, 2) }")
        self.assertEqual(node[0], "newarray")
        self.assertEqual(len(node[3][1]), 2)

    def test_generics_do_not_confuse_the_declaration_detector(self):
        block = parse_block("{ Map<String, List<String>> m = new HashMap<>(); a < b; }")
        self.assertEqual(block[1][0][0], "local")

    def test_negative_literals_and_chars(self):
        self.assertEqual(parse_expr("-3"), ("lit", -3, 1))
        self.assertEqual(parse_expr("'I'")[0], "char")

    def test_class_literal_and_static_call(self):
        self.assertEqual(oracle.unparse(parse_expr("ThaumicTinkerer.registry.getFirstItemFromClass(ItemX.class)")),
                         "ThaumicTinkerer.registry.getFirstItemFromClass(ItemX.class)")

    def test_statements(self):
        block = parse_block("{ for (int i = 0; i < 3; i++) { x += i; } switch (t) { case 0: return a; default: break; } "
                            "if (c) return; else x = 1; for (Foo f : list) {} }")
        self.assertEqual([s[0] for s in block[1]], ["for", "switch", "if", "foreach"])

    def test_lambdas_are_refused(self):
        with self.assertRaises(oracle.ParseError):
            parse_expr("list.forEach(x -> x + 1)")

    def test_a_missing_semicolon_is_a_parse_error_with_the_line(self):
        with self.assertRaises(oracle.ParseError) as ctx:
            parse_block("{ a = 1\n b = 2; }")
        self.assertIn("t:2", str(ctx.exception))


class ScannerTest(unittest.TestCase):
    SOURCE = """package p;
import a.b.C;
/** doc */
public abstract class Foo<T extends Bar> extends Base implements One, Two {
    public static final String A = "x", B = A + "y";
    private int[] arr = new int[] { 1, 2 };
    @Override public String name() { return "n"; }
    public Foo(int a) { this(a, 1); }
    public Foo(int a, int b) { super(a); }
    static <U> Map<String, List<U>> gen(List<U> in, int... rest) throws Exception { return null; }
    abstract void nobody();
    static { init(); }
    enum Mode { ONE, TWO(2); Mode() {} Mode(int x) {} }
    class Inner { void m() {} }
}
"""

    def test_members(self):
        classes = {}
        oracle.scan_file("p/Foo.java", self.SOURCE, classes)
        foo = classes["Foo"]
        self.assertEqual(foo.extends, "Base")
        self.assertEqual(foo.implements, ["One", "Two"])
        self.assertEqual(sorted(foo.fields), ["A", "B", "arr"])
        self.assertTrue(foo.fields["A"].static)
        self.assertEqual([len(c.params) for c in foo.ctors], [1, 2])
        self.assertEqual(sorted(foo.methods), ["gen", "name", "nobody"])
        gen = foo.methods["gen"][0]
        self.assertTrue(gen.varargs)
        self.assertEqual(gen.params, ["in", "rest"])
        self.assertIsNone(foo.methods["nobody"][0].start)
        self.assertEqual(sorted(foo.nested), ["Inner", "Mode"])
        self.assertEqual(foo.nested["Mode"].enum_constants, ["ONE", "TWO"])
        self.assertEqual(len(foo.nested["Mode"].ctors), 2)
        self.assertIn("Inner", classes)

    def test_unbalanced_source_is_an_error(self):
        with self.assertRaises(oracle.OracleError):
            oracle.scan_file("p/X.java", "class X { void m() { ", {})


# ----------------------------------------------------------------------------------------------- the interpreter

def interp_for(source: str, **extra):
    classes: dict = {}
    oracle.scan_file("t/X.java", source, classes)
    for name, text in extra.items():
        oracle.scan_file(f"t/{name}.java", text, classes)
    return oracle.Interp(classes, {name: name.lower() + "x" for name in ASPECTS}), classes


def run_method(source: str, method="run", args=(), **extra):
    interp, classes = interp_for(source, **extra)
    cls = classes["X"]
    m = interp.pick(interp.find_methods(cls, method), len(args))
    ctx = oracle.Ctx(cls, None, dict(zip(m.params, args)))
    return interp, [(val, c) for c, sig, val in interp.run_stmts(interp.body_ast(m)[1], ctx) if sig == "return"]


class InterpreterTest(unittest.TestCase):
    def test_arithmetic_strings_and_constants(self):
        _, out = run_method("""class X { static final String K = "a"; static final int N = 3;
            static String run() { return K + N * 2 + (7 % 4) + 'c'; } }""")
        self.assertEqual(out[0][0], "a63c")

    def test_a_loop_builds_a_list(self):
        _, out = run_method("""class X { static Object run() { ArrayList l = new ArrayList();
            for (int i = 0; i < 4; i++) { if (i == 2) continue; l.add(i * i); } return l; } }""")
        self.assertEqual(out[0][0], [0, 1, 9])

    def test_switch_with_fall_through_and_default(self):
        _, out = run_method("""class X { static int run(int t) { switch (t) { case 0: case 1: return 10; case 2: return 20;
            default: return 99; } } }""", args=(1,))
        self.assertEqual(out[0][0], 10)
        _, out = run_method("""class X { static int run(int t) { switch (t) { case 0: return 1; default: return 99; } } }""", args=(5,))
        self.assertEqual(out[0][0], 99)

    def test_a_configuration_flag_forks_the_path_and_remembers_the_condition(self):
        _, out = run_method("""class X { static Object run() { if (!ConfigHandler.enableKami) return null;
            if (LoadedMods.computerCraftLoaded) { return "cc"; } return "plain"; } }""")
        by = {tuple((k, v[1]) for k, v in c.conds.items()): val for val, c in out}
        self.assertEqual(by[(("ConfigHandler.enableKami", False),)], None)
        self.assertEqual(by[(("ConfigHandler.enableKami", True), ("LoadedMods.computerCraftLoaded", True))], "cc")
        self.assertEqual(by[(("ConfigHandler.enableKami", True), ("LoadedMods.computerCraftLoaded", False))], "plain")

    def test_the_same_flag_twice_does_not_fork_twice(self):
        _, out = run_method("""class X { static Object run() { if (Config.allowMirrors) { a(); } if (Config.allowMirrors) { return 1; }
            return 2; } static void a() {} }""")
        # two paths only (the flag is known after the first `if`), and the one where it holds returns 1
        self.assertEqual(len(out), 2)
        self.assertEqual({val for val, c in out if c.conds["Config.allowMirrors"][1]}, {1})
        self.assertEqual({val for val, c in out if not c.conds["Config.allowMirrors"][1]}, {2})

    def test_a_flag_in_a_condition_that_cannot_be_forked_is_an_error(self):
        interp, classes = interp_for("class X { static Object run() { return ConfigHandler.a ? 1 : 2; } }")
        m = interp.find_methods(classes["X"], "run")[0]
        with self.assertRaises(oracle.Unsupported):
            interp.run_stmts(interp.body_ast(m)[1], oracle.Ctx(classes["X"], None))

    def test_a_user_method_is_interpreted(self):
        _, out = run_method("""class X { static int twice(int a) { return a + a; } static int run() { return twice(twice(3)); } }""")
        self.assertEqual(out[0][0], 12)

    def test_unknown_calls_stay_symbolic(self):
        _, out = run_method("class X { static Object run() { return Mystery.call(1).next(); } }")
        self.assertIsInstance(out[0][0], oracle.Sym)

    def test_an_aspect_list_merges_equal_aspects_in_order(self):
        _, out = run_method("""class X { static Object run() { return new AspectList().add(Aspect.EARTH, 2).add(Aspect.FIRE, 3)
            .add(Aspect.EARTH, 1); } }""")
        self.assertEqual(out[0][0].as_pairs(), [["earthx", 3], ["firex", 3]])

    def test_an_unknown_aspect_is_an_error(self):
        with self.assertRaises(oracle.Unsupported):
            run_method("class X { static Object run() { return new AspectList().add(Aspect.NOPE, 1); } }")

    def test_itemstack_arguments(self):
        _, out = run_method("""class X { static Object run() { return new ItemStack(Items.diamond, 3, 4); } }""")
        stack = out[0][0]
        self.assertEqual((stack.item.ref, stack.count, stack.meta, stack.text), ("Items.diamond", 3, 4,
                                                                                  "new ItemStack(Items.diamond, 3, 4)"))

    def test_instances_take_constructor_arguments_and_super_fields(self):
        source = """class X { static Object run() { return new Piece(2); } }"""
        piece = "class Piece extends ItemArmor { Piece(int t) { super(M, 0, t); } }"
        interp, out = run_method(source, Piece=piece)
        self.assertEqual(out[0][0].fields["armorType"], 2)


class ResearchSemanticsTest(unittest.TestCase):
    def research(self, chain: str):
        _, out = run_method(f"""class X {{ static Object run() {{ return new TTResearchItem("K", new AspectList(), 1, 2, 9,
            new ItemStack(Items.diamond)){chain}; }} }}""")
        return out[0][0]

    def test_an_infusion_page_prepends_the_infusion_research_to_the_hidden_parents(self):
        r = self.research('.setParentsHidden("A").setPages(ResearchHelper.infusionPage("K"), new ResearchPage("0"))')
        self.assertEqual(r.parents_hidden, ["INFUSION", "A"])

    def test_every_infusion_page_prepends_once_more(self):
        r = self.research('.setPages(ResearchHelper.infusionPage("K"), ResearchHelper.infusionPage("L"))')
        self.assertEqual(r.parents_hidden, ["INFUSION", "INFUSION"])

    def test_hidden_parents_set_after_the_pages_replace_them(self):
        r = self.research('.setPages(ResearchHelper.infusionPage("K")).setParentsHidden("A")')
        self.assertEqual(r.parents_hidden, ["A"])

    def test_kami_research_is_concealed_and_collects_hidden_parents_unless_auto_unlocked(self):
        _, out = run_method("""class X { static Object run() {
            ResearchItem a = new KamiResearchItem("A", new AspectList(), 1, 2, 3, new ItemStack(Items.diamond));
            a.setPages(new ResearchPage("0"));
            ResearchItem b = new KamiResearchItem("B", new AspectList(), 1, 3, 3, new ItemStack(Items.diamond)).setAutoUnlock();
            b.setPages(ResearchHelper.infusionPage("B"));
            return Arrays.asList(a, b); } }""")
        a, b = out[0][0]
        self.assertTrue(a.flags["concealed"])
        self.assertTrue(a.kami_auto_hidden)
        self.assertFalse(b.kami_auto_hidden)
        self.assertEqual(b.parents_hidden, [])        # a Kami entry never gets the infusion requirement

    def test_page_kinds(self):
        r = self.research('.setPages(new ResearchPage("0"), new ResearchPage("R", "t"), ResearchHelper.crucibleRecipePage("C"), '
                          'ResearchHelper.infusionPage("I", 3), ResearchHelper.recipePage("N"), ResearchHelper.arcaneRecipePage("A"))')
        self.assertEqual([p.kind for p in r.pages], ["text", "text_concealed", "crucible", "infusion", "normal", "arcane"])
        self.assertEqual(r.pages[3].refs, ["I0", "I1", "I2"])

    def test_a_page_with_a_non_constant_name_is_an_error(self):
        with self.assertRaises(oracle.Unsupported):
            self.research('.setPages(ResearchHelper.arcaneRecipePage(Mystery.name()))')

    def test_complexity_is_clamped_in_the_document(self):
        self.assertEqual(oracle.clamp_complexity(9), 3)
        self.assertEqual(oracle.clamp_complexity(0), 1)
        self.assertEqual(oracle.clamp_complexity(2), 2)

    def test_scan_triggers_are_refused_not_ignored(self):
        with self.assertRaises(oracle.Unsupported):
            self.research(".setItemTriggers(new ItemStack(Items.diamond))")


class RecipeSemanticsTest(unittest.TestCase):
    def recipe(self, expr):
        _, out = run_method(f"class X {{ static Object run() {{ return {expr}; }} }}")
        return out[0][0]

    def test_shaped_arcane_recipe(self):
        r = self.recipe('new ThaumicTinkererArcaneRecipe("N", "R", new ItemStack(Items.diamond, 2), new AspectList().add(Aspect.AIR, 5), '
                        '"A ", " B", \'A\', Items.diamond, \'B\', "ingotX")')
        self.assertEqual((r.kind, r.name, r.research), ("arcane_shaped", "N", "R"))
        rows, key = oracle.split_pattern(r.stuff, "t")
        self.assertEqual(rows, ["A ", " B"])
        self.assertEqual(sorted(key), ["A", "B"])

    def test_infusion_forms(self):
        short = self.recipe('new ThaumicTinkererInfusionRecipe("N", new ItemStack(Items.diamond), 7, new AspectList(), '
                            'new ItemStack(Items.coal), new ItemStack(Items.coal))')
        self.assertEqual((short.research, short.instability, len(short.stuff)), ("N", 7, 1))
        long = self.recipe('new ThaumicTinkererInfusionRecipe("N", "R", new ItemStack(Items.diamond), 9, new AspectList(), '
                           'new ItemStack(Items.coal), new ItemStack[] { new ItemStack(Items.coal), new ItemStack(Items.coal) })')
        self.assertEqual((long.research, long.instability, len(long.stuff), len(long.stuff_texts)), ("R", 9, 2, 2))

    def test_crucible_uses_its_name_as_the_research(self):
        r = self.recipe('new ThaumicTinkererCrucibleRecipe("C", new ItemStack(Items.diamond), new ItemStack(Items.coal), new AspectList())')
        self.assertEqual((r.kind, r.research), ("crucible", "C"))

    def test_a_multi_flattens(self):
        r = self.recipe('new ThaumicTinkererRecipeMulti(new ThaumicTinkererCrucibleRecipe("C", new ItemStack(Items.diamond), '
                        'new ItemStack(Items.coal), new AspectList()))')
        flat = oracle.Interp.flatten(r, [], oracle.RecipeV)
        self.assertEqual([x.name for x in flat], ["C"])

    def test_a_pattern_without_rows_is_refused(self):
        with self.assertRaises(oracle.Unsupported):
            oracle.split_pattern([oracle.Char("A"), "x"], "t")

    def test_ore_dict_or_stack(self):
        r = self.recipe('RecipeHelper.oreDictOrStack(new ItemStack(Items.gold_ingot), "ingotCopper")')
        self.assertEqual((r.ore, r.stack.item.ref), ("ingotCopper", "Items.gold_ingot"))


# ----------------------------------------------------------------------------------------------- the extraction

class MergeTest(unittest.TestCase):
    def test_complementary_conditions_merge_and_different_content_does_not(self):
        base = {"key": "A", "pages": [1]}
        on = {**base, "when": [{"expr": "LoadedMods.x", "kind": "mod", "value": True}]}
        off = {**base, "when": [{"expr": "LoadedMods.x", "kind": "mod", "value": False}]}
        other = {"key": "A", "pages": [2], "when": [{"expr": "LoadedMods.x", "kind": "mod", "value": True}]}
        merged = oracle.merge_complementary([on, off])
        self.assertEqual(len(merged), 1)
        self.assertEqual(merged[0]["when"], [])
        self.assertEqual(len(oracle.merge_complementary([other, off])), 2)

    def test_two_conditions_merge_one_at_a_time(self):
        def entry(a, b):
            return {"key": "A", "when": [{"expr": "a", "kind": "config", "value": a}, {"expr": "b", "kind": "config", "value": b}]}
        merged = oracle.merge_complementary([entry(True, True), entry(True, False), entry(False, True), entry(False, False)])
        self.assertEqual([m["when"] for m in merged], [[]])


class ExtractionTest(TempCase):
    def test_research_of_the_synthetic_checkout(self):
        plan = self.plan()
        self.assertEqual(plan.errors, [])
        research, recipes, prov = self.docs(plan)
        by_key = {e["key"]: e for e in research["entries"]}
        self.assertEqual(sorted(by_key), ["BOX", "GEMS", "GEMS_DEAD", "PERIPHERALS", "WAND"])
        box = by_key["BOX"]
        self.assertEqual(box["aspects"], {"earthx": 3, "firex": 3})            # added twice, merged, order kept
        self.assertEqual((box["column"], box["row"], box["complexitySource"], box["complexity"]), (7, 8, 5, 3))
        self.assertTrue(box["kami"] and box["flags"]["concealed"] and box["parentsHiddenAuto"])
        self.assertEqual(box["parents"], ["WAND"])
        self.assertEqual(box["parentsHidden"], ["X"])                          # a Kami entry gets no infusion requirement
        self.assertEqual([p["kind"] for p in box["pages"]], ["text", "infusion"])
        self.assertEqual(box["when"], [{"expr": "ConfigHandler.enableKami", "kind": "config", "value": True}])
        self.assertEqual(box["icon"]["resolved"], "thaumictinkerer:port_box")  # getBlockName() -> LibBlockNames.BOX -> port value
        self.assertEqual(box["owner"], "BlockBox")
        self.assertRegex(box["source"], r"^src/main/java/t/BlockBox\.java:\d+$")
        self.assertEqual(by_key["PERIPHERALS"]["when"], [{"expr": "LoadedMods.computerCraftLoaded", "kind": "mod", "value": True}])
        self.assertEqual(by_key["GEMS"]["when"], [])
        self.assertTrue(by_key["GEMS"]["flags"]["secondary"])
        wand = by_key["WAND"]
        self.assertEqual(wand["parentsHidden"], ["INFUSION"])
        self.assertEqual([p["kind"] for p in wand["pages"]], ["text", "arcane", "infusion"])
        self.assertEqual(wand["pages"][2]["recipes"], ["WAND0", "WAND1"])
        self.assertEqual(wand["when"], [{"expr": "ConfigHandler.enableKami", "kind": "config", "value": True}])

    def test_a_research_whose_owner_never_registers_is_kept_but_marked(self):
        research, _, _ = self.docs(self.plan())
        dead = [e for e in research["entries"] if e["key"] == "GEMS_DEAD"][0]
        self.assertFalse(dead["registered"])
        self.assertTrue(all(e["registered"] for e in research["entries"] if e["key"] != "GEMS_DEAD"))

    def test_recipes_of_the_synthetic_checkout(self):
        _, recipes, prov = self.docs(self.plan())
        by_name = {}
        for e in recipes["entries"]:
            by_name.setdefault(e["name"], []).append(e)
        infusion = by_name["BOX"][0]
        self.assertEqual((infusion["kind"], infusion["research"], infusion["instability"], infusion["ingredientCount"]),
                         ("infusion", "BOX", 7, 2))
        self.assertEqual(infusion["input"]["resolved"], "minecraft:diamond")
        self.assertEqual([c["resolved"] for c in infusion["components"]], ["thaumcraft:earth_shard"] * 2)
        arcane = by_name["BOXA"][0]
        self.assertEqual((arcane["pattern"], arcane["ingredientCount"], arcane["output"]["count"]), (["SS", " D"], 3, 2))
        self.assertEqual(arcane["key"]["D"]["resolved"], "minecraft:diamond")
        lamp = by_name["LAMP"]
        self.assertEqual(len(lamp), 1)                                          # only the single slab has the recipe
        self.assertEqual(lamp[0]["output"]["resolved"], "thaumictinkerer:port_box")
        self.assertEqual(lamp[0]["output"]["count"], 6)
        self.assertEqual(lamp[0]["key"]["Q"]["resolved"], "thaumcraft:greatwood_log")
        loop = sorted(by_name["WAND0"] + by_name["WAND1"], key=lambda e: e["name"])
        self.assertEqual([e["aspects"] for e in loop], [{"airx": 4}, {"airx": 5}])
        self.assertEqual([e["output"]["meta"] for e in loop], [0, 1])
        shapeless = by_name[None][0]
        self.assertEqual((shapeless["kind"], shapeless["output"]["count"], shapeless["ingredientCount"]), ("crafting_shapeless", 9, 1))
        self.assertEqual(shapeless["when"], [{"expr": "ConfigHandler.enableKami", "kind": "config", "value": True}])
        self.assertEqual(prov["recipe_names_registered_more_than_once"], [])

    def test_meta_instances_are_separate_registrations(self):
        interp_plan = self.plan()
        _, recipes, _ = self.docs(interp_plan)
        self.assertEqual(sum(1 for e in recipes["entries"] if e["name"] == "LAMP"), 1)

    def test_unresolved_expressions_are_listed_not_guessed(self):
        plan = self.plan(t__ItemWand=ITEM_WAND.replace("new ItemStack(Items.diamond), new ItemStack(Items.diamond) }",
                                                       "new ItemStack(Items.nothing_known, 1, 5), new ItemStack(Items.diamond) }"))
        _, _, prov = self.docs(plan)
        reasons = [u["reason"] for u in prov["unresolved_items"]]
        self.assertTrue(any("Items.nothing_known" in r and "VANILLA" in r for r in reasons))

    def test_provenance_hashes_every_source_file_read(self):
        source = self.checkout()
        plan = oracle.build(source, self.root)
        _, _, prov = self.docs(plan)
        for rel, digest in prov["sources"].items():
            self.assertEqual(hashlib.sha256((source / rel).read_bytes()).hexdigest(), digest)
        self.assertEqual(sorted(prov["sources"]), sorted(p.relative_to(source).as_posix() for p in source.rglob("*.java")))
        self.assertEqual(set(prov["inputs"]), {oracle.ASPECT_JAVA, oracle.LEGACY_IDS, f"{oracle.PORT_LIB}/LibItemNames.java",
                                               f"{oracle.PORT_LIB}/LibBlockNames.java"})
        self.assertEqual(prov["totals"]["research_sites"], 5)
        self.assertEqual(prov["constructions"]["src/main/java/t/ResearchHelper.java"]["research"], 2)

    def test_the_output_is_deterministic(self):
        first = self.plan()
        second = oracle.build(self.checkout(), self.root)
        self.assertEqual(first.files, second.files)

    def test_a_construction_that_is_never_reached_is_an_error(self):
        plan = self.plan(t__ItemGems=ITEM_GEMS.replace("public ThaumicTinkererRecipe getRecipeItem() { return null; }",
                         "public ThaumicTinkererRecipe getRecipeItem() { return null; }\n"
                         "    void orphan() { new ThaumicTinkererCrucibleRecipe(\"O\", new ItemStack(this), new ItemStack(this), new AspectList()); }"))
        self.assertTrue(plan.errors)
        self.assertRegex(plan.errors[0], r"ItemGems\.java:\d+: ThaumicTinkererCrucibleRecipe construction not understood or not reached")
        self.assertEqual(plan.files, {})

    def test_a_construction_the_interpreter_cannot_read_is_an_error(self):
        broken = BLOCK_BOX.replace("new AspectList().add(Aspect.MAN, 32)", "new AspectList().add(Aspect.NO_SUCH, 32)")
        plan = self.plan(t__BlockBox=broken)
        self.assertTrue(plan.errors)
        self.assertIn("NO_SUCH", plan.errors[0])

    def test_a_lambda_in_a_registration_method_is_an_error(self):
        broken = ITEM_GEMS.replace("return null; }\n}", "list.forEach(x -> x); return null; }\n}")
        plan = self.plan(t__ItemGems=broken)
        self.assertTrue(plan.errors)

    def test_research_registered_by_a_helper_chain_is_found(self):
        helper = HELPER.replace("research.setPages(new ResearchPage(\"0\")).setSecondary().registerResearchItem();",
                                "research.setPages(new ResearchPage(\"0\")).setSecondary();")
        plan = self.plan(t__ResearchHelper=helper)
        research, _, _ = self.docs(plan)
        self.assertNotIn("GEMS", [e["key"] for e in research["entries"]])      # created but never registered: not in the oracle


class CliTest(TempCase):
    def setUp(self):
        super().setUp()
        self.source = self.checkout()

    def args(self, *extra):
        return ["--source", str(self.source), "--allow-unpinned", *extra]

    def test_write_then_check(self):
        code, out, err = self.run_main(*self.args())
        self.assertEqual(code, 0, err)
        for name in ("research.json", "recipes.json", "provenance.json"):
            self.assertTrue((self.root / oracle.OUT_DIR / name).is_file())
        code, out, err = self.run_main(*self.args("--check"))
        self.assertEqual(code, 0, err)
        self.assertIn("current", out)

    def test_check_detects_a_stale_file_and_writes_nothing(self):
        self.run_main(*self.args())
        path = self.root / oracle.OUT_DIR / "research.json"
        path.write_text(path.read_text().replace('"column": 7', '"column": 8'), encoding="utf-8")
        stale = path.read_text()
        code, out, err = self.run_main(*self.args("--check"))
        self.assertEqual(code, 1)
        self.assertIn("stale: " + oracle.OUT_DIR + "/research.json", err)
        self.assertEqual(path.read_text(), stale)

    def test_check_detects_a_missing_file(self):
        self.run_main(*self.args())
        (self.root / oracle.OUT_DIR / "recipes.json").unlink()
        code, _, err = self.run_main(*self.args("--check"))
        self.assertEqual(code, 1)
        self.assertIn("missing: ", err)

    def test_check_fails_when_the_source_changed(self):
        self.run_main(*self.args())
        block = self.source / oracle.JAVA_ROOT / "t/BlockBox.java"
        block.write_text(block.read_text().replace("7, 8, 5", "7, 9, 5"), encoding="utf-8")
        code, _, err = self.run_main(*self.args("--check"))
        self.assertEqual(code, 1)

    def test_extraction_errors_exit_with_2_and_write_nothing(self):
        block = self.source / oracle.JAVA_ROOT / "t/ItemGems.java"
        block.write_text(block.read_text().replace("return null; }\n}", "return null; }\n    void x() { new TTResearchItem(\"Z\", null, 1, 1, 1, null); }\n}", 1), encoding="utf-8")
        code, _, err = self.run_main(*self.args())
        self.assertEqual(code, 2)
        self.assertIn("could not be extracted", err)
        self.assertFalse((self.root / oracle.OUT_DIR).exists())

    def test_the_pin_is_enforced(self):
        subprocess.run(["git", "init", "-q", str(self.source)], check=True)
        subprocess.run(["git", "-C", str(self.source), "-c", "user.email=t@t", "-c", "user.name=t", "commit", "-q",
                        "--allow-empty", "-m", "x"], check=True)
        code, _, err = self.run_main("--source", str(self.source))
        self.assertEqual(code, 2)
        self.assertIn(oracle.SOURCE_COMMIT, err)

    def test_a_missing_checkout_is_an_error(self):
        code, _, err = self.run_main("--source", str(self.base / "nowhere"), "--allow-unpinned")
        self.assertEqual(code, 2)


# ----------------------------------------------------------------------------------------------- the real checkout

def pinned_checkout() -> Path | None:
    source = oracle.DEFAULT_SOURCE
    if not (source / oracle.JAVA_ROOT).is_dir():
        return None
    try:
        head = subprocess.run(["git", "-C", str(source), "rev-parse", "HEAD"], capture_output=True, text=True, check=True).stdout.strip()
    except (OSError, subprocess.CalledProcessError):
        return None
    return source if head == oracle.SOURCE_COMMIT else None


@unittest.skipIf(pinned_checkout() is None, "the pinned Thaumic Tinkerer checkout is not available")
class RealCheckoutTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = pinned_checkout()
        cls.plan = oracle.build(cls.source, oracle.ROOT)
        cls.research = json.loads(cls.plan.files[f"{oracle.OUT_DIR}/research.json"]) if cls.plan.files else None
        cls.recipes = json.loads(cls.plan.files[f"{oracle.OUT_DIR}/recipes.json"]) if cls.plan.files else None
        cls.prov = json.loads(cls.plan.files[f"{oracle.OUT_DIR}/provenance.json"]) if cls.plan.files else None

    def test_every_construction_is_understood(self):
        self.assertEqual(self.plan.errors, [])
        self.assertEqual(self.prov["totals"]["source_files"], 334)

    def test_the_counts_match_an_independent_count_of_the_source(self):
        text = {p: p.read_text(encoding="utf-8") for p in (self.source / oracle.JAVA_ROOT).rglob("*.java")}
        research_sites = sum(len(re.findall(r"new\s+(?:TTResearchItem|KamiResearchItem)\s*\(", t))
                             for p, t in text.items() if p.name not in ("TTResearchItem.java", "KamiResearchItem.java"))
        recipe_sites = sum(len(re.findall(r"new\s+ThaumicTinkerer(?:Arcane|Crucible|Infusion|CraftingBench)Recipe\s*\(", t))
                           + len(re.findall(r"GameRegistry\s*\.\s*add\w*Recipe", t))
                           + len(re.findall(r"getRecipeList\s*\(\s*\)\s*\.\s*add\s*\(", t))
                           for p, t in text.items() if not p.name.startswith("ThaumicTinkerer"))
        self.assertEqual(self.prov["totals"]["research_sites"], research_sites)
        self.assertEqual(self.prov["totals"]["recipe_sites"], recipe_sites)
        self.assertEqual((research_sites, recipe_sites), (79, 98))

    def test_the_research_keys_equal_the_keys_written_in_the_source(self):
        constants = dict(re.findall(r'String\s+(KEY_\w+)\s*=\s*"([^"]+)"',
                                    (self.source / oracle.JAVA_ROOT / "thaumic/tinkerer/common/lib/LibResearch.java").read_text()))
        keys = set()
        for path in (self.source / oracle.JAVA_ROOT).rglob("*.java"):
            if path.name in ("TTResearchItem.java", "KamiResearchItem.java"):
                continue
            for first in re.findall(r"new\s+(?:TTResearchItem|KamiResearchItem)\s*\(\s*([A-Za-z_.\"]+)", path.read_text()):
                keys.add(constants[first.split(".")[-1]] if first.startswith("LibResearch") else first.strip('"'))
        self.assertEqual({e["key"] for e in self.research["entries"]}, keys)
        self.assertEqual(len(keys), 78)

    def test_recipe_numbers(self):
        kinds = self.prov["totals"]["recipe_kinds"]
        self.assertEqual(sum(kinds.values()), 130)
        self.assertEqual(kinds["infusion"], 55)
        self.assertEqual(kinds["crafting_custom"], 1)            # SpellClothRecipe, added to CraftingManager's list

    def test_known_facts_of_the_original(self):
        by_key = {}
        for e in self.research["entries"]:
            by_key.setdefault(e["key"], []).append(e)
        enchanter = by_key["ENCHANTER"][0]
        self.assertEqual((enchanter["column"], enchanter["row"], enchanter["complexitySource"], enchanter["complexity"]), (5, 4, 5, 3))
        self.assertEqual(list(enchanter["aspects"].items()), [("praecantatio", 2), ("auram", 1), ("alienis", 1), ("tenebrae", 1), ("cognitio", 1)])
        self.assertEqual(enchanter["parents"], ["SPELL_CLOTH"])
        self.assertEqual(enchanter["parentsHidden"], ["INFUSION"])
        self.assertEqual(len(by_key["ICHOR"]), 1)
        self.assertEqual(by_key["ICHOR"][0]["warp"], 5)
        self.assertTrue(by_key["ICHOR"][0]["parentsHiddenAuto"])
        self.assertFalse(by_key["DIMENSION_SHARDS"][0]["parentsHiddenAuto"])      # auto-unlocked before setPages
        self.assertEqual({e["pages"][0]["key"] for e in by_key["FOCUS_ENDER_CHEST"]}, {"0", "ES"})
        self.assertFalse(by_key["REMOTE_PLACER"][0]["registered"])
        enchant = by_key["TTENCH_FINAL"][0]
        self.assertEqual((enchant["column"], enchant["row"], enchant["parents"], enchant["flags"]["secondary"]),
                         (0, 8, ["TTENCH_FOCUSED", "TTENCH_DISPERSED"], True))
        self.assertEqual(enchant["when"], [])                                       # the ComputerCraft fork after it does not apply

    def test_the_category_is_the_constant_of_the_original_and_unnamed_recipes_have_no_name(self):
        self.assertEqual({e["category"] for e in self.research["entries"]}, {"TT_CATEGORY"})
        unnamed = [e for e in self.recipes["entries"] if e["name"] is None]
        self.assertEqual(sorted(e["kind"] for e in unnamed), ["crafting_custom", "crafting_shaped", "crafting_shapeless"])
        custom = [e for e in unnamed if e["kind"] == "crafting_custom"][0]
        self.assertEqual((custom["recipeClass"], custom["output"], custom["owner"]), ("SpellClothRecipe", None, "ItemSpellCloth"))

    def test_the_summon_loop_is_unrolled(self):
        summon = [e for e in self.recipes["entries"] if e["name"] == "SUMMON"]
        condense = [e for e in self.recipes["entries"] if e["name"] == "SUMMON1"]
        self.assertEqual((len(summon), len(condense)), (17, 17))
        self.assertEqual(summon[0]["instability"], 4)
        self.assertTrue(all(e["output"]["resolved"] and "mob_aspect" in e["output"]["resolved"] for e in summon + condense))

    def test_nothing_is_left_unresolved(self):
        self.assertEqual(self.prov["unresolved_items"], [])

    def test_no_page_refers_to_a_missing_recipe(self):
        self.assertEqual(self.prov["page_references_without_recipe"], [])

    def test_the_checked_in_oracle_is_current(self):
        for rel, text in self.plan.files.items():
            path = oracle.ROOT / rel
            self.assertTrue(path.is_file(), f"{rel} is not checked in")
            self.assertEqual(path.read_text(encoding="utf-8"), text, f"{rel} is stale: run scripts/porting/tt_oracle.py")


if __name__ == "__main__":
    unittest.main()
