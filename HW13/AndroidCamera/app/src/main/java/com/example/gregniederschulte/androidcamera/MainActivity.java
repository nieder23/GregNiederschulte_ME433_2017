package com.example.gregniederschulte.androidcamera;

// libraries
import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import java.io.IOException;
import static android.graphics.Color.blue;
import static android.graphics.Color.green;
import static android.graphics.Color.red;
import static android.graphics.Color.rgb;
import static java.lang.Math.abs;

public class MainActivity extends Activity implements TextureView.SurfaceTextureListener {
    private Camera mCamera;
    private TextureView mTextureView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private Bitmap bmp = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);
    private Canvas canvas = new Canvas(bmp);
    private Paint paint1 = new Paint();
    private TextView fpsTextView; // variable called fpsTextView of type TextView (can only receive data in the form of TextView types)
    SeekBar redControl; // variable called redControl of type SeekBar
    TextView redTextView; // variable called redTextView of type TextView
    SeekBar greenControl;
    TextView greenTextView;
    SeekBar blueControl;
    TextView blueTextView;
    SeekBar rowGapControl;
    TextView rowGapTextView;
    SeekBar colGapControl;
    TextView colGapTextView;

    static long prevtime = 0; // for FPS calculation

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // keeps the screen from turning off

        //Making connections between variables and the xml file layout via their IDs
        fpsTextView = (TextView) findViewById(R.id.cameraStatus); // ties together the textview cameraStatus and the variable fpsTextView

        redControl = (SeekBar) findViewById(R.id.seek1); // ties together the seekbar seek1 and the variable redControl
        redControl.setMax(255);
        greenControl = (SeekBar) findViewById(R.id.seek2); // ties together the seekbar seek2 and the variable greenControl
        greenControl.setMax(255);
        blueControl = (SeekBar) findViewById(R.id.seek3); // ties together the seekbar seek3 and the variable blueControl
        blueControl.setMax(255);
        rowGapControl = (SeekBar) findViewById(R.id.seek4);
        rowGapControl.setMax(30);
        colGapControl = (SeekBar) findViewById(R.id.seek5);
        colGapControl.setMax(30);

        redTextView = (TextView) findViewById(R.id.redTextView); // ties together the textview redTextView and the variable redTextView
        redTextView.setText("Enter whatever you Like!");
        setRedControlListener();
        greenTextView = (TextView) findViewById(R.id.greenTextView);
        greenTextView.setText("Enter whatever you Like!");
        setGreenControlListener();
        blueTextView = (TextView) findViewById(R.id.blueTextView);
        blueTextView.setText("Enter whatever you Like!");
        setBlueControlListener();
        rowGapTextView = (TextView) findViewById(R.id.rowGapTextView);
        rowGapTextView.setText("Enter whatever you Like!");
        setRowGapControlListener();
        colGapTextView = (TextView) findViewById(R.id.colGapTextView);
        colGapTextView.setText("Enter whatever you Like!");
        setColGapControlListener();

        //See if the app has permission to use the camera
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, 1);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            //Establish the camera feed window (TextureView) and the feed that will be drawn on (SurfaceView)
            mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview); // Provides a dedicated drawing surface embedded inside of a view hierarchy
            mSurfaceHolder = mSurfaceView.getHolder(); // Access to the underlying surface is provided via the SurfaceHolder interface

            mTextureView = (TextureView) findViewById(R.id.textureview); // A TextureView can be used to display a content stream
            mTextureView.setSurfaceTextureListener(this); // Using a TextureView is simple: all you need to do is get its SurfaceTexture. The SurfaceTexture can then be used to render content

            //Set the paintbrush for writing text on the image
            paint1.setColor(0xffff0000); // red
            paint1.setTextSize(24);

            fpsTextView.setText("started camera");
        } else {
            fpsTextView.setText("no camera permissions");
        }
    }

    //Invoked when mTextureView's SurfaceTexture is ready for use; SurfaceTexture is the surface returned by getSurfaceTexture()
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCamera = Camera.open();
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY); // no autofocusing
        parameters.setAutoExposureLock(true); // keep the white balance constant
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90); // rotate to portrait mode

        try {
            mCamera.setPreviewTexture(surface);
            mCamera.startPreview();
        }
        catch (IOException ioe) {
            //Something bad happened
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mCamera.stopPreview();
        mCamera.release();
        return true;
    }

    //Establishing some important variable outside of the function (they don't need to be defined every time the frame updates)


    //The important function
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //Every time there is a new Camera preview frame, create a bitmap of it and store it in the variable bmp
        mTextureView.getBitmap(bmp);
        int CoM;
        int mr = 0;
        int n = 1;
        int rowGap = rowGapControl.getProgress(); // how many rows to analyze along the Y axis
        int colGap = colGapControl.getProgress(); // how many columns to analyze along the X axis
        final Canvas c = mSurfaceHolder.lockCanvas();
        if (c != null) {
            int[] pixels = new int[bmp.getWidth()]; // pixels[] is the vector that will hold the RGBA data, this is setting it up to be the proper size
            for(int startY = 0; startY < bmp.getHeight(); startY += rowGap ) {// which row in the bitmap to analyze to read
                //Pull the row startY from the bmp and put the rgb data of each pixel in pixels[] for analysis
                bmp.getPixels(pixels, 0, bmp.getWidth(), 0, startY, bmp.getWidth(), 1); //getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height)

                //Analyze the row going across pixel by pixel and change it to black if it is read as gray-ish (the red, green, and blue values are all about the same)
                for (int i = 0; i < bmp.getWidth(); i += colGap) {
                    if(abs(green(pixels[i]) - red(pixels[i])) < redControl.getProgress() && abs(blue(pixels[i]) - green(pixels[i])) < greenControl.getProgress() && abs(red(pixels[i]) - blue(pixels[i])) < blueControl.getProgress() && green(pixels[i])>115 && blue(pixels[i])>115 && red(pixels[i])>115) {
                        pixels[i] = rgb(1, 1, 1); // over write the pixel with black
                        mr = mr + i;
                        n++;
                    }
                }
                //Update the row
                bmp.setPixels(pixels, 0, bmp.getWidth(), 0, startY, bmp.getWidth(), 1);
                if (n > 0 && mr > 0) {
                    CoM = mr/n;
                } else {
                    CoM = bmp.getWidth()/2;
                }
                //Draw a circle at center of mass
                canvas.drawCircle(CoM, startY, 5, paint1); // x position, y position, diameter, color
            }
        }
        c.drawBitmap(bmp, 0, 0, null);
        mSurfaceHolder.unlockCanvasAndPost(c);

        // calculate the FPS to see how fast the code is running
        long nowtime = System.currentTimeMillis();
        long diff = nowtime - prevtime;
        fpsTextView.setText("FPS " + 1000 / diff);
        prevtime = nowtime;
    }

    private void setRedControlListener() {
        redControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                redTextView.setText(getResources().getString(R.string.redSlider) +progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void setGreenControlListener() {
        greenControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                greenTextView.setText(getResources().getString(R.string.greenSlider) +progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void setBlueControlListener() {
        blueControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                blueTextView.setText(getResources().getString(R.string.blueSlider) +progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void setRowGapControlListener() {
        rowGapControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                rowGapTextView.setText(getResources().getString(R.string.rowGapSlider) +progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    private void setColGapControlListener() {
        colGapControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            int progressChanged = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
                colGapTextView.setText(getResources().getString(R.string.colGapSlider) +progress);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
}
