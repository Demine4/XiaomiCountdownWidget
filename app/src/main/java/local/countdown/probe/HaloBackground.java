package local.countdown.probe;

/** Organic, overlapping color clouds. Also renders unchanged on the JVM for visual inspection. */
public final class HaloBackground {
    private static int blend(int base, int tint, double alpha) {
        int r = (int)Math.round((base >> 16 & 255) * (1-alpha) + (tint >> 16 & 255) * alpha);
        int g = (int)Math.round((base >> 8 & 255) * (1-alpha) + (tint >> 8 & 255) * alpha);
        int b = (int)Math.round((base & 255) * (1-alpha) + (tint & 255) * alpha);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
    private static double cloud(double x, double y, double cx, double cy, double rx, double ry, double tilt, double speed) {
        double dx = x - cx, dy = y - cy;
        double u = dx + dy * tilt;
        double distance = u*u/(rx*rx) + dy*dy/(ry*ry);
        double fade = Math.max(0, 1 - distance);
        return fade * fade * Math.exp(-speed * distance);
    }
    public static int[] render(int width, int height, boolean dark, float strength, float brightness) {
        return render(width, height, dark, strength, brightness, 70, 3);
    }
    public static int[] render(int width, int height, boolean dark, float strength, float brightness, int size, int fadeSpeed) {
        return render(width, height, dark, strength, brightness, size, fadeSpeed, 18, 31);
    }
    public static int[] render(int width, int height, boolean dark, float strength, float brightness, int size, int fadeSpeed, int horizontal, int vertical) {
        return render(width, height, dark, strength, brightness, size, fadeSpeed, horizontal, vertical, null);
    }
    /** A null palette preserves the original theme. Custom palettes contain one to four RGB colors. */
    public static int[] render(int width, int height, boolean dark, float strength, float brightness, int size, int fadeSpeed, int horizontal, int vertical, int[] palette) {
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid bitmap size");
        if (palette != null && (palette.length < 1 || palette.length > 4)) throw new IllegalArgumentException("Choose 1–4 colors");
        double amount = Math.max(0, Math.min(1, strength));
        double exposure = Math.max(-1, Math.min(1, brightness));
        double scale = Math.max(20, Math.min(240, size)) / 100.0;
        double speed = Math.max(1, Math.min(10, fadeSpeed));
        double offsetX = (Math.max(0, Math.min(100, horizontal)) - 18) / 100.0;
        double offsetY = (Math.max(0, Math.min(100, vertical)) - 31) / 100.0;
        int[] pixels = new int[width * height];
        int base = dark ? 0xFF25262B : 0xFFFFFFFF;
        // Brightness changes the backing surface, never an overlay covering the color clouds.
        if (exposure != 0) base = blend(base, exposure > 0 ? 0xFFFFFFFF : 0xFF000000, Math.abs(exposure));
        int pink = dark ? 0xFF815367 : 0xFFF0D4DF;
        int purple = dark ? 0xFF71619F : 0xFFCCB8F4;
        int blue = dark ? 0xFF416F87 : 0xFFC7E3F5;
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) {
            double u = (x+.5)/width - offsetX, v = (y+.5)/height - offsetY;
            int color = base;
            if (palette == null) {
                color = blend(color, pink, amount * .65 * cloud(u,v,.115,.25,.19*scale,.43*scale,-.07,speed));
                color = blend(color, blue, amount * .70 * cloud(u,v,.18,.45,.22*scale,.48*scale,.045,speed));
                color = blend(color, purple, amount * .65 * cloud(u,v,.255,.235,.22*scale,.52*scale,.07,speed));
            } else {
                for (int i = 0; i < palette.length; i++) {
                    double cx, cy;
                    if (palette.length == 1) { cx = .185; cy = .33; }
                    else if (palette.length == 2) { cx = i == 0 ? .14 : .235; cy = i == 0 ? .28 : .39; }
                    else { cx = i == 0 ? .115 : i == 1 ? .18 : i == 2 ? .255 : .30; cy = i == 0 ? .25 : i == 1 ? .45 : i == 2 ? .235 : .43; }
                    color = blend(color, palette[i], amount * .68 * cloud(u,v,cx,cy,.22*scale,.48*scale,(i % 2 == 0 ? -.06 : .06),speed));
                }
            }
            pixels[y*width+x] = color;
        }
        return pixels;
    }
}
