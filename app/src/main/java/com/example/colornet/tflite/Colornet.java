package com.example.colornet.tflite;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageDecoder;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.gpu.GpuDelegate;
import androidx.core.graphics.ColorUtils;

public abstract class Colornet {

    protected Colornet() {
    }

    public enum Model {
        FLOAT_COLORNET,
    }

    public enum Device {
        CPU,
        GPU
    }

    private GpuDelegate gpuDelegate = null;

    private MappedByteBuffer tflite_colorModel;
    private MappedByteBuffer tflite_mobilenetModel;

    protected Interpreter tflite_color_ipr;
    protected Interpreter tflite_mobilenet_ipr;

    private final Interpreter.Options tfliteOptions = new Interpreter.Options();

    public static Colornet create(Activity activity, Model model, Device device, int numThreads)
            throws IOException {
        if (model == Model.FLOAT_COLORNET) {
            return new ColorNetFloat(activity, device, numThreads);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    protected Colornet(Activity activity, Device device, int numThreads) throws IOException {
        tflite_colorModel = FileUtil.loadMappedFile(activity, getColorModelPath());
        tflite_mobilenetModel = FileUtil.loadMappedFile(activity, getMobileNetModelPath());
        switch (device) {
            case GPU:
                gpuDelegate = new GpuDelegate();
                tfliteOptions.addDelegate(gpuDelegate);
                break;
            case CPU:
                break;
        }
        tfliteOptions.setNumThreads(numThreads);
        tflite_color_ipr = new Interpreter(tflite_colorModel, tfliteOptions);
        tflite_mobilenet_ipr = new Interpreter(tflite_mobilenetModel, tfliteOptions);
    }

    public Bitmap colorizeImage(Bitmap bitmap) {
        // Preprocess Input Image
        // RGB to LAB and RGB to gray -> Reshape to correct format
        Bitmap inpBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        float [][][][] l_input = loadImage_colornet((inpBitmap));
        float [][][][] g_input = loadImage_mobilenet((inpBitmap));

        float[][] m_output = new float[1][1000];
        float[][][][] c_output = new float[1][256][256][2];

        // Get Output from Mobilenet
        tflite_mobilenet_ipr.run(g_input, m_output);

        Object[] inputs = {l_input, m_output};
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(0, c_output);

        // Get output from Colornet
        tflite_color_ipr.runForMultipleInputsOutputs(inputs, outputs);

        // Convert L from input image and AB from model output
        float[][][] lab_output_image = new float[256][256][3];
        for (int x = 0; x < 256; x++) {
            for (int y = 0; y < 256; y++) {
                // Set L
                lab_output_image[x][y][0] = l_input[0][x][y][0];
                // Set AB
                lab_output_image[x][y][1] = c_output[0][x][y][0] * 128.0f;
                lab_output_image[x][y][2] = c_output[0][x][y][1] * 128.0f;
            }
        }

        Bitmap output_bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        // Convert LAB to RGB
        for (int i = 0; i < 256; i++) {
            for (int j = 0; j < 256; j++) {
                int l = Math.round(lab_output_image[i][j][0]);
                int a = Math.round(lab_output_image[i][j][1]);
                int b = Math.round(lab_output_image[i][j][2]);

                int rgbcolor = ColorUtils.LABToColor(l, a, b);
                output_bitmap.setPixel(i, j, rgbcolor);
            }
        }

        return output_bitmap;
    }

    public void close() {
        if (tflite_color_ipr != null) {
            tflite_color_ipr.close();
            tflite_color_ipr = null;
        }
        if (tflite_mobilenet_ipr != null) {
            tflite_mobilenet_ipr.close();
            tflite_mobilenet_ipr = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }

        tflite_colorModel = null;
        tflite_mobilenetModel = null;
    }

    private float[][][][] loadImage_colornet(Bitmap bitmap) {
        Bitmap lab_bitmap = bitmap;
        Bitmap resized = Bitmap.createScaledBitmap(lab_bitmap, 256, 256, true);
        float[][][][] l_input = new float[1][256][256][1];

        // Convert RGB to LAB
        for (int i = 0; i < resized.getWidth(); i++) {
            for (int j = 0; j < resized.getHeight(); j++) {
                int colour = resized.getPixel(i, j);
                int red = Color.red(colour);
                int blue = Color.blue(colour);
                int green = Color.green(colour);

                double[] lab=new double[3];
                ColorUtils.RGBToLAB(red, green, blue, lab);
                l_input[0][i][j][0] = (float) lab[0];
            }
        }
        return l_input;
    }

    private float[][][][] loadImage_mobilenet(Bitmap bitmap) {
        Bitmap gray_bitmap = bitmap;
        Bitmap resized = Bitmap.createScaledBitmap(gray_bitmap, 224, 224, true);

        // Convert RGB to Gray
        float[][][][] g_input = new float[1][224][224][3];
        for (int i = 0; i < resized.getWidth(); i++) {
            for (int j = 0; j < resized.getHeight(); j++) {
                // get pixel color
                int colour = resized.getPixel(i, j);
                int red = Color.red(colour);
                int blue = Color.blue(colour);
                int green = Color.green(colour);
                int gray = (int)(red+blue+green)/3;

                g_input[0][i][j][0] = gray / 255.0f;
                g_input[0][i][j][1] = gray / 255.0f;
                g_input[0][i][j][2] = gray / 255.0f;
            }
        }

        return g_input;
    }

    protected String getColorModelPath() { return "colormodel.tflite"; }
    protected String getMobileNetModelPath() { return "mobilenet.tflite"; }

}
