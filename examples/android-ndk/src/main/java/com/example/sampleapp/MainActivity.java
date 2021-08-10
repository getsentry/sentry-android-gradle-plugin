package com.example.sampleapp;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.loadLibrary("hello-jni");
        helloWorld();
    }

    public void helloWorld() {
        System.out.println(stringFromJNI());
    }

    public native String stringFromJNI();

}
