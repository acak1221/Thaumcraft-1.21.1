package thaumcraft.client.gui;

/**
 * What the aspect tag draws and where, as plain numbers: the pure half of {@link ThaumcraftRecipeDisplay#drawAspectTag}.
 *
 * <p>The public {@code thaumcraft.api.client.AspectRenderer.draw(graphics, aspect, x, y, amount, bonus)} builds one of these
 * from its arguments and the renderer reads every position, every threshold and every text from it, so the order of the
 * arguments (four adjacent ints), the thresholds of the amount and of the bonus and the text of the numbers can be asserted by a
 * unit test without a screen. The offsets inside the tag are the constants of {@code AspectRenderer}.</p>
 *
 * <p>Nothing here names a client class that needs a running game.</p>
 *
 * @param x      the left edge of the tag, in the pixels of the graphics
 * @param y      the top edge of the tag, in the pixels of the graphics
 * @param scale  how many times as large as its sixteen pixels the tag is drawn; 1 for the public renderer
 * @param amount the number at the bottom right corner; zero or less shows none
 * @param bonus  zero or less shows no bonus, one the sparkle, more the sparkle and the number
 */
public record AspectTagLayout(int x, int y, float scale, int amount, int bonus) {
    /** The layout of a tag; the order of the arguments is the one of {@code AspectRenderer.draw}. */
    public static AspectTagLayout of(int x, int y, int amount, int bonus, float scale) {
        return new AspectTagLayout(x, y, scale, amount, bonus);
    }

    /** Whether the amount is drawn: above zero. */
    public boolean showsAmount() {
        return amount > 0;
    }

    /** The amount as it is written: plain digits, no sign and no thousands separator (the book's number). */
    public String amountText() {
        return Integer.toString(amount);
    }

    /** Whether the sparkle of the bonus is drawn: above zero. */
    public boolean showsBonus() {
        return bonus > 0;
    }

    /** Whether the number of the bonus is drawn as well: above one. */
    public boolean showsBonusNumber() {
        return bonus > 1;
    }

    /** The bonus as it is written when {@link #showsBonusNumber()}. */
    public String bonusText() {
        return Integer.toString(bonus);
    }
}
