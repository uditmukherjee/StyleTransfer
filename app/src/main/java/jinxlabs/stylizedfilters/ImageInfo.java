package jinxlabs.stylizedfilters;

import android.graphics.Bitmap;

/**
 * Created by uditmukherjee on 20/07/17.
 */

public class ImageInfo {
    private int[] rgbPacked;
    private float[] rgbValues;

    public ImageInfo(int targetWidth, int targetHeight) {
        rgbPacked = new int[targetWidth * targetHeight];
        rgbValues = new float[targetWidth * targetHeight * 3];
    }

    public int[] getRgbPacked() {
        return rgbPacked;
    }

    public float[] getRgbValues() {
        return rgbValues;
    }

    public void readPixelValues(Bitmap imageBitmap) {
        imageBitmap.getPixels(rgbPacked, 0, imageBitmap.getWidth(), 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight());

        for (int i = 0; i < rgbPacked.length; i++) {
            final int value = rgbPacked[i];

            rgbValues[i * 3] = ((value >> 16) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 1] = ((value >> 8) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 2] = (value & 0xFF) / 255.0f;
        }
    }
}
