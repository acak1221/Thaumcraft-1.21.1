"""Regression tests reproduce exact bad requests, not merely asset counts."""
import copy
import json
import unittest
from pathlib import Path
from unittest.mock import patch

import composed_resources as composed
import convert_assets as converter


class ComposedResourcesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.contract = json.loads(composed.CONTRACT.read_text())
        cls.sprites = converter.block_atlas_sprites()

    def validate(self, contract=None, sprites=None):
        return composed.validate(converter.ROOT, converter.RESOURCE_ROOT,
                                 self.sprites if sprites is None else sprites,
                                 self.contract if contract is None else contract)

    def test_all_audited_expansions_exist_and_atlas_sprites_are_stitched(self):
        checks = self.validate()
        self.assertEqual(49, checks['checked_composed_sites'])
        self.assertEqual(288, checks['checked_composed_files'])
        self.assertEqual(23, checks['checked_composed_sprites'])
        with patch.object(composed, 'validate', side_effect=ValueError('composed guard reached')):
            with self.assertRaisesRegex(ValueError, 'composed guard reached'):
                converter.validate_resources()

    def test_all_56_baseline_missing_requests_have_existing_corrected_files(self):
        root = converter.RESOURCE_ROOT / 'assets/thaumcraft'
        for index in range(1, 48):
            request = root / f'textures/blocks/warded_glass_{index}'
            self.assertFalse(request.is_file(), str(request))
            self.assertTrue(request.with_suffix('.png').is_file())
        for tag in ('greatwood', 'obsidian', 'blaze', 'ice', 'quartz', 'bone', 'reed', 'silverwood', 'primal'):
            self.assertFalse((root / f'textures/models/wand_rod_{tag}_staff.png').is_file())
            self.assertTrue((root / f'textures/models/wand_rod_{tag}.png').is_file())

    def test_deleted_dynamic_texture_fails_even_if_models_never_reference_it(self):
        exists = Path.is_file
        with patch.object(Path, 'is_file', lambda p: False if str(p).endswith('/textures/models/ripple15.png') else exists(p)):
            with self.assertRaisesRegex(ValueError, 'Missing composed resource thaumcraft:textures/models/ripple15.png'):
                self.validate()

    def test_reaudited_research_navigation_and_both_backgrounds_stay_required(self):
        # Native geometry changes reuse these sheets; reviewing the supplier
        # contract must never make their removal an accepted new baseline.
        exists = Path.is_file
        for filename in ('gui_researchbook.png', 'gui_researchback.png', 'gui_researchbackeldritch.png'):
            suffix = '/textures/gui/' + filename
            with self.subTest(filename=filename), patch.object(
                    Path, 'is_file', lambda p, suffix=suffix: False if str(p).endswith(suffix) else exists(p)):
                with self.assertRaisesRegex(ValueError, 'Missing (?:composed|literal client) resource .*' + filename):
                    self.validate()

    def test_extensionless_warded_glass_request_is_not_mistaken_for_existing_png(self):
        contract = copy.deepcopy(self.contract)
        site = next(s for s in contract['sites'] if s['site'].endswith('/TileWardedGlassRenderer.java'))
        site['paths'] = [p.removesuffix('.png') for p in site['paths']]
        with self.assertRaisesRegex(ValueError, 'Missing composed resource thaumcraft:textures/blocks/warded_glass_1 '):
            self.validate(contract)

    def test_staff_tag_request_is_not_mistaken_for_existing_base_rod_png(self):
        contract = copy.deepcopy(self.contract)
        site = next(s for s in contract['sites'] if s['site'].endswith('/WandRod.java:66'))
        site['paths'] = ['thaumcraft:textures/models/wand_rod_primal_staff.png']
        with self.assertRaisesRegex(ValueError, 'Missing composed resource .*wand_rod_primal_staff.png'):
            self.validate(contract)

    def test_unstitched_dynamic_sprite_fails_even_when_png_exists(self):
        sprites = dict(self.sprites)
        del sprites['thaumcraft:blocks/pipe_valve']
        with self.assertRaisesRegex(ValueError, 'Unstitched composed sprite thaumcraft:blocks/pipe_valve'):
            self.validate(sprites=sprites)

    def test_literal_obj_is_checked_without_a_json_model_reference(self):
        exists = Path.is_file
        with patch.object(Path, 'is_file', lambda p: False if str(p).endswith('/textures/models/orb.obj') else exists(p)):
            with self.assertRaisesRegex(ValueError, 'Missing literal client resource .*orb.obj'):
                self.validate()

    def test_changed_composition_and_new_factory_require_review(self):
        contract = copy.deepcopy(self.contract)
        contract['dynamic_factories'].pop(next(iter(contract['dynamic_factories'])))
        with self.assertRaisesRegex(ValueError, 'Dynamic ResourceLocation inventory changed'):
            self.validate(contract)

    def test_new_helper_caller_cannot_escape_existing_factory_inventory(self):
        contract = copy.deepcopy(self.contract)
        contract['helper_calls'].pop(next(iter(contract['helper_calls'])))
        with self.assertRaisesRegex(ValueError, 'Resource helper callers changed'):
            self.validate(contract)

    def test_changed_supplier_literal_or_loop_bound_requires_review(self):
        for before, after in [('ripple', 'rippl'), ('15', '16'), (' + frame + ', ' - frame + ')]:
            path = converter.ROOT / 'common/src/main/java/thaumcraft/client/fx/particles/WarpSonicParticle.java'
            source = path.read_text()
            self.assertNotEqual(composed.source_contract(source), composed.source_contract(source.replace(before, after)))
        contract = copy.deepcopy(self.contract)
        contract['source_contracts'][str(path.relative_to(converter.ROOT))] = 'outdated'
        with self.assertRaisesRegex(ValueError, 'Resource supplier changed: .*WarpSonicParticle.java'):
            self.validate(contract)

    def test_new_corpus_icon_requires_review_instead_of_silent_skip(self):
        paths = composed.corpus_paths(converter.RESOURCE_ROOT)
        paths['GuiResearchPopup.java'].add('thaumcraft:textures/misc/absent.png')
        with patch.object(composed, 'corpus_paths', return_value=paths):
            with self.assertRaisesRegex(ValueError, 'Resource corpus changed: GuiResearchPopup.java'):
                self.validate()

    def test_balanced_factory_inventory_ignores_comments_and_corpus_strings(self):
        source = '''// ResourceLocation.parse(missing)
        String data = "new ResourceLocation(\\\"thaumcraft\\\", \\\"fake\\\")";
        ResourceLocation.fromNamespaceAndPath("thaumcraft", "textures/" + material.sheet());
        ResourceLocation.parse("thaumcraft:textures/misc/nodes.png");
        /* ResourceLocation.parse(other) */'''
        self.assertEqual(['ResourceLocation.fromNamespaceAndPath("thaumcraft","textures/"+material.sheet())'],
                         composed.dynamic_calls(source))
        self.assertEqual(composed.source_contract('int index = 1; // comment'),
                         composed.source_contract('int  index=1; /* another comment */'))


if __name__ == '__main__':
    unittest.main()
