package com.example.lizhi.kuman_mpi_3508_lcd_touch;

import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;

import com.google.android.things.pio.PeripheralManagerService;
import com.google.android.things.pio.SpiDevice;
import com.google.android.things.userdriver.InputDriver;
import com.google.android.things.userdriver.UserDriverManager;

import java.io.IOException;
import java.util.List;

/**
 * Created by lizhi on 5/2/18.
 */

public class Mpi3508LcdTouchDriver {
    private final static int SPI_DEVICE_CHANNEL = 1;
    private final static int SPI_DEVICE_FREQUENCY = 50000;

    private InputDriver mInputDriver;
    private Thread mInputThread;

    private SpiDevice mDevice;

    private volatile boolean mStopped;

    private static final String TAG = "MPI-3508-LCD-Touch-Driver";



    private boolean mIsPressing = false;
    private boolean mOutlinerDetected = false;
    private int cX = -1; // The current touching x-value
    private int cY = -1; // The current touching y-value
    private long xyTime = -1L;

    // TODO: what are these?
    private final byte[] xRead = new byte[]{(byte) 0xd0, (byte) 0x00, (byte) 0x00};
    private final byte[] yRead = new byte[]{(byte) 0x90, (byte) 0x00, (byte) 0x00};
    private final byte[] xBuffer = new byte[3];
    private final byte[] yBuffer = new byte[3];

    private final boolean switchXY = true;
    private final boolean inverseX = true;
    private final boolean inverseY = true;
    private final boolean flakeynessCorrection = true;
    private final boolean shiverringCorrection = true;

    private int mXResolution;
    private int mYResolution;

    public Mpi3508LcdTouchDriver(int xResolution, int yResolution) {
        mXResolution = xResolution;
        mYResolution = yResolution;
    }

