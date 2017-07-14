package jinxlabs.stylizedfilters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

import java.io.File;

public class StylizeActivity extends AppCompatActivity {

    private ImageView imagePreview;
    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stylize);

        String filePath = getIntent().getExtras().getString("IMAGE_FILE_URL");

        imageFile = new File(filePath);

        Bitmap imageBitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        imagePreview = (ImageView) findViewById(R.id.filter_preview);
        imagePreview.setImageBitmap(imageBitmap);
    }
}
