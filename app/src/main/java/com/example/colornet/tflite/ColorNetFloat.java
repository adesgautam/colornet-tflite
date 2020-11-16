package com.example.colornet.tflite;

import android.app.Activity;

import java.io.IOException;

public class ColorNetFloat extends Colornet {

    public ColorNetFloat(Activity activity, Device device, int numThreads)
            throws IOException {
        super(activity, device, numThreads);
    }

}
