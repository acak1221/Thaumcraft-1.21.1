package thaumic.tinkerer.common.core.golems;

/** The golem accessories as the Golem Connector reports them (original names, characters and lang keys). */
public enum EnumGolemDecorations {

    TopHat('H', "item.ItemGolemDecoration.0.name"),
    Glasses('G', "item.ItemGolemDecoration.1.name"),
    BowTie('B', "item.ItemGolemDecoration.2.name"),
    Fez('F', "item.ItemGolemDecoration.3.name"),
    DartGun('D', "item.ItemGolemDecoration.4.name"),
    Visor('V', "item.ItemGolemDecoration.5.name"),
    Plating('P', "item.ItemGolemDecoration.6.name"),
    MaceArm('M', "item.ItemGolemDecoration.7.name");

    private final char decorationChar;
    private final String name;

    EnumGolemDecorations(char ch, String name) {
        decorationChar = ch;
        this.name = name;
    }

    /**
     * The accessory for a character of the golem's decoration string. The original maps {@code 'R'} (not {@code 'D'},
     * the character stored in the enum) to the dart gun, because that is the character Thaumcraft writes; kept.
     */
    public static EnumGolemDecorations getFromChar(char ch) {
        switch (ch) {
            case 'H':
                return TopHat;
            case 'G':
                return Glasses;
            case 'B':
                return BowTie;
            case 'F':
                return Fez;
            case 'R':
                return DartGun;
            case 'V':
                return Visor;
            case 'P':
                return Plating;
            case 'M':
                return MaceArm;
            default:
                return null;
        }
    }

    /** The translation key of the accessory (a key of Thaumcraft's own language file). */
    public String getName() {
        return name;
    }

    public char getChar() {
        return decorationChar;
    }
}