    public void run() {
        mStopped = false;
        try {
            initSpiDevice();
        } catch (UnableToOpenTouchDriverException e) {
            Log.e(TAG, "Mpi3508LcdTouchDriver.run: ", e);
            return;
        }

        // TODO: tune the fuzz and flat values.
        mInputDriver = new InputDriver.Builder(InputDevice.SOURCE_TOUCHSCREEN)
                .setName(TAG)
                .setAbsMax(MotionEvent.AXIS_X, mXResolution)
                .setAbsMax(MotionEvent.AXIS_Y, mYResolution)
                .build();
        UserDriverManager.getManager().registerInputDriver(mInputDriver);

        mInputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!mInputThread.isInterrupted() && !mStopped) {
                    try {
                        TouchInput touchInput = getTouchInput();

                        // Log.e(TAG, "Mpi3508LcdTouchDriver.run: emitting: " + touchInput.toString());
                        mInputDriver.emit(touchInput.x, touchInput.y, touchInput.pressing);
                    } catch (TouchDriverReadingException e) {
                        Log.e(TAG, "Mpi3508LcdTouchDriver.run: ", e);
                    }
                }
            }
        });
        mInputThread.start();
    }

    public void stop() {
        mStopped = true;
        if (mInputThread != null) {
            mInputThread.interrupt();
        }
        UserDriverManager.getManager().unregisterInputDriver(mInputDriver);
    }

    private void initSpiDevice() throws UnableToOpenTouchDriverException {
        PeripheralManagerService spiService = new PeripheralManagerService();
        List<String> deviceList = spiService.getSpiBusList();

        Log.w(TAG, "Available SPI device list: " + deviceList);

        if (deviceList.isEmpty()) {
            Log.e(TAG, "No SPI device found");
            throw new UnableToOpenTouchDriverException();
        }

        final String spiName = deviceList.get(SPI_DEVICE_CHANNEL);
        try {
            mDevice = spiService.openSpiDevice(spiName);
            mDevice.setFrequency(SPI_DEVICE_FREQUENCY);
            // TODO: what is this?
            mDevice.transfer(new byte[]{(byte) 0x80, (byte) 0x00, (byte) 0x000}, new byte[4], 1);
        } catch (IOException e) {
            Log.e(TAG, "initSpiDevice: ", e);
            throw new UnableToOpenTouchDriverException();
        }
    }

    private TouchInput getTouchInput() throws TouchDriverReadingException {
        try {
            mDevice.transfer(xRead, xBuffer, 3);
            mDevice.transfer(yRead, yBuffer, 3);
            // Log.e(TAG, "Mpi3508LcdTouchDriver.getTouchInput: read xBuffer: " + Arrays.toString(xBuffer));
            // Log.e(TAG, "Mpi3508LcdTouchDriver.getTouchInput: read yBuffer: " + Arrays.toString(yBuffer));
        } catch (IOException e) {
            Log.e(TAG, "getTouchInput: ", e);
            throw new TouchDriverReadingException();
        }

        byte[] buffer = concat(xBuffer, yBuffer);
        boolean press = isPressing(buffer);

        int screenWidth = mXResolution;
        int screenHeight = mYResolution;
        float halfScreenWidth = screenWidth / 2f;
        float halfScreenHeight = screenHeight / 2f;

        int originalX = (buffer[2] + (buffer[1] << 8) >> 4);
        int originalY = (buffer[5] + (buffer[4] << 8) >> 4);
        if (switchXY) {
            int temp = originalY;
            originalY = originalX;
            originalX = temp;
        }
        int x = (int) ((originalX / 2030f) * screenWidth);
        int y = (int) ((originalY / 2100f) * screenHeight);

        int yErrorMargin = 24; // TODO make parameter
        float halfYDistance = halfScreenHeight - yErrorMargin;
        float travelledYDistance = y < halfScreenHeight ? halfYDistance - y - yErrorMargin : y - halfScreenHeight - yErrorMargin;
        int applicableYErrorMargin = (int) (((1 / halfYDistance) * travelledYDistance) * yErrorMargin);
        if (y < halfScreenHeight) {
            y = Math.max(0, y - applicableYErrorMargin);
        } else if (y > halfScreenHeight) {
            y = Math.min(screenHeight, y + applicableYErrorMargin);
        }

        int xErrorMargin = 20; // TODO make parameter
        float halfXDistance = halfScreenWidth - xErrorMargin;
        float travelledXDistance = x < halfScreenWidth ? halfXDistance - x - xErrorMargin : x - halfScreenWidth - xErrorMargin;
        int applicableXErrorMargin = (int) (((1 / halfXDistance) * travelledXDistance) * xErrorMargin);
        if (x < halfScreenWidth) {
            x = Math.max(0, x - applicableXErrorMargin);
        } else {
            x = Math.min(screenWidth, x + applicableXErrorMargin);
        }

        if (inverseX) {
            x = (int) (x < halfScreenWidth ? halfScreenWidth+ (Math.abs(halfScreenWidth - x)) : halfScreenWidth - (Math.abs(halfScreenWidth - x)));
        }
        if (inverseY) {
            y = (int) (y < halfScreenHeight ? halfScreenHeight + (Math.abs(halfScreenHeight - y)) : halfScreenHeight - (Math.abs(halfScreenHeight - y)));
        }

        long millisSinceLastTouch = System.currentTimeMillis() - xyTime;
        boolean outlierX = false;
        boolean outlierY = false;
        boolean shiverring = false;
        boolean keepsPressing = press && mIsPressing;
        if (keepsPressing && !mOutlinerDetected) {
            boolean fastXyTracking = millisSinceLastTouch <= 50;
            if (fastXyTracking && cX != -1) {
                int xOffset = Math.abs(x - cX);
                if (flakeynessCorrection && xOffset > 12) {
                    outlierX = true;
                    x = cX;
                } else if (shiverringCorrection && xOffset <= 12 && xOffset > 0) {
                    shiverring = true;
                    x = cX;
                }
            }
            if (fastXyTracking && cY != -1) {
                int yOffset = Math.abs(y - cY);
                if (flakeynessCorrection && yOffset > 12) {
                    outlierY = true;
                    y = cY;
                } else if (shiverringCorrection && yOffset <= 12 && yOffset > 0) {
                    shiverring = true;
                    y = cY;
                }
            }
        }
        mOutlinerDetected = outlierX || outlierY;

        if (press) {
            Log.v(TAG, "x,y=" + originalX + "," + originalY + " | x,y=" + x + "," + y + " | cx,cy=" + cX + "," + cY + " | dx,dy=" + applicableXErrorMargin + "," + applicableYErrorMargin + " (" + millisSinceLastTouch + "ms)" + (outlierX ? " CORRECTED-X!!!" : "") + (outlierY ? " CORRECTED-Y!!!" : "") + (shiverring ? " SHIVERRING!!!" : ""));
        } else if (mIsPressing && !press) {
            Log.v(TAG, "release");
        }

        mIsPressing = press;
        TouchInput touchInput = new TouchInput(x, y, press);

        if (press) {
            xyTime = System.currentTimeMillis();
            cX = x;
            cY = y;
        } else {
            cX = -1;
            cY = -1;
        }

        return touchInput;
    }

    private byte[] concat(byte[]... arrays) {
        int length = 0;
        for (byte[] array : arrays) {
            length += array.length;
        }
        byte[] result = new byte[length];
        int destPos = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, destPos, array.length);
            destPos += array.length;
        }
        return result;
    }

    private boolean isPressing(byte[] buffer) {
        return buffer[4] != 127;
    }
}
