package jinxlabs.stylizedfilters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jinxlabs.stylizedfilters.utils.Logger;

public class StylizeActivity extends AppCompatActivity {

    @BindView(R.id.filter_preview) ImageView imagePreview;
    @BindView(R.id.stylize_button) Button stylizeButton;

    private static final int targetWidth = 720;
    private static final int targetHeight = 720;

    private int[] rgbPackedValues = new int[targetWidth * targetHeight];
    private float[] rgbValues = new float[targetWidth * targetHeight * 3];

    private Bitmap imageBitmap;
    private Logger logger = new Logger(StylizeActivity.class);

    private TensorFlowInferenceInterface inferenceInterface;

    private static final String MODEL_FILE = "file:///android_asset/style_transfer_model.pb";
    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;

    private final float[] styleVals = new float[NUM_STYLES];

    private Observer<Bitmap> applyStyleObserver = new Observer<Bitmap>() {
        @Override
        public void onSubscribe(@NonNull Disposable d) {

        }

        @Override
        public void onNext(@NonNull Bitmap bitmap) {
            logger.startTimeLogging("Applying Style");
            applyStyle(bitmap);
            logger.logTimeTaken();
        }

        @Override
        public void onError(@NonNull Throwable e) {
            e.printStackTrace();
        }

        @Override
        public void onComplete() {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stylize);

        //String filePath = getIntent().getExtras().getString("IMAGE_FILE_URL");
        String filePath = "/sdcard/Download/test3.jpg";

        logger.d("Image file path %s", filePath);

        ButterKnife.bind(this);

        // Show original image
        imageBitmap = BitmapFactory.decodeFile(filePath);
        logger.d("Original image width : %d height : %d", imageBitmap.getWidth(), imageBitmap.getHeight());

        logger.startTimeLogging("Read pixel values to array");
        readPixelValues(imageBitmap);
        logger.logTimeTaken();

        imagePreview.setImageBitmap(imageBitmap);

        initTensorFlow();
    }

    private void readPixelValues(Bitmap imageBitmap) {
        Bitmap resizedBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, targetWidth, targetHeight);
        resizedBitmap.getPixels(rgbPackedValues, 0, targetWidth, 0, 0, targetWidth, targetHeight);

        for (int i = 0; i < rgbPackedValues.length; i++) {
            final int value = rgbPackedValues[i];

            rgbValues[i * 3] = ((value >> 16) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 1] = ((value >> 8) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 2] = (value & 0xFF) / 255.0f;
        }
    }

    @OnClick(R.id.stylize_button)
    public void onClickStylizeButton() {
        Observable.just(imageBitmap)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(applyStyleObserver);
    }

    private void applyStyle(Bitmap imageBitmap) {
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
        imagePreview.setImageBitmap(stylizedBitmap);
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
