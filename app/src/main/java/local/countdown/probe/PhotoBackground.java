package local.countdown.probe;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

/** Import an oriented, downsampled private copy; no ongoing gallery access is needed. */
final class PhotoBackground {
    private static AtomicFile file(Context context) { return new AtomicFile(new File(context.getFilesDir(), "background-photo.png")); }
    static synchronized void importPhoto(Context context, Uri uri) throws IOException {
        Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri), (decoder, info, source) -> {
            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
            float scale = Math.min(1f, 1600f / Math.max(w, h));
            decoder.setTargetSize(Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)));
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
        });
        AtomicFile target = file(context);
        FileOutputStream stream = null;
        try {
            stream = target.startWrite();
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) throw new IOException("Image encoding failed");
            target.finishWrite(stream); stream = null;
        } finally {
            if (stream != null) target.failWrite(stream);
            bitmap.recycle();
        }
        context.getSharedPreferences("settings", 0).edit().putInt("background_type", 3)
                .putInt("photo_zoom", 100).putInt("photo_x", 50).putInt("photo_y", 50).apply();
    }
    static synchronized Bitmap load(Context context) {
        try (FileInputStream stream = file(context).openRead()) { return BitmapFactory.decodeStream(stream); }
        catch (IOException | RuntimeException unreadable) { return null; }
    }
    static synchronized boolean exists(Context context) { return file(context).getBaseFile().isFile(); }
    static synchronized void remove(Context context) { file(context).delete(); }
}
