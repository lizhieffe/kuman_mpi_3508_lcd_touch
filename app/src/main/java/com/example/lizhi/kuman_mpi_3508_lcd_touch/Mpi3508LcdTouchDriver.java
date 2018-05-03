package com.example.lizhi.kuman_mpi_3508_lcd_touch;

import android.view.InputDevice;
import android.view.MotionEvent;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.input.InputDriverEvent;

/**
 * Created by lizhi on 5/2/18.
 */

public class Mpi3508LcdTouchDriver {
    private final static int X_RESOLUTION = 480;
    private final static int Y_RESOLUTION = 480;

    private InputDriver mInputDriver;
    private Thread mInputThread;

    boolean mStopped;

    private static final String TAG = "MPI-3508-LCD-Touch-Driver";

    public void run() {
        // TODO: tune the fuzz and flat values.
        mInputDriver = new InputDriver.Builder()
                .setName(TAG)
                .setAxisConfiguration(MotionEvent.AXIS_X, 0, X_RESOLUTION, 1, 1)
                .setAxisConfiguration(MotionEvent.AXIS_Y, 0, Y_RESOLUTION, 1, 1)
                .build();
        UserDriverManager.getInstance().registerInputDriver(mInputDriver);

        mInputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mInputThread.isInterrupted() && !mStopped) {
                    try {
                        TouchInput touchInput = getTouchInput();
                        mInputDriver.emit(new InputDriverEvent());
                    }
                }
            }
        });
        mInputThread.start();
    }
}
