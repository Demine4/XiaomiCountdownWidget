import local.countdown.probe.HaloBackground;
import java.util.Arrays;

public final class HaloBackgroundTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static int[] render(int[] colors, float strength, boolean dark) {
        return HaloBackground.render(200,100,dark,strength,0,70,3,18,31,colors);
    }
    public static void main(String[] args) {
        check(Arrays.equals(HaloBackground.render(200,100,false,.85f,0,70,3,18,31),render(null,.85f,false)),"Default overload changed");
        int[] red = render(new int[]{0xFFFF0000},1,false);
        boolean visible = false;
        for (int pixel : red) {
            check((pixel >>> 24) == 255,"Background must be opaque");
            check(((pixel >> 16)&255) == 255,"A single red halo introduced another hue");
            check(((pixel >> 8)&255) == (pixel&255),"Red halo must keep green/blue equal");
            visible |= (pixel&255) < 255;
        }
        check(visible,"Single color halo invisible");
        for (int pixel : render(new int[]{0xFFFF0000},0,false)) check(pixel == 0xFFFFFFFF,"Zero strength changed base");
        for (int pixel : render(new int[]{0xFFFF0000},0,true)) check(pixel == 0xFF25262B,"Dark base lost");
        int[] palette = {0xFFFF0000,0xFF00FF00,0xFF0000FF,0xFFFFFF00};
        for (int count = 2; count <= 4; count++) {
            int[] current = Arrays.copyOf(palette,count);
            check(!Arrays.equals(render(current,1,false),render(Arrays.copyOf(palette,count-1),1,false)),"Extra selected color has no effect: "+count);
            check(Arrays.equals(current,Arrays.copyOf(palette,count)),"Palette mutated");
        }
        for (int count : new int[]{0,5}) {
            try { render(new int[count],1,false); throw new AssertionError("Invalid palette accepted"); }
            catch (IllegalArgumentException expected) {}
        }
        for (boolean dark : new boolean[]{false,true}) for (float brightness : new float[]{-1,1}) {
            for (int count = 0; count <= 4; count++) {
                int[] colors = count == 0 ? null : Arrays.copyOf(palette,count);
                int[] image = HaloBackground.render(200,100,dark,1,brightness,70,3,18,31,colors);
                int base = brightness < 0 ? 0xFF000000 : 0xFFFFFFFF;
                boolean haloVisible = false;
                for (int pixel : image) haloVisible |= pixel != base;
                check(haloVisible,"Extreme brightness covered halo: "+brightness+" colors="+count+" dark="+dark);
                check(image[19999] == base,"Brightness did not change backing surface");
            }
        }
        System.out.println("Halo palette tests passed (defaults, 1–4 colors, hue, strength, dark mode, invalid counts)");
    }
}
