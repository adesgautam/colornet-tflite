package com.example.colornet;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
//import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.example.colornet.tflite.Colornet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    Integer CHOOSE_IMAGE = 1;
    Colornet colornet;

    Bitmap colouredBaseBitmap = null;
    private Uri photoUri;
    SeekBar seekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button chooseImage = (Button) findViewById(R.id.chooseImage);
        chooseImage.setOnClickListener(this);

        Button colorizeImageButton = (Button) findViewById(R.id.colorizeImage);
        colorizeImageButton.setOnClickListener(this);

        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(this);

        ImageView imView = (ImageView) findViewById(R.id.imageView);

        seekBar = (SeekBar)findViewById(R.id.seekBar);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChangedValue = progress;

                Bitmap filteredBmp = setSaturation( (float) progressChangedValue/10);
                imView.setImageBitmap(filteredBmp);

//                Toast.makeText(MainActivity.this, "Seek bar progress is :" + progressChangedValue,
//                        Toast.LENGTH_SHORT).show();
            }

            public void onStartTrackingTouch(SeekBar seekBar) { }
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    public Bitmap setSaturation(float value){
        int width, height;
        height = colouredBaseBitmap.getHeight();
        width = colouredBaseBitmap.getWidth();

        Bitmap bmpFiltered = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpFiltered);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(value);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(colouredBaseBitmap, 0, 0, paint);
        return bmpFiltered;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.chooseImage:
                // To Choose Image
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(intent, CHOOSE_IMAGE);
                }
                startActivityForResult(intent, CHOOSE_IMAGE);
                break;

            case R.id.colorizeImage:
                try {
                    colornet = Colornet.create(this, Colornet.Model.FLOAT_COLORNET, Colornet.Device.CPU, 4);

                    // Get Bitmap from ImageView
                    ImageView ivPreview = (ImageView) findViewById(R.id.imageView);
                    Bitmap inpImage =((BitmapDrawable) ivPreview.getDrawable()).getBitmap();

                    // Colorize Image
                    Bitmap opImage = colornet.colorizeImage(inpImage);
                    colouredBaseBitmap = opImage.copy(Bitmap.Config.ARGB_8888, true);

                    // Set colorized Image
                    ivPreview.setImageBitmap(opImage);

                    Toast.makeText(getApplicationContext(),"Colorization Done!", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.saveButton:
                ImageView ivPreview = (ImageView) findViewById(R.id.imageView);
                Bitmap image =((BitmapDrawable) ivPreview.getDrawable()).getBitmap();

                try {
                    savebitmap(image);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Toast.makeText(getApplicationContext(),"Image Saved!", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public Bitmap loadFromUri(Uri photoUri) {
        Bitmap image = null;
        try {
            if(Build.VERSION.SDK_INT > 27){
                // on newer versions of Android, use the new decodeBitmap method
                ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                // support older versions of Android by using getBitmap
                image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        photoUri = data.getData();
        Toast.makeText(getApplicationContext(),photoUri.toString(),Toast.LENGTH_SHORT).show();

        if ((data != null) && requestCode == CHOOSE_IMAGE) {
            Bitmap selectedImage = loadFromUri(photoUri);
            ImageView ivPreview = (ImageView) findViewById(R.id.imageView);
            ivPreview.setImageBitmap(selectedImage);
            colouredBaseBitmap = selectedImage.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    public void savebitmap(Bitmap bmp) throws IOException {
        OutputStream imageOutStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "testImage.jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            imageOutStream = getContentResolver().openOutputStream(uri);
        } else {
            String imagePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString();
            File image = new File(imagePath, "testImage.jpg");
            imageOutStream = new FileOutputStream(image);
        }

        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
        } finally {
            imageOutStream.close();
        }
    }
//
//    SeekBar.OnSeekBarChangeListener seekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
//
//        @Override
//        public void onProgressChanged(SeekBar seekBar, int progress,
//                                      boolean fromUser) {
//        }
//
//        @Override
//        public void onStartTrackingTouch(SeekBar seekBar) {
//        }
//
//        @Override
//        public void onStopTrackingTouch(SeekBar seekBar) {
//            loadBitmapSat();
//        }
//    };
//
//    private void loadBitmapSat() {
//        if (bitmapMaster != null) {
//
//            int progressSat = satBar.getProgress();
//
//            //Saturation, 0=gray-scale. 1=identity
//            float sat = (float) progressSat / 256;
////            satText.setText("Saturation: " + String.valueOf(sat));
//            imgView.setImageBitmap(updateSat(bitmapMaster, sat));
//        }
//    }
//
//    private Bitmap updateSat(Bitmap src, float settingSat) {
//
//        int w = src.getWidth();
//        int h = src.getHeight();
//
//        Bitmap bitmapResult =
//                Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//        Canvas canvasResult = new Canvas(bitmapResult);
//        Paint paint = new Paint();
//        ColorMatrix colorMatrix = new ColorMatrix();
//        colorMatrix.setSaturation(settingSat);
//        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
//        paint.setColorFilter(filter);
//        canvasResult.drawBitmap(src, 0, 0, paint);
//
//        return bitmapResult;
//    }
}