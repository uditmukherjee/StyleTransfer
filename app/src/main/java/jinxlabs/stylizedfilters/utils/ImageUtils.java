package jinxlabs.stylizedfilters.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Range;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by uditmukherjee on 19/07/17.
 */

public class ImageUtils {
    private Logger logger = new Logger(ImageUtils.class);

    static {
        System.loadLibrary("image_stitching");
    }

    public ImageUtils() {
        if (!OpenCVLoader.initDebug()) {
            logger.e("OpenCVLoader.initDebug(), not working.");
        } else {
            logger.i("OpenCVLoader.initDebug(), working.");
        }
    }

    public ArrayList<Bitmap> splitImageIntoSquares(Bitmap imageBitmap, int targetWidth, int targetHeight) {
        logger.startTimeLogging("Splitting images into square pieces");
        Mat imageMat = new Mat();
        Utils.bitmapToMat(imageBitmap, imageMat);

        resize(imageMat, targetWidth); // Resize keeping a constant width and variable height
        int heightWithAspectRatio = imageMat.height();

        ArrayList<Bitmap> squareImages = new ArrayList<>();

        Mat part1 = new Mat(imageMat, new Range(0, targetHeight), new Range(0, targetWidth));
        Mat part2 = new Mat(imageMat, new Range((heightWithAspectRatio - targetHeight), heightWithAspectRatio), new Range(0, targetWidth));

        Bitmap resultObject1 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        Bitmap resultObject2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);

        Utils.matToBitmap(part1, resultObject1);
        squareImages.add(resultObject1);

        Utils.matToBitmap(part2, resultObject2);
        squareImages.add(resultObject2);

        logger.logTimeTaken();
        return squareImages;
    }

    public void resize(Mat imageMat, int targetWidth) {
        // Resize maintaining aspect ratio while keeping width to 720
        int originalWidth = imageMat.width();
        int originalHeight = imageMat.height();

        float aspectRatio = (float) originalWidth / originalHeight;
        int heightWithAspectRatio = (int) (targetWidth / aspectRatio);

        Imgproc.resize(imageMat, imageMat, new Size(targetWidth, heightWithAspectRatio));

        logger.d("Target Width : %d Target Height : %d Aspect ratio %f", imageMat.cols(), imageMat.rows(), aspectRatio);

    }

    public Bitmap stitchImages(ArrayList<Bitmap> images) {
        Mat[] imgMats = new Mat[images.size()];

        for (int i = 0; i < images.size(); i++) {
            Bitmap bitmap = images.get(i);
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            imgMats[i] = mat;
        }

        Mat resultMat = new Mat();

        logger.startTimeLogging("stitching images");
        stitch(imgMats, resultMat.getNativeObjAddr());
        logger.logTimeTaken();

        Bitmap resultBitmap = Bitmap.createBitmap(resultMat.width(), resultMat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(resultMat, resultBitmap);

        return resultBitmap;
    }



    public Bitmap fetchBitmapFromFile(String filePath) {
        return BitmapFactory.decodeFile(filePath);
    }

    private native void stitch(Mat imgs[], long resultMatAddr);
}
