package com.generate.generatemsg;
import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;

import com.facebook.messenger.MessengerThreadParams;
import com.facebook.messenger.MessengerUtils;
import com.facebook.messenger.ShareToMessengerParams;
import com.facebook.FacebookSdk;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;
import android.util.Log;
import android.widget.ImageView;
import android.content.pm.PackageManager;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;

import jp.co.cyberagent.android.gpuimage.GPUImageBoxBlurFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageEmbossFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageFilter;
import jp.co.cyberagent.android.gpuimage.GPUImageHueFilter;
import jp.co.cyberagent.android.gpuimage.GPUImagePixelationFilter;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;

import java.io.FileOutputStream;
import java.util.Random;

import jp.co.cyberagent.android.gpuimage.GPUImage;
import android.opengl.GLSurfaceView;
import android.graphics.Bitmap;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.graphics.drawable.AnimationDrawable;
import java.util.List;
import javax.microedition.khronos.opengles.GL10;
import android.opengl.GLException;
import java.nio.IntBuffer;

// currently only support 1:1 ratio
enum ASPECT_RATIO { SQUARE }

public class MainActivity extends Activity {

  private static final int REQUEST_CODE_SHARE_TO_MESSENGER = 1;

  // buttons
  private Button mMessengerButton;
  private Button captureButton;
  private Button reGenerateBtn;
  private Button reTakePhotoBtn;
  private ImageView loadingSignal;
  private AnimationDrawable loadingAnimation;

  private MessengerThreadParams mThreadParams;
  private boolean mPicking;

  private String TAG = "GenerateMsg:";

  private Camera mCamera;
  private CameraPreview mPreview;
  private Uri photoURI;
  private GPUImage mGPUImage;
  private android.opengl.GLSurfaceView mGLSurfaceView;
  private ASPECT_RATIO aspectRatio;
  private float inputAspectRatio = 1f;

  private Bitmap bitmapToSave;
  private Context context;
  private int currentRotation = 90;
  private String currentFilter;
  private String lastFilter = "NONE";
  private String[] filterNameList = {"HUE","PIXEL", "RPG", "BOX", "DOT"}; // New filter need to add name into this list
  private float currentFactor;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FacebookSdk.sdkInitialize(getApplicationContext()); // facebook sdk initialize
    context = this;
    setContentView(R.layout.activity_main);

    // Create an instance of Camera
    mCamera = getCameraInstance();
    mCamera.setParameters(setParameters());

