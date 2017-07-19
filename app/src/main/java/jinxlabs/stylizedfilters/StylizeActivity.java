package jinxlabs.stylizedfilters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Range;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import jinxlabs.stylizedfilters.utils.ImageUtils;
import jinxlabs.stylizedfilters.utils.Logger;

public class StylizeActivity extends AppCompatActivity {

    @BindView(R.id.filter_preview) ImageView imagePreview;
    @BindView(R.id.stylize_button) Button stylizeButton;

    private static final int targetWidth = 720;
    private static final int targetHeight = 720;

    private Bitmap imageBitmap;
    private Logger logger = new Logger(StylizeActivity.class);

    private TensorFlowInferenceInterface inferenceInterface;

    private static final String MODEL_FILE = "file:///android_asset/style_transfer_model.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;

    private final float[] styleVals = new float[NUM_STYLES];

    private ArrayList<Bitmap> squareParts;

    private ImageUtils imageUtils;

    private ArrayList<ImageInfo> imageInfoArrayList = new ArrayList<>();

    static {
        System.loadLibrary("image_stitching");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stylize);

        //String filePath = getIntent().getExtras().getString("IMAGE_FILE_URL");
        String filePath = "/sdcard/Download/test3.jpg";

        logger.d("Image file path %s", filePath);

        ButterKnife.bind(this);

        imageUtils = new ImageUtils();

        // Show original image
        imageBitmap = imageUtils.fetchBitmapFromFile(filePath);
        logger.d("Original image width : %d height : %d", imageBitmap.getWidth(), imageBitmap.getHeight());

        squareParts = imageUtils.splitImageIntoSquares(imageBitmap, targetWidth, targetHeight);
        fetchPixelValues(squareParts);
        imagePreview.setImageBitmap(imageBitmap);

        initTensorFlow();
    }

    private void fetchPixelValues(ArrayList<Bitmap> squareParts) {
        for (Bitmap bitmap : squareParts) {
            ImageInfo imageInfo = new ImageInfo(targetWidth, targetHeight);
            imageInfo.readPixelValues(bitmap);

            imageInfoArrayList.add(imageInfo);
        }
    }

/*    private Bitmap drawSquares(Bitmap imageBitmap) {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(imageBitmap, imageMat);

        int originalWidth = imageMat.width();
        int originalHeight = imageMat.height();

        float aspectRatio = (float) originalWidth / originalHeight;
        int heightWithAspectRatio = (int) (targetWidth / aspectRatio);

        Imgproc.resize(imageMat, imageMat, new Size(targetWidth, heightWithAspectRatio));

        logger.i("Resulting mat object dimens : w : %d, h : %d, w/h : %f", imageMat.cols(), imageMat.rows(), aspectRatio);

        Imgproc.rectangle(imageMat, new Point(0, 0), new Point(targetWidth, targetHeight), new Scalar(0, 0, 255));
        Imgproc.rectangle(imageMat, new Point(0, (heightWithAspectRatio - targetHeight)), new Point(targetWidth, heightWithAspectRatio), new Scalar(0, 255, 255));

        Bitmap resultBitmap = Bitmap.createBitmap(targetWidth, heightWithAspectRatio, Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imageMat, resultBitmap);

        return resultBitmap;
    }*/


    @OnClick(R.id.stylize_button)
    public void onClickStylizeButton() {
        Observable.fromArray(imageInfoArrayList)
                .map(new Function<ArrayList<ImageInfo>, ArrayList<Bitmap>>() {
                    @Override
                    public ArrayList<Bitmap> apply(@NonNull ArrayList<ImageInfo> imageInfos) throws Exception {
                        ArrayList<Bitmap> styles = new ArrayList<Bitmap>();
                        for (ImageInfo imageInfo : imageInfos) {
                            Bitmap bitmap = applyStyle(imageInfo.getRgbValues(), imageInfo.getRgbPacked());
                            styles.add(bitmap);
                        }

                        return styles;
                    }
                })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ArrayList<Bitmap>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {

                    }

                    @Override
                    public void onNext(@NonNull ArrayList<Bitmap> bitmaps) {
                        Bitmap resultBitmap = imageUtils.stitchImages(bitmaps);
                        imagePreview.setImageBitmap(resultBitmap);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private Bitmap applyStyle(float[] rgbValues, int[] rgbPackedValues) {
        setStyle();

        // Copy the input data into TensorFlow.
        logger.startTimeLogging("Feeding data to tensorflow");
        inferenceInterface.feed(INPUT_NODE, rgbValues, 1, targetWidth, targetHeight, 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);
        logger.logTimeTaken();

        // Execute the output node's dependency sub-graph.
        logger.startTimeLogging("Running Tensorflow");
        inferenceInterface.run(new String[] {OUTPUT_NODE}, false);
        logger.logTimeTaken();

        // Copy the data from TensorFlow back into our array.
        float[] rgbValsFromTF = new float[targetWidth * targetHeight * 3];
        int[] rgbPackedFromTF = new int[targetWidth * targetHeight];

        logger.startTimeLogging("Fetching from output node");
        inferenceInterface.fetch(OUTPUT_NODE, rgbValsFromTF);
        logger.logTimeTaken();

        for (int i = 0; i < rgbPackedValues.length; i++) {
            rgbPackedFromTF[i] =
                    0xFF000000
                            | (((int) (rgbValsFromTF[i * 3] * 255)) << 16)
                            | (((int) (rgbValsFromTF[i * 3 + 1] * 255)) << 8)
                            | ((int) (rgbValsFromTF[i * 3 + 2] * 255));
        }

        Bitmap stylizedBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
        stylizedBitmap.setPixels(rgbPackedFromTF, 0, stylizedBitmap.getWidth(), 0, 0, stylizedBitmap.getWidth(), stylizedBitmap.getHeight());

        return stylizedBitmap;
    }

    private void setStyle() {
        for (int i = 0; i < NUM_STYLES; i++) {
            styleVals[i] = 0;
        }

        //styleVals[0] = 1.0f;

        styleVals[0] = 0.5f;
        styleVals[10] = 0.3f;
        styleVals[11] = 0.2f;
    }

    private void initTensorFlow() {
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
    }


}
