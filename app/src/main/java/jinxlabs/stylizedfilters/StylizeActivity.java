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

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import jinxlabs.stylizedfilters.utils.Logger;

public class StylizeActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private Button stylizeButton;

    private File imageFile;

    private Bitmap imageBitmap;

    private TensorFlowInferenceInterface inferenceInterface;

    private static final String MODEL_FILE = "file:///android_asset/style_transfer_model.pb";

    private static final String INPUT_NODE = "input";
    private static final String STYLE_NODE = "style_num";
    private static final String OUTPUT_NODE = "transformer/expand/conv3/conv/Sigmoid";
    private static final int NUM_STYLES = 26;

    private final float[] styleVals = new float[NUM_STYLES];

    private Logger logger = new Logger(StylizeActivity.class);

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
        String filePath = "/sdcard/Download/test.jpg";

        imageFile = new File(filePath);
        logger.d("Image file path %s", imageFile.getAbsolutePath());

        imagePreview = (ImageView) findViewById(R.id.filter_preview);
        stylizeButton = (Button) findViewById(R.id.stylize_button);

        imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imagePreview.setImageBitmap(imageBitmap);

        initTensorFlow();

        stylizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Observable.just(imageBitmap)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(applyStyleObserver);
            }
        });
    }

    private void applyStyle(Bitmap imageBitmap) {
        int targetWidth = 720;
        int targetHeight = 720;

        Bitmap resizedBitmap = ThumbnailUtils.extractThumbnail(imageBitmap, targetWidth, targetHeight);

        int[] rgbPackedValues = new int[targetWidth * targetHeight];
        float[] rgbValues = new float[targetWidth * targetHeight * 3];

        for (int i = 0; i < NUM_STYLES; i++) {
            styleVals[i] = 0;
        }

        //styleVals[0] = 1.0f;

        styleVals[0] = 0.5f;
        styleVals[10] = 0.3f;
        styleVals[11] = 0.2f;

        resizedBitmap.getPixels(rgbPackedValues, 0, targetWidth, 0, 0, targetWidth, targetHeight);

        for (int i = 0; i < rgbPackedValues.length; i++) {
            final int value = rgbPackedValues[i];

            rgbValues[i * 3] = ((value >> 16) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 1] = ((value >> 8) & 0xFF) / 255.0f;
            rgbValues[i * 3 + 2] = (value & 0xFF) / 255.0f;
        }

        // Copy the input data into TensorFlow.
        inferenceInterface.feed(INPUT_NODE, rgbValues,
                1, targetWidth, targetHeight, 3);
        inferenceInterface.feed(STYLE_NODE, styleVals, NUM_STYLES);

        // Execute the output node's dependency sub-graph.
        inferenceInterface.run(new String[] {OUTPUT_NODE}, false);

        // Copy the data from TensorFlow back into our array.
        inferenceInterface.fetch(OUTPUT_NODE, rgbValues);

        for (int i = 0; i < rgbPackedValues.length; i++) {
            rgbPackedValues[i] =
                    0xFF000000
                            | (((int) (rgbValues[i * 3] * 255)) << 16)
                            | (((int) (rgbValues[i * 3 + 1] * 255)) << 8)
                            | ((int) (rgbValues[i * 3 + 2] * 255));
        }

        resizedBitmap.setPixels(rgbPackedValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
        imagePreview.setImageBitmap(resizedBitmap);
    }

    private void initTensorFlow() {
        inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
    }
}
