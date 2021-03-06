/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.hoho.android.usbserial.examples;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.Color.rgb;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends Activity implements TextureView.SurfaceTextureListener {

    private final String TAG = SerialConsoleActivity.class.getSimpleName();

    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private CheckBox chkDTR;
    private CheckBox chkRTS;

    //Camera
    SeekBar myControl;
    SeekBar pwmControl;
    SeekBar turningControl;
    private Camera mCamera;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap bmp = Bitmap.createBitmap(640,480,Bitmap.Config.ARGB_8888);
    private Canvas canvas = new Canvas(bmp);
    private Paint paint1 = new Paint();
    private TextView mTextView;

    int thresh;
    int turning_thresh;
    int pwm_thresh;
    int count = 0;

    static long prevtime = 0; // for FPS calculation



    //

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
            SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SerialConsoleActivity.this.updateReceivedData(data);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.serial_console);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = (TextView) findViewById(R.id.consoleText);
        mScrollView = (ScrollView) findViewById(R.id.demoScroller);
        chkDTR = (CheckBox) findViewById(R.id.checkBoxDTR);
        chkRTS = (CheckBox) findViewById(R.id.checkBoxRTS);

        chkDTR.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setDTR(isChecked);
                }catch (IOException x){}
            }
        });

        chkRTS.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                try {
                    sPort.setRTS(isChecked);
                }catch (IOException x){}
            }
        });

        //Camera stuff
        mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();

        mTextureView = (TextureView) findViewById(R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);

        mTextView = (TextView) findViewById(R.id.cameraStatus);

        paint1.setColor(0xffff0000); // red
        paint1.setTextSize(24);

        //SeekBar
        myControl = (SeekBar) findViewById(R.id.seek1);

        turningControl = (SeekBar) findViewById(R.id.seek2);
        pwmControl = (SeekBar) findViewById(R.id.seek3);
        setMyControlListener();
        //End of camera Stuff

    }
    private void setMyControlListener() {
        myControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                thresh = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        turningControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                turning_thresh = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        pwmControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                pwm_thresh = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }

    void showStatus(TextView theTextView, String theLabel, boolean theValue){
        String msg = theLabel + ": " + (theValue ? "enabled" : "disabled") + "\n";
        theTextView.append(msg);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            mTitleTextView.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                mTitleTextView.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(115200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

//                showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
//                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
//                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
//                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
//                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
//                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
//                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());
                String sData = "1000 2500\n\r";
                try {
                    sPort.write(sData.getBytes(), 10);
                }
                catch (IOException e) { }

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                mTitleTextView.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());
//        byte[] sData = {'a',0};
//        String sData = "1000 2500\n\r";
//        try {
//            sPort.write(sData.getBytes(), 10);
//        }
//        catch (IOException e) { }
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     * @param driver
     */
    static void show(Context context, UsbSerialPort port) {
        sPort = port;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
    }

    //Camera functions

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
//        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        parameters.setPreviewSize(640, 480);
        parameters.setColorEffect(Camera.Parameters.EFFECT_NONE); // black and white
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY); // no autofocusing
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90); // rotate to portrait mode
        parameters.setAutoWhiteBalanceLock(true);
        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    // the important function
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
        mTextureView.getBitmap(bmp);

        final Canvas c = mSurfaceHolder.lockCanvas();
        if (c != null) {
            int startY = 150;
            int wbTotal = 0; // total mass
            int wbCOM = 0; // total (mass time position)
            for(int currentY = startY; currentY < startY+1; currentY += 1) {
                int[] pixels = new int[bmp.getWidth()];
                // which row in the bitmap to analyse to read
                // only look at one row in the image
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, currentY, bmp.getWidth(), 1); // (array name, offset inside array, stride (size of row), start x, start y, num pixels to read per row, num rows to read)

                // pixels[] is the RGBA data (in black an white).
                // instead of doing center of mass on it, decide if each pixel is dark enough to consider black or white
                // then do a center of mass on the thresholded array
                int[] thresholdedPixels = new int[bmp.getWidth()];
                int[] thresholdedColors = new int[bmp.getWidth()];

                count = 0;


                for (int i = 0; i < bmp.getWidth(); i++) {
                    // sum the red, green and blue, subtract from 255 to get the darkness of the pixel.
                    // if it is greater than some value (600 here), consider it black
                    // play with the 600 value if you are having issues reliably seeing the line
//                    if (255*3-(red(pixels[i])+green(pixels[i])+blue(pixels[i])) > thresh*6) {
//                    if (255*3-(red(pixels[i])+green(pixels[i])+blue(pixels[i])) > (480+thresh)) {
//                    if (255*3-(red(pixels[i])+green(pixels[i])+blue(pixels[i])) > (500+thresh*2)) {
//                    if (255*2-(red(pixels[i])+green(pixels[i])) > (260+thresh)) {
                    if (red(pixels[i])>thresh*2) {
                        //Green pixels
                        thresholdedPixels[i] = 255 * 3;
                        thresholdedColors[i] = rgb(0, 0, 0);
                        count += 1;
                    } else {
                        thresholdedPixels[i] = 0;
                        thresholdedColors[i] = rgb(0, 255, 0);
                    }
                    if(currentY == startY) {
                        wbTotal = wbTotal + thresholdedPixels[i];
                        wbCOM = wbCOM + thresholdedPixels[i] * i;
                    }
                    bmp.setPixels(thresholdedColors, 0, bmp.getWidth(), 0, currentY, bmp.getWidth(), 1);
                }
            }
            int COM;
            //watch out for divide by 0
            if (wbTotal<=0) {
                COM = bmp.getWidth()/2;
            }
            else {
                COM = wbCOM/wbTotal;
            }

            // draw a circle where you think the COM is
            canvas.drawCircle(COM, startY, 5, paint1);
            int leftPWM;
            int rightPWM;
            if((count > 60) && (count < 400)){
                //Set PWM
                int pwm_value = (pwm_thresh * 2999) / 100;
                int temp = COM - 320;

                if (temp < 0) { //left side
                    leftPWM = pwm_value + ((temp * turning_thresh) / 10);
                    rightPWM = pwm_value;

                } else if (temp > 0) {
                    leftPWM = pwm_value;
                    rightPWM = pwm_value - ((temp * turning_thresh) / 10);

                } else {
                    leftPWM = pwm_value;
                    rightPWM = pwm_value;
                }
            } else {
                leftPWM = 600;
                rightPWM = 600;

                if(count < 60){//threshold is too high
//                    myControl.setProgress(thresh-5);
                    thresh = thresh - 5;
                } else { //threshold is too low
//                    myControl.setProgress(thresh+5);
                    thresh = thresh + 5;
                }

            }

//            String sData = "1000 2500\n\r";
            String sData = String.valueOf(leftPWM) + " " + String.valueOf(rightPWM) + "\n\r";
            try {
                sPort.write(sData.getBytes(), 10);
            }
            catch (IOException e) { }

            //Write PWM

            // also write the value as text
            canvas.drawText("COM = " + COM, 10, 200, paint1);
//            canvas.drawText("THRESHOLD = " + thresh*6, 10, 230, paint1);
            canvas.drawText("THRESHOLD = " + (thresh*2), 10, 230, paint1);
            canvas.drawText("LEFTPWM = " + leftPWM, 10, 260, paint1);
            canvas.drawText("RIGHTPWM = " + rightPWM, 10, 290, paint1);
            canvas.drawText("COUNT = " + count, 10, 320, paint1);
            c.drawBitmap(bmp, 0, 0, null);
            mSurfaceHolder.unlockCanvasAndPost(c);

            // calculate the FPS to see how fast the code is running
            long nowtime = System.currentTimeMillis();
            long diff = nowtime - prevtime;
            mTextView.setText("FPS " + 1000/diff);
            prevtime = nowtime;



        }
    }
    //End of Camera functions

}
