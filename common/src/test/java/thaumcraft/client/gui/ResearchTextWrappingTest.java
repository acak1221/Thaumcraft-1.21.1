package thaumcraft.client.gui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.resources.language.FormattedBidiReorder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

class ResearchTextWrappingTest {
    // Real native splitting/reordering, with deterministic normal/bold advances. The extra
    // bold pixel matches GlyphInfo.getAdvance(boolean), independent of a graphics context.
    private static final StringSplitter SPLITTER = new StringSplitter((codePoint, style) ->
            (codePoint == ' ' ? 4F : 6F) + (style.isBold() ? 1F : 0F));
    private static final String VISPOWER = "§lУзел-преобразователь§r над узлом. При подаче сигнала красного камня к преобразователю узел начнет меняться";

    @Test void aLegacyResetAfterAWrappedBoldWordDoesNotBleedIntoTheNextColumn() {
        for (int width : new int[]{128, 139}) {
            var oldLines = SPLITTER.splitLines(Component.literal(VISPOWER), width, Style.EMPTY);
            assertTrue(renderedStyles(oldLines).stream().filter(glyph -> glyph.text().equals("н")).findFirst().orElseThrow().bold(),
                    "native raw-code control carries bold past the explicit reset");
            if (width == 139) assertTrue(oldLines.stream().map(ResearchTextWrappingTest::visual)
                    .anyMatch(line -> SPLITTER.stringWidth(line) > width),
                    "the raw-code continuation overflows the actual book column");
            var lines = SPLITTER.splitLines(ResearchText.formatted(VISPOWER), width, Style.EMPTY);
            assertTrue(lines.stream().map(ResearchTextWrappingTest::visual).allMatch(line -> SPLITTER.stringWidth(line) <= width),
                    "every rendered line fits its measured column at " + width);
            assertFalse(renderedStyles(lines).stream().filter(glyph -> glyph.text().equals("н")).findFirst().orElseThrow().bold(),
                    "text after §r returns to the default even after the bold word wrapped");
        }
    }

    @Test void colorsResetBoldAndExplicitResetRestoresTheOriginalDefault() {
        var text = ResearchText.formatted("§lА§cБ§oВ§rГ");
        var glyphs = renderedStyles(List.of(text));
        assertEquals(List.of("А", "Б", "В", "Г"), glyphs.stream().map(Glyph::text).toList());
        assertTrue(glyphs.get(0).bold());
        assertFalse(glyphs.get(1).bold(), "legacy colors clear previous formatting");
        assertTrue(glyphs.get(2).italic());
        assertFalse(glyphs.get(3).bold());
        assertFalse(glyphs.get(3).italic());
        assertNull(glyphs.get(3).color());
    }

    @Test void paragraphsIllustrationsAndRulesKeepTheirParsedContent() {
        var parts = ResearchText.parse("§lНачало§r<BR>Конец<LINE><IMG>thaumcraft:textures/misc/research2.png:0:88:255:158:.5</IMG>После");
        assertEquals("Начало\n\nКонец", ResearchText.formatted(((ResearchText.Text)parts.get(0)).value()).getString());
        assertInstanceOf(ResearchText.Rule.class, parts.get(1));
        assertEquals(new ResearchText.Image("thaumcraft:textures/misc/research2.png", 0, 88, 255, 158, .5F), parts.get(2));
        assertEquals("После", ResearchText.formatted(((ResearchText.Text)parts.get(3)).value()).getString());
    }

    private static FormattedCharSequence visual(FormattedText line) { return FormattedBidiReorder.reorder(line, false); }

    private record Glyph(String text, boolean bold, boolean italic, net.minecraft.network.chat.TextColor color) {}

    private static List<Glyph> renderedStyles(List<? extends FormattedText> lines) {
        List<Glyph> result = new ArrayList<>();
        for (var line : lines) visual(line).accept((index, style, codePoint) -> {
            result.add(new Glyph(Character.toString(codePoint), style.isBold(), style.isItalic(), style.getColor()));
            return true;
        });
        return result;
    }
}
