package com.example.lizhi.kuman_mpi_3508_lcd_touch;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity {

    private Mpi3508LcdTouchDriver mTouchDriver;

    private Button mButton1;
    private Button mButton2;
    private Button mButton3;
    private Button mButton4;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.e("===lizhi", "onTouchEvent: " +event.getAction() + " " + event.getX() + " " + event.getY());
        return super.onTouchEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;
        Log.e("===lizhi", "onCreate: screen resolution: " + width + " " + height);


        setContentView(R.layout.activity_main);
        Log.e("===lizhi", "isInTouchMode: " + findViewById(R.id.main_view).isInTouchMode());

        mButton1 = findViewById(R.id.button);
        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton1.setBackgroundColor(Color.BLUE);
            }
        });

        mButton2 = findViewById(R.id.button2);
        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton2.setBackgroundColor(Color.BLUE);
            }
        });
        mButton3 = findViewById(R.id.button3);
        mButton3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton3.setBackgroundColor(Color.BLUE);
            }
        });
        mButton4 = findViewById(R.id.button4);
        mButton4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mButton4.setBackgroundColor(Color.BLUE);
            }
        });

        mTouchDriver = new Mpi3508LcdTouchDriver(width, height);
        mTouchDriver.run();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mTouchDriver.stop();
    }
}
