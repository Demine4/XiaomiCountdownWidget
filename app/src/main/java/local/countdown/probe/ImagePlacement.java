package local.countdown.probe;

/** Center-crop geometry, independent of Android. Position 0/100 aligns the corresponding edges. */
public final class ImagePlacement {
    public static float[] cover(int imageW, int imageH, int canvasW, int canvasH, int zoom, int x, int y) {
        if (imageW <= 0 || imageH <= 0 || canvasW <= 0 || canvasH <= 0) throw new IllegalArgumentException("Invalid image dimensions");
        float scale = Math.max(canvasW / (float)imageW, canvasH / (float)imageH) * Math.max(100, Math.min(300, zoom)) / 100f;
        float w = imageW * scale, h = imageH * scale;
        float left = -(w - canvasW) * Math.max(0, Math.min(100, x)) / 100f;
        float top = -(h - canvasH) * Math.max(0, Math.min(100, y)) / 100f;
        return new float[]{left, top, left + w, top + h};
    }
}