    mGLSurfaceView = (GLSurfaceView)findViewById(R.id.gpu_image);
    mGLSurfaceView.post(new Runnable(){
      @Override
      public void run(){
        mGLSurfaceView.postInvalidate();
        setAspectRatio(ASPECT_RATIO.SQUARE);
      }
    });
    mGPUImage = new GPUImage(this);
    mGPUImage.setGLSurfaceView(mGLSurfaceView); //Sets the GLSurfaceView which will display the preview.
    currentFactor = randomFilterFactor();
    currentFilter = randomFilter(filterNameList);
    mGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));

    // Create Buttons
    captureButton = (Button) findViewById(R.id.camera_capture); // take photo button
    reGenerateBtn = (Button) findViewById(R.id.regenerate);
    mMessengerButton = (Button)findViewById(R.id.send_msg); // send to Messenger button
    reTakePhotoBtn = (Button) findViewById(R.id.retakephoto);
    loadingSignal = (ImageView) findViewById(R.id.loading);
    loadingAnimation = (AnimationDrawable) loadingSignal.getBackground();

    /** Capture button click listener **/
    captureButton.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View v) {
                captureImage();
                // change UI to sendMsg btn and Retake btn
                captureButton.setVisibility(View.INVISIBLE);
                reGenerateBtn.setVisibility(View.INVISIBLE);
                //mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg_gray);
                mMessengerButton.setVisibility(View.VISIBLE);
                loadingSignal.setVisibility(View.VISIBLE);
                reTakePhotoBtn.setVisibility(View.VISIBLE);
              }
            });

    Intent intent = getIntent();
    if (Intent.ACTION_PICK.equals(intent.getAction())) {
      mThreadParams = MessengerUtils.getMessengerThreadParamsForIntent(intent);
      mPicking = true;
      // Note, if mThreadParams is non-null, it means the activity was launched from Messenger.
      // It will contain the metadata associated with the original content, if there was content.
    }

    /** Send Photo To Messenger button click listener **/
    mMessengerButton.setOnClickListener(new View.OnClickListener() {

      @Override
      public void onClick(View v) {
        onMessengerButtonClicked(photoURI);
      }
    });

    /** Re Generate Filter button click listener **/
    reGenerateBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        //random a new filter factor
        currentFactor = randomFilterFactor();
        do{
          currentFilter = randomFilter(filterNameList);

        }while (currentFilter == lastFilter);

        lastFilter = currentFilter;
        mGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));
      }
    });

    /** Retake Photo button click listener **/
    reTakePhotoBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        //restart camera preview
        mCamera.startPreview();
        // change UI to sendMsg btn and Retake btn
        captureButton.setVisibility(View.VISIBLE);
        reGenerateBtn.setVisibility(View.VISIBLE);
        mMessengerButton.setVisibility(View.INVISIBLE);
        mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg_gray);
        reTakePhotoBtn.setVisibility(View.INVISIBLE);
      }
    });
  }

  @Override
  protected void onResume() {
    // Super First
    super.onResume();
    setGPUImageToCamera();
  }

  /** Set the surfaceView ratio to 1:1 **/
  private void setAspectRatio(ASPECT_RATIO ratio){
    aspectRatio = ratio;
    int height = 0;
    switch (ratio){
      case SQUARE:
        inputAspectRatio = 1f;
        height = mGLSurfaceView.getWidth();
        break;
      default:
        float displayWidth = (float) getWindowManager().getDefaultDisplay().getWidth();
        float displayHeight = (float) getWindowManager().getDefaultDisplay().getHeight();
        inputAspectRatio = displayHeight/displayWidth;
        height = (int) displayHeight;
        break;
    }
    setSurfaceViewHeight(height);
  }

  /** Set the surfaceView height **/
  private void setSurfaceViewHeight(int height) {
    RelativeLayout.LayoutParams mParams;
    mParams = (RelativeLayout.LayoutParams) mGLSurfaceView.getLayoutParams();
    mParams.height = height;
    mGLSurfaceView.setLayoutParams(mParams);
  }

  /** Take image **/
  private void captureImage(){
    mCamera.takePicture(null, null, new Camera.PictureCallback() {
      @Override
      public void onPictureTaken(byte[] data, final Camera camera) {
        if(data == null) {
          return;
        }
        // Use this like for loading the image in another thread
        new WriteImageFileTask().execute(data);
      }
    });
  }

  /** Async image writing **/
  // Currently save the photo first and asign filter again onto the photo
  // Didn't figure out different way to do it
  private class WriteImageFileTask extends AsyncTask<byte[], Object, Bitmap> {
    protected Bitmap doInBackground(byte[]... params) {

      Bitmap bitmap = BitmapFactory.decodeByteArray(params[0] , 0, params[0] .length);
      GPUImage yGPUImage = new GPUImage(context);
      yGPUImage.setFilter(getGPUImageFilter(currentFilter, currentFactor));
      Bitmap filteredBitmap = yGPUImage.getBitmapWithFilterApplied(bitmap);
      filteredBitmap = rotateImage(filteredBitmap, currentRotation);
      filteredBitmap = cropImage(filteredBitmap);

      return filteredBitmap;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
      SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
      String timeStamp = s.format(new Date());

      File storageDir = getApplicationContext().getFilesDir();
      String fileName = "img_filtered";
      File mediaFile = new File(storageDir, fileName.concat(timeStamp).concat(".jpg"));

      try {
        FileOutputStream fos = new FileOutputStream(mediaFile);
        result.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        fos.close();
      } catch (FileNotFoundException e) {
        Log.d(TAG, "File not found: " + e.getMessage());
      } catch (IOException e) {
        Log.d(TAG, "Error accessing file: " + e.getMessage());
      }

      photoURI = FileProvider.getUriForFile(getApplicationContext(), "com.generage.generatemsg.android.fileprovider", mediaFile);
      loadingSignal.setVisibility(View.INVISIBLE);
      mMessengerButton.setBackgroundResource(R.drawable.btn_sendmsg);
    }
  }

  /** Rotate image **/
  private Bitmap rotateImage(Bitmap source, float angle) {
    Matrix matrix = new Matrix();
    matrix.postRotate(angle);
    if(source == null) {
      Toast.makeText(context, "Null Image - please try again", Toast.LENGTH_SHORT).show();
    }
    return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
  }

  /** Crop final photo, get the middle square of the photo **/
  private Bitmap cropImage(Bitmap source) {
    if(source == null) {
      Toast.makeText(context, "Null Image - please try again", Toast.LENGTH_SHORT).show();
    }
    int width = source.getWidth();
    int height = source.getHeight();
    int marginTop = (height-width)/2;

    return Bitmap.createBitmap(source, 0, marginTop, width, width);
  }

  /** ===================================================
   *  Support Functions
   *  ===================================================**/

  /** Check if this device has a camera **/
  // didn't use it currently
  private boolean checkCameraHardware(Context context) {
    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
      return true; // this device has a camera
    } else {
      return false; // no camera on this device
    }
  }

  /** A safe way to get an instance of the Camera object. */
  public static Camera getCameraInstance(){
    Camera c = null;
    try {
      c = Camera.open(); // attempt to get a Camera instance
    }
    catch (Exception e){
      // Camera is not available (in use or does not exist)
    }
    return c; // returns null if camera is unavailable
  }

  /** A basic Camera preview class */
  public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
      super(context);
      mCamera = camera;
      mCamera.setDisplayOrientation(90);
      // Install a SurfaceHolder.Callback so we get notified when the
      // underlying surface is created and destroyed.
      mHolder = getHolder();
      mHolder.addCallback(this);
      // deprecated setting, but required on Android versions prior to 3.0
      mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
      // The Surface has been created, now tell the camera where to draw the preview.
      try {
        mCamera.setPreviewDisplay(holder);
        mCamera.startPreview();
      } catch (IOException e) {
        Log.d(TAG, "Error setting camera preview: " + e.getMessage());
      }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
      // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
      // If your preview can change or rotate, take care of those events here.
      // Make sure to stop the preview before resizing or reformatting it.

      if (mHolder.getSurface() == null){
        // preview surface does not exist
        return;
      }

      // stop preview before making changes
      try {
        mCamera.stopPreview();
      } catch (Exception e){
        // ignore: tried to stop a non-existent preview
      }

      // set preview size and make any resize, rotate or
      // reformatting changes here

      // start preview with new settings
      try {
        mCamera.setPreviewDisplay(mHolder);
        mCamera.startPreview();

      } catch (Exception e){
        Log.d(TAG, "Error starting camera preview: " + e.getMessage());
      }
    }
  }
  public void closeCamera() {
    if (mCamera != null) {
      mCamera.stopPreview();
      mCamera.setPreviewCallback(null);
      mCamera.lock();
      mCamera.release();
      mCamera = null;
    }
  }

  /** Setup GPU image for camera in order to have preview with filter **/
  private void setGPUImageToCamera() {
    Boolean shouldFlipVertically = false;
    try {
      mCamera = Camera.open();
      mCamera.setParameters(setParameters());
    } catch (Exception e) {
      e.printStackTrace();
    }
    mGPUImage.setUpCamera(mCamera, currentRotation, false, shouldFlipVertically);
  }

  /** Setup Parameters for camera **/
  // The preview width and height have to be set same with the picture width and height
  // or the photo it takes will looks different with preview
  private Parameters setParameters() {
    Parameters parameters = null;
    if (mCamera != null) {
      parameters = mCamera.getParameters();
    }

    // Set Focus
    if (parameters.getSupportedFocusModes().contains(
            Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
      parameters
              .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
    }

    // Set picture size same with preview
    List<Camera.Size> previewSizeList = parameters.getSupportedPreviewSizes();

    parameters.setPreviewSize(previewSizeList.get(0).width,previewSizeList.get(0).height);
    parameters.setPictureSize(previewSizeList.get(0).width,previewSizeList.get(0).height);

    return parameters;
  }

  /** The Facebook Messenger API part **/
  private void onMessengerButtonClicked(Uri photoURI) {
    // The URI can reference a file://, content://, or android.resource. Here we use
    // android.resource for sample purposes.
    Uri uri =photoURI;

    // Create the parameters for what we want to send to Messenger.
    ShareToMessengerParams shareToMessengerParams =
        ShareToMessengerParams.newBuilder(uri, "image/jpeg")
            .build();

    if (mPicking) {
      // If we were launched from Messenger, we call MessengerUtils.finishShareToMessenger to return
      // the content to Messenger.
      MessengerUtils.finishShareToMessenger(this, shareToMessengerParams);
    } else {
      // Otherwise, we were launched directly (for example, user clicked the launcher icon). We
      // initiate the broadcast flow in Messenger. If Messenger is not installed or Messenger needs
      // to be upgraded, this will direct the user to the play store.
      try{
        MessengerUtils.shareToMessenger(
                this,
                REQUEST_CODE_SHARE_TO_MESSENGER,
                shareToMessengerParams);
      }catch (Exception e){
        Log.e(TAG, "ERROR!!! : " + e);
      }
    }
  }

  /** Random a filter and set filter factor on it **/
  // the idea here is set all factors of filter with on float number
  // I don't have time to do much on the formula part, so it's still draft
  // The following 5 filters are for testing
  private GPUImageFilter getGPUImageFilter(String filterName, float filterFactor){
    Log.d(TAG, "filter : "+filterName);
    switch (filterName){
      case "HUE":
        GPUImageHueFilter filterHUE = new GPUImageHueFilter();
        filterHUE.setHue(filterFactor*360);
        return filterHUE;
      case "PIXEL":
        GPUImagePixelationFilter filterPIXEL = new GPUImagePixelationFilter();
        filterPIXEL.setPixel(filterFactor*30);
        return filterPIXEL;
      case "RPG":
        GPUImageEmbossFilter filterRPG = new GPUImageEmbossFilter();
        filterRPG.setLineSize(filterFactor*360);
        filterRPG.setTexelHeight(filterFactor*3);
        filterRPG.setTexelWidth(filterFactor*7);
        return filterRPG;
      case "DOT":
        PolkaDotFilter filterDOT = new PolkaDotFilter();
        filterDOT.setDotScaling(filterFactor*0.1f+0.7f);
        return filterDOT;
      case "BOX":
        GPUImageBoxBlurFilter filterBox = new GPUImageBoxBlurFilter();
        filterBox.setBlurSize(filterFactor*12+10);
        return filterBox;
    }
    return null;
  }

  /** Get a random filter name from filter list **/
  private String randomFilter(String[] filterNameList){
    int seed = new Random().nextInt(filterNameList.length);
    return filterNameList[seed];
  }

  /** Get a random float number as filter factor **/
  private float randomFilterFactor(){
    float randomSeed = new Random().nextFloat();
    return randomSeed;
  }
}

