import local.countdown.probe.ImagePlacement;

public class ImagePlacementTest {
    private static int count;
    private static void check(boolean value, String name) { if (!value) throw new AssertionError(name); count++; }
    private static boolean close(float a, float b) { return Math.abs(a-b) < .01f; }
    public static void main(String[] args) {
        float[] portrait = ImagePlacement.cover(1000, 2000, 720, 330, 100, 50, 50);
        check(close(portrait[0], 0) && close(portrait[2], 720), "portrait fills width");
        check(portrait[1] < 0 && portrait[3] > 330, "portrait center crop");
        float[] landscape = ImagePlacement.cover(2000, 500, 720, 330, 100, 0, 50);
        check(close(landscape[0], 0) && landscape[2] > 720, "landscape left aligned");
        float[] right = ImagePlacement.cover(2000, 500, 720, 330, 100, 100, 50);
        check(right[0] < 0 && close(right[2], 720), "landscape right aligned");
        float[] zoom = ImagePlacement.cover(720, 330, 720, 330, 200, 50, 50);
        check(close(zoom[2]-zoom[0], 1440) && close(zoom[3]-zoom[1], 660), "double zoom");
        check(close(zoom[0], -360) && close(zoom[1], -165), "zoom stays centered");
        float[] above = ImagePlacement.cover(1000, 2000, 720, 330, 100, 50, 0);
        float[] below = ImagePlacement.cover(1000, 2000, 720, 330, 100, 50, 100);
        check(close(above[1], 0) && close(below[3], 330), "vertical endpoints");
        boolean noGaps = true;
        for (int z : new int[]{100, 150, 300}) for (int x : new int[]{0, 50, 100}) for (int y : new int[]{0, 50, 100}) {
            float[] r = ImagePlacement.cover(4032, 3024, 720, 330, z, x, y);
            noGaps &= r[0] <= .01f && r[1] <= .01f && r[2] >= 719.99f && r[3] >= 329.99f;
        }
        check(noGaps, "all positions cover full background");
        try { ImagePlacement.cover(0, 100, 720, 330, 100, 50, 50); throw new AssertionError("invalid size accepted"); }
        catch (IllegalArgumentException expected) { count++; }
        System.out.println("PASS: " + count + " photo crop checks");
    }
}
