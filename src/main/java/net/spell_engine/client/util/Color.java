package net.spell_engine.client.util;

public record Color(float red, float green, float blue, float alpha) {
    public Color(float red, float green, float blue) {
        this(red, green, blue, 1);
    }

    public static Color from(int rgb) {
        float red = ((float) ((rgb >> 16) & 0xFF)) / 255F;
        float green = ((float) ((rgb >> 8) & 0xFF)) / 255F;
        float blue = ((float) (rgb & 0xFF)) / 255F;
        return new Color(red, green, blue);
    }

    public record IntFormat(int red, int green, int blue, int alpha) {
        public static IntFormat fromLongRGBA(long rgba) {
            var red = (rgba >> 24) & 255;
            var green = (rgba >> 16) & 255;
            var blue = (rgba >> 8) & 255;
            var alpha = rgba & 255;
            return new IntFormat((int)red, (int)green, (int)blue, (int)alpha);
        }
    }
    public IntFormat toIntFormat() {
        return new IntFormat((int) (red * 255), (int) (green * 255), (int) (blue * 255), (int) (alpha * 255));
    }
    public record ByteFormat(byte red, byte green, byte blue, byte alpha) { }
    public ByteFormat toByteFormat() {
        return new ByteFormat((byte) (red * 255), (byte) (green * 255), (byte) (blue * 255), (byte) (alpha * 255));
    }

    public static final Color RED = new Color(1, 0, 0);
    public static final Color GREEN = new Color(1, 0, 0);
    public static final Color BLUE = new Color(1, 0, 0);
    public static final Color WHITE = new Color(1, 1, 1);

    public static final Color HOLY = Color.from(0xffffcc);
    public static final Color NATURE = Color.from(0x66ff66);
    public static final Color FROST = Color.from(0x66ccff);
    public static final Color ELECTRIC = Color.from(0xffff66);
    public static final Color RAGE = Color.from(0xbf4040);
}
